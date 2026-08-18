from pathlib import Path

from fastapi.testclient import TestClient
from sqlalchemy import func, select

from app.models.email import ProcessingStatus, RawEmail


def test_valid_email_ingestion(
    client: TestClient, valid_email_payload: dict[str, object]
) -> None:
    response = client.post("/api/v1/emails", json=valid_email_payload)

    assert response.status_code == 201
    result = response.json()
    assert result["id"] == 1
    assert result["external_message_id"] == "demo-mail-001"
    assert result["recipients"] == ["student@example.edu"]
    assert result["processing_status"] == ProcessingStatus.RECEIVED.value
    assert result["created_at"]


def test_invalid_email_request(client: TestClient) -> None:
    response = client.post(
        "/api/v1/emails",
        json={
            "external_message_id": "bad-mail-001",
            "sender": "not-an-email-address",
            "recipients": [],
            "subject": "",
            "body_text": "Missing several valid values",
            "received_at": "2026-08-15T12:00:00",
            "source": "manual_test",
        },
    )

    assert response.status_code == 422


def test_duplicate_message_ingestion(
    client: TestClient, valid_email_payload: dict[str, object]
) -> None:
    first_response = client.post("/api/v1/emails", json=valid_email_payload)
    second_response = client.post("/api/v1/emails", json=valid_email_payload)

    assert first_response.status_code == 201
    assert second_response.status_code == 200
    assert second_response.json()["id"] == first_response.json()["id"]

    session = client.app.state.session_factory()
    try:
        count = session.scalar(select(func.count()).select_from(RawEmail))
    finally:
        session.close()
    assert count == 1


def test_email_is_persisted_to_sqlite(
    client: TestClient,
    database_path: Path,
    valid_email_payload: dict[str, object],
) -> None:
    response = client.post("/api/v1/emails", json=valid_email_payload)
    assert response.status_code == 201

    session = client.app.state.session_factory()
    try:
        stored_email = session.scalar(
            select(RawEmail).where(
                RawEmail.external_message_id == "demo-mail-001"
            )
        )
        assert stored_email is not None
        assert stored_email.subject == "Startup Case Competition"
        assert stored_email.processing_status is ProcessingStatus.RECEIVED
    finally:
        session.close()

    assert database_path.exists()
