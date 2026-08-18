from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Protocol

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError


class StaleGmailHistoryError(RuntimeError):
    """The stored Gmail history ID can no longer be queried."""


@dataclass(frozen=True)
class GmailWatchResult:
    history_id: str
    expires_at: datetime


class GmailClient(Protocol):
    """Small Gmail API boundary used by synchronization."""

    def list_recent_inbox_message_ids(self, max_results: int) -> list[str]:
        ...

    def get_message(self, message_id: str) -> dict[str, Any]:
        ...

    def watch_inbox(self, topic_name: str) -> GmailWatchResult:
        ...

    def list_history_page(
        self,
        start_history_id: str,
        page_token: str | None = None,
    ) -> dict[str, Any]:
        ...

    def get_current_history_id(self) -> str:
        ...


class GmailApiClient:
    """Adapter around the generated Google Gmail API client."""

    def __init__(self, service: Any, user_id: str = "me") -> None:
        self.service = service
        self.user_id = user_id

    @classmethod
    def from_credentials(
        cls,
        credentials: Credentials,
        user_id: str = "me",
    ) -> "GmailApiClient":
        service = build("gmail", "v1", credentials=credentials, cache_discovery=False)
        return cls(service=service, user_id=user_id)

    def list_recent_inbox_message_ids(self, max_results: int) -> list[str]:
        if not 1 <= max_results <= 500:
            raise ValueError("max_results must be between 1 and 500")

        response = (
            self.service.users()
            .messages()
            .list(
                userId=self.user_id,
                labelIds=["INBOX"],
                maxResults=max_results,
            )
            .execute()
        )
        return [
            message["id"]
            for message in response.get("messages", [])
            if message.get("id")
        ]

    def get_message(self, message_id: str) -> dict[str, Any]:
        return (
            self.service.users()
            .messages()
            .get(
                userId=self.user_id,
                id=message_id,
                format="full",
            )
            .execute()
        )

    def watch_inbox(self, topic_name: str) -> GmailWatchResult:
        response = (
            self.service.users()
            .watch(
                userId=self.user_id,
                body={
                    "topicName": topic_name,
                    "labelIds": ["INBOX"],
                    "labelFilterBehavior": "INCLUDE",
                },
            )
            .execute()
        )
        try:
            history_id = str(response["historyId"])
            expiration_ms = int(response["expiration"])
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError("Gmail watch returned an invalid response") from exc
        return GmailWatchResult(
            history_id=history_id,
            expires_at=datetime.fromtimestamp(
                expiration_ms / 1000,
                tz=timezone.utc,
            ),
        )

    def list_history_page(
        self,
        start_history_id: str,
        page_token: str | None = None,
    ) -> dict[str, Any]:
        request_arguments: dict[str, Any] = {
            "userId": self.user_id,
            "startHistoryId": start_history_id,
            "historyTypes": ["messageAdded"],
            "labelId": "INBOX",
            "maxResults": 500,
        }
        if page_token:
            request_arguments["pageToken"] = page_token

        try:
            return (
                self.service.users()
                .history()
                .list(**request_arguments)
                .execute()
            )
        except HttpError as exc:
            if exc.resp.status == 404:
                raise StaleGmailHistoryError from exc
            raise

    def get_current_history_id(self) -> str:
        response = (
            self.service.users()
            .getProfile(userId=self.user_id)
            .execute()
        )
        try:
            return str(response["historyId"])
        except (KeyError, TypeError) as exc:
            raise ValueError("Gmail profile returned no historyId") from exc
