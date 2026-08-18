import json
import logging
from dataclasses import dataclass
from threading import Lock
from typing import Callable, Protocol

logger = logging.getLogger(__name__)


class InvalidGmailNotificationError(ValueError):
    """The Pub/Sub message does not contain a valid Gmail notification."""


@dataclass(frozen=True)
class GmailNotification:
    email_address: str
    history_id: str


class AcknowledgeableMessage(Protocol):
    data: bytes

    def ack(self) -> None:
        ...


def decode_gmail_notification(data: bytes) -> GmailNotification:
    """Decode JSON bytes delivered by the Python Pub/Sub pull client."""
    try:
        payload = json.loads(data.decode("utf-8"))
    except UnicodeDecodeError as exc:
        raise InvalidGmailNotificationError(
            "Gmail notification data is not valid UTF-8"
        ) from exc
    except json.JSONDecodeError as exc:
        raise InvalidGmailNotificationError(
            "Gmail notification data is not valid JSON"
        ) from exc

    if not isinstance(payload, dict):
        raise InvalidGmailNotificationError(
            "Gmail notification data is not a JSON object"
        )

    email_address = payload.get("emailAddress")
    history_id = payload.get("historyId")
    if not isinstance(email_address, str) or not email_address.strip():
        raise InvalidGmailNotificationError(
            "Gmail notification is missing emailAddress"
        )
    if isinstance(history_id, bool):
        raise InvalidGmailNotificationError(
            "Gmail notification has an invalid historyId"
        )
    if isinstance(history_id, int):
        if history_id < 0:
            raise InvalidGmailNotificationError(
                "Gmail notification has an invalid historyId"
            )
        history_id = str(history_id)
    elif not isinstance(history_id, str) or not history_id.isdigit():
        raise InvalidGmailNotificationError(
            "Gmail notification has an invalid historyId"
        )
    return GmailNotification(
        email_address=email_address.strip(),
        history_id=history_id,
    )


class GmailNotificationProcessor:
    """Decode, process, then acknowledge one Pub/Sub delivery."""

    def __init__(
        self,
        handler: Callable[[GmailNotification], None],
    ) -> None:
        self.handler = handler
        self._lock = Lock()

    def process(self, message: AcknowledgeableMessage) -> bool:
        with self._lock:
            try:
                notification = decode_gmail_notification(message.data)
                self.handler(notification)
            except Exception as exc:
                logger.error(
                    "Gmail notification processing failed: %s",
                    type(exc).__name__,
                )
                return False

            message.ack()
            return True
