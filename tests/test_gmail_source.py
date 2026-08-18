import base64
from datetime import datetime, timezone

from app.services.gmail_source import GmailEmailSource


def encode_body(value: str, encoding: str = "utf-8") -> str:
    return base64.urlsafe_b64encode(value.encode(encoding)).decode().rstrip("=")


def test_normal_gmail_message_conversion() -> None:
    message = {
        "id": "18cafe123",
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/plain",
            "headers": [
                {"name": "From", "value": "E-Cell <ecell@example.edu>"},
                {
                    "name": "To",
                    "value": "Student One <one@example.edu>, two@example.edu",
                },
                {"name": "Subject", "value": "Startup Case Competition"},
                {"name": "Date", "value": "Sat, 15 Aug 2026 12:00:00 +0000"},
            ],
            "body": {"data": encode_body("Registration closes on 25 August.")},
        },
    }

    result = GmailEmailSource(message).fetch()

    assert result.external_message_id == "gmail:18cafe123"
    assert result.sender == "ecell@example.edu"
    assert result.recipients == ["one@example.edu", "two@example.edu"]
    assert result.subject == "Startup Case Competition"
    assert result.body_text == "Registration closes on 25 August."
    assert result.received_at == datetime(2026, 8, 15, 12, tzinfo=timezone.utc)
    assert result.source == "gmail"


def test_multipart_prefers_plain_text_and_ignores_attachments() -> None:
    message = {
        "id": "multipart-1",
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "multipart/mixed",
            "headers": [],
            "parts": [
                {
                    "mimeType": "multipart/alternative",
                    "parts": [
                        {
                            "mimeType": "text/html",
                            "body": {"data": encode_body("<p>HTML version</p>")},
                        },
                        {
                            "mimeType": "text/plain",
                            "body": {"data": encode_body("Plain version")},
                        },
                    ],
                },
                {
                    "mimeType": "text/plain",
                    "filename": "notes.txt",
                    "body": {"data": encode_body("Attachment contents")},
                },
            ],
        },
    }

    result = GmailEmailSource(message).fetch()

    assert result.body_text == "Plain version"
    assert "Attachment" not in result.body_text
    assert "HTML" not in result.body_text


def test_base64url_body_decoding_handles_unicode_and_missing_padding() -> None:
    original = "Campus café registration — आज"
    message = {
        "id": "base64url-1",
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/plain",
            "headers": [],
            "body": {"data": encode_body(original)},
        },
    }

    result = GmailEmailSource(message).fetch()

    assert result.body_text == original


def test_html_body_is_used_as_plain_text_fallback() -> None:
    message = {
        "id": "html-1",
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/html",
            "headers": [],
            "body": {
                "data": encode_body(
                    "<html><style>hidden</style><p>Hello <b>student</b>.</p>"
                    "<script>secret()</script></html>"
                )
            },
        },
    }

    result = GmailEmailSource(message).fetch()

    assert result.body_text == "Hello student."


def test_missing_optional_headers_use_safe_defaults_and_internal_date() -> None:
    message = {
        "id": "minimal-1",
        "internalDate": "1786795200000",
        "payload": {
            "mimeType": "text/plain",
            "headers": [],
            "body": {"data": encode_body("Body remains available")},
        },
    }

    result = GmailEmailSource(message).fetch()

    assert result.sender == "unknown"
    assert result.recipients == []
    assert result.subject == "(no subject)"
    assert result.received_at == datetime.fromtimestamp(
        1786795200, tz=timezone.utc
    )
