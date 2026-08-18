import base64
from collections.abc import Generator
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

import pytest
from sqlalchemy import Engine, func, select
from sqlalchemy.orm import Session

from app.db.session import create_database_engine, create_session_factory, init_db
from app.integrations.gmail.client import StaleGmailHistoryError
from app.models.email import RawEmail
from app.repositories.email import EmailRepository
from app.repositories.gmail_state import GmailStateRepository
from app.services.gmail_history import GmailHistorySyncService
from app.services.gmail_sync import GmailSyncService
from app.services.ingestion import IngestionService


class FakeHistoryClient:
    def __init__(
        self,
        *,
        pages: dict[str | None, dict[str, Any]] | None = None,
        messages: dict[str, dict[str, Any]] | None = None,
        stale: bool = False,
        current_history_id: str = "200",
        failing_message_id: str | None = None,
    ) -> None:
        self.pages = pages or {}
        self.messages = messages or {}
        self.stale = stale
        self.current_history_id = current_history_id
        self.failing_message_id = failing_message_id
        self.history_calls: list[tuple[str, str | None]] = []

    def list_history_page(
        self,
        start_history_id: str,
        page_token: str | None = None,
    ) -> dict[str, Any]:
        self.history_calls.append((start_history_id, page_token))
        if self.stale:
            raise StaleGmailHistoryError
        return self.pages[page_token]

    def get_message(self, message_id: str) -> dict[str, Any]:
        if message_id == self.failing_message_id:
            raise RuntimeError("simulated message retrieval failure")
        return self.messages[message_id]

    def list_recent_inbox_message_ids(self, max_results: int) -> list[str]:
        return list(self.messages)[:max_results]

    def get_current_history_id(self) -> str:
        return self.current_history_id


@pytest.fixture
def history_database(
    database_path: Path,
) -> Generator[tuple[Session, Engine], None, None]:
    engine = create_database_engine(f"sqlite:///{database_path.as_posix()}")
    init_db(engine)
    session = create_session_factory(engine)()
    try:
        yield session, engine
    finally:
        session.close()
        engine.dispose()


def gmail_message(message_id: str) -> dict[str, Any]:
    body = base64.urlsafe_b64encode(f"Body {message_id}".encode()).decode()
    return {
        "id": message_id,
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/plain",
            "headers": [
                {"name": "From", "value": "notices@example.edu"},
                {"name": "To", "value": "student@example.edu"},
                {"name": "Subject", "value": f"Message {message_id}"},
            ],
            "body": {"data": body},
        },
    }


def initialize_state(repository: GmailStateRepository, history_id: str = "100") -> None:
    repository.record_watch(
        history_id=history_id,
        expires_at=datetime.now(timezone.utc) + timedelta(days=7),
    )


def build_service(
    client: FakeHistoryClient,
    session: Session,
) -> GmailHistorySyncService:
    email_repository = EmailRepository(session)
    ingestion_service = IngestionService(email_repository)
    return GmailHistorySyncService(
        gmail_client=client,
        ingestion_service=ingestion_service,
        state_repository=GmailStateRepository(session),
        recovery_sync_service=GmailSyncService(client, ingestion_service),
    )


def test_history_list_processes_message_added_across_multiple_pages(
    history_database: tuple[Session, Engine],
) -> None:
    session, _ = history_database
    state_repository = GmailStateRepository(session)
    initialize_state(state_repository)
    client = FakeHistoryClient(
        pages={
            None: {
                "history": [
                    {
                        "id": "105",
                        "messages": [{"id": "must-not-be-used"}],
                        "messagesAdded": [{"message": {"id": "m1"}}],
                    }
                ],
                "historyId": "105",
                "nextPageToken": "page-2",
            },
            "page-2": {
                "history": [
                    {
                        "id": "110",
                        "messagesAdded": [
                            {"message": {"id": "m2"}},
                            {"message": {"id": "m1"}},
                        ],
                    }
                ],
                "historyId": "110",
            },
        },
        messages={"m1": gmail_message("m1"), "m2": gmail_message("m2")},
    )

    summary = build_service(client, session).sync("110")

    assert client.history_calls == [("100", None), ("100", "page-2")]
    assert summary.history_records == 2
    assert summary.messages_found == 2
    assert summary.created == 2
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 2
    state = state_repository.get()
    assert state is not None
    assert state.last_history_id == "110"


def test_duplicate_notification_does_not_repeat_history_or_create_rows(
    history_database: tuple[Session, Engine],
) -> None:
    session, _ = history_database
    repository = GmailStateRepository(session)
    initialize_state(repository)
    client = FakeHistoryClient(
        pages={
            None: {
                "history": [
                    {"messagesAdded": [{"message": {"id": "m1"}}]}
                ],
                "historyId": "110",
            }
        },
        messages={"m1": gmail_message("m1")},
    )
    service = build_service(client, session)

    first = service.sync("110")
    second = service.sync("110")

    assert first.created == 1
    assert second.messages_found == 0
    assert client.history_calls == [("100", None)]
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 1


def test_history_state_does_not_advance_after_partial_processing_failure(
    history_database: tuple[Session, Engine],
) -> None:
    session, _ = history_database
    repository = GmailStateRepository(session)
    initialize_state(repository)
    client = FakeHistoryClient(
        pages={
            None: {
                "history": [
                    {
                        "messagesAdded": [
                            {"message": {"id": "m1"}},
                            {"message": {"id": "m2"}},
                        ]
                    }
                ],
                "historyId": "110",
            }
        },
        messages={"m1": gmail_message("m1"), "m2": gmail_message("m2")},
        failing_message_id="m2",
    )

    with pytest.raises(RuntimeError, match="retrieval failure"):
        build_service(client, session).sync("110")

    state = repository.get()
    assert state is not None
    assert state.last_history_id == "100"
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 1


def test_stale_history_runs_bounded_sync_and_resets_state(
    history_database: tuple[Session, Engine],
) -> None:
    session, _ = history_database
    repository = GmailStateRepository(session)
    initialize_state(repository)
    client = FakeHistoryClient(
        stale=True,
        current_history_id="250",
        messages={"m1": gmail_message("m1")},
    )

    summary = build_service(client, session).sync("200")

    assert summary.recovered_from_stale_history is True
    assert summary.created == 1
    state = repository.get()
    assert state is not None
    assert state.last_history_id == "250"
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 1
