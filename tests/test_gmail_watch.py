from collections.abc import Generator
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import Mock

import pytest
from sqlalchemy import Engine
from sqlalchemy.orm import Session

from app.db.session import create_database_engine, create_session_factory, init_db
from app.integrations.gmail.client import GmailApiClient, GmailWatchResult
from app.repositories.gmail_state import GmailStateRepository
from app.services.gmail_watch import GmailWatchService


class FakeWatchClient:
    def __init__(self, results: list[GmailWatchResult]) -> None:
        self.results = iter(results)
        self.topic_names: list[str] = []

    def watch_inbox(self, topic_name: str) -> GmailWatchResult:
        self.topic_names.append(topic_name)
        return next(self.results)


@pytest.fixture
def watch_database(
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


def test_gmail_api_watch_uses_inbox_include_filter() -> None:
    request = Mock()
    request.execute.return_value = {
        "historyId": "100",
        "expiration": "1787000000000",
    }
    users = Mock()
    users.watch.return_value = request
    service = Mock()
    service.users.return_value = users
    client = GmailApiClient(service=service, user_id="me")

    result = client.watch_inbox("projects/project-a/topics/gmail-notifications")

    users.watch.assert_called_once_with(
        userId="me",
        body={
            "topicName": "projects/project-a/topics/gmail-notifications",
            "labelIds": ["INBOX"],
            "labelFilterBehavior": "INCLUDE",
        },
    )
    assert result.history_id == "100"


def test_successful_watch_registration_persists_state(
    watch_database: tuple[Session, Engine],
) -> None:
    session, engine = watch_database
    expires_at = datetime.now(timezone.utc) + timedelta(days=7)
    fake_client = FakeWatchClient(
        [GmailWatchResult(history_id="100", expires_at=expires_at)]
    )
    topic = "projects/project-a/topics/gmail-notifications"

    registration = GmailWatchService(
        gmail_client=fake_client,
        state_repository=GmailStateRepository(session),
        topic_name=topic,
    ).register()

    assert registration.history_id == "100"
    assert registration.last_processed_history_id == "100"
    assert fake_client.topic_names == [topic]

    session.close()
    reopened_session = create_session_factory(engine)()
    try:
        persisted = GmailStateRepository(reopened_session).get()
        assert persisted is not None
        assert persisted.last_history_id == "100"
        assert persisted.watch_history_id == "100"
        assert persisted.watch_expires_at is not None
    finally:
        reopened_session.close()


def test_watch_renewal_updates_expiration_without_skipping_history(
    watch_database: tuple[Session, Engine],
) -> None:
    session, _ = watch_database
    now = datetime.now(timezone.utc)
    fake_client = FakeWatchClient(
        [
            GmailWatchResult(history_id="100", expires_at=now + timedelta(days=7)),
            GmailWatchResult(history_id="150", expires_at=now + timedelta(days=14)),
        ]
    )
    repository = GmailStateRepository(session)
    service = GmailWatchService(
        gmail_client=fake_client,
        state_repository=repository,
        topic_name="projects/project-a/topics/gmail-notifications",
    )

    service.register()
    repository.update_last_history_id("120")
    renewal = service.register()

    state = repository.get()
    assert state is not None
    assert renewal.history_id == "150"
    assert renewal.last_processed_history_id == "120"
    assert state.last_history_id == "120"
    assert state.watch_history_id == "150"
