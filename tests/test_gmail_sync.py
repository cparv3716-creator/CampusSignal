import base64
from collections.abc import Generator
from pathlib import Path
from typing import Any

import pytest
from sqlalchemy import Engine, func, select
from sqlalchemy.orm import Session

from app.db.session import create_database_engine, create_session_factory, init_db
from app.models.email import RawEmail
from app.repositories.email import EmailRepository
from app.services.gmail_sync import GmailSyncService
from app.services.ingestion import IngestionService


class FakeGmailClient:
    def __init__(
        self,
        messages: dict[str, dict[str, Any]],
        *,
        failing_message_ids: set[str] | None = None,
        list_failure: bool = False,
    ) -> None:
        self.messages = messages
        self.failing_message_ids = failing_message_ids or set()
        self.list_failure = list_failure
        self.requested_max_results: list[int] = []

    def list_recent_inbox_message_ids(self, max_results: int) -> list[str]:
        self.requested_max_results.append(max_results)
        if self.list_failure:
            raise RuntimeError("simulated Gmail list failure")
        return list(self.messages)[:max_results]

    def get_message(self, message_id: str) -> dict[str, Any]:
        if message_id in self.failing_message_ids:
            raise RuntimeError("simulated Gmail get failure")
        return self.messages[message_id]


@pytest.fixture
def database_session(
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


def gmail_message(message_id: str, subject: str) -> dict[str, Any]:
    body = base64.urlsafe_b64encode(f"Body for {subject}".encode()).decode()
    return {
        "id": message_id,
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/plain",
            "headers": [
                {"name": "From", "value": "notices@example.edu"},
                {"name": "To", "value": "student@example.edu"},
                {"name": "Subject", "value": subject},
            ],
            "body": {"data": body},
        },
    }


def make_synchronizer(
    gmail_client: FakeGmailClient,
    session: Session,
) -> GmailSyncService:
    repository = EmailRepository(session)
    return GmailSyncService(
        gmail_client=gmail_client,
        ingestion_service=IngestionService(repository),
    )


def test_multiple_message_synchronization(
    database_session: tuple[Session, Engine],
) -> None:
    session, _ = database_session
    fake_client = FakeGmailClient(
        {
            "gmail-a": gmail_message("gmail-a", "Announcement A"),
            "gmail-b": gmail_message("gmail-b", "Announcement B"),
        }
    )

    summary = make_synchronizer(fake_client, session).sync(max_results=10)

    assert summary.fetched == 2
    assert summary.created == 2
    assert summary.already_existed == 0
    assert summary.failed == 0
    assert fake_client.requested_max_results == [10]
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 2


def test_duplicate_synchronization_does_not_create_rows(
    database_session: tuple[Session, Engine],
) -> None:
    session, _ = database_session
    fake_client = FakeGmailClient(
        {"gmail-a": gmail_message("gmail-a", "Announcement A")}
    )
    synchronizer = make_synchronizer(fake_client, session)

    first = synchronizer.sync()
    second = synchronizer.sync()

    assert first.created == 1
    assert second.created == 0
    assert second.already_existed == 1
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 1


def test_single_message_api_failure_is_counted_and_sync_continues(
    database_session: tuple[Session, Engine],
) -> None:
    session, _ = database_session
    fake_client = FakeGmailClient(
        {
            "gmail-a": gmail_message("gmail-a", "Announcement A"),
            "gmail-b": gmail_message("gmail-b", "Announcement B"),
        },
        failing_message_ids={"gmail-a"},
    )

    summary = make_synchronizer(fake_client, session).sync()

    assert summary.fetched == 2
    assert summary.created == 1
    assert summary.failed == 1
    assert session.scalar(select(func.count()).select_from(RawEmail)) == 1


def test_list_api_failure_returns_failed_summary(
    database_session: tuple[Session, Engine],
) -> None:
    session, _ = database_session
    fake_client = FakeGmailClient({}, list_failure=True)

    summary = make_synchronizer(fake_client, session).sync()

    assert summary.fetched == 0
    assert summary.created == 0
    assert summary.already_existed == 0
    assert summary.failed == 1
