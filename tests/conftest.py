from collections.abc import Generator
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app


@pytest.fixture
def database_path(tmp_path: Path) -> Path:
    return tmp_path / "test_campus_signal.db"


@pytest.fixture
def client(database_path: Path) -> Generator[TestClient, None, None]:
    database_url = f"sqlite:///{database_path.as_posix()}"
    app = create_app(Settings(database_url=database_url))
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture
def valid_email_payload() -> dict[str, object]:
    return {
        "external_message_id": "demo-mail-001",
        "sender": "ecell@example.edu",
        "recipients": ["student@example.edu"],
        "subject": "Startup Case Competition",
        "body_text": "Registration closes on 25 August.",
        "received_at": "2026-08-15T12:00:00Z",
        "source": "manual_test",
    }
