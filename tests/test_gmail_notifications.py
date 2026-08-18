import json

import pytest

from app.services.gmail_notifications import (
    GmailNotificationProcessor,
    InvalidGmailNotificationError,
    decode_gmail_notification,
)


class FakePubSubMessage:
    def __init__(self, data: bytes) -> None:
        self.data = data
        self.ack_count = 0

    def ack(self) -> None:
        self.ack_count += 1


def notification_json() -> bytes:
    return json.dumps(
        {"emailAddress": "student@example.edu", "historyId": "12345"}
    ).encode()


def test_python_pubsub_message_data_is_utf8_json_bytes() -> None:
    data = b'{"emailAddress":"user@example.com","historyId":"123456"}'

    notification = decode_gmail_notification(data)

    assert notification.email_address == "user@example.com"
    assert notification.history_id == "123456"


def test_pubsub_client_decoded_json_bytes_are_supported() -> None:
    notification = decode_gmail_notification(notification_json())

    assert notification.history_id == "12345"


def test_numeric_gmail_history_id_is_normalized_to_string() -> None:
    data = b'{"emailAddress":"user@example.com","historyId":1001302}'

    notification = decode_gmail_notification(data)

    assert notification.history_id == "1001302"


@pytest.mark.parametrize(
    "data",
    [
        b"\xff\xfe",
        b"not-json",
        b"{}",
        b'{"emailAddress":"student@example.edu"}',
        b'{"historyId":"12345"}',
        b'{"emailAddress":"student@example.edu","historyId":"bad"}',
        b'{"emailAddress":"student@example.edu","historyId":true}',
        b'{"emailAddress":"student@example.edu","historyId":-1}',
    ],
)
def test_malformed_notification_handling(data: bytes) -> None:
    with pytest.raises(InvalidGmailNotificationError):
        decode_gmail_notification(data)


def test_pubsub_message_acknowledged_only_after_success() -> None:
    handled: list[str] = []
    message = FakePubSubMessage(notification_json())
    processor = GmailNotificationProcessor(
        lambda notification: handled.append(notification.history_id)
    )

    succeeded = processor.process(message)

    assert succeeded is True
    assert handled == ["12345"]
    assert message.ack_count == 1


def test_pubsub_message_not_acknowledged_on_processing_failure() -> None:
    message = FakePubSubMessage(notification_json())

    def fail(_: object) -> None:
        raise RuntimeError("simulated processing failure")

    succeeded = GmailNotificationProcessor(fail).process(message)

    assert succeeded is False
    assert message.ack_count == 0


def test_malformed_pubsub_message_is_not_acknowledged() -> None:
    message = FakePubSubMessage(b"invalid")

    succeeded = GmailNotificationProcessor(lambda _: None).process(message)

    assert succeeded is False
    assert message.ack_count == 0
