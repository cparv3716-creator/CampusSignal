from dataclasses import dataclass
import logging
from typing import Any

from app.integrations.gmail.client import GmailClient, StaleGmailHistoryError
from app.repositories.gmail_state import GmailStateRepository
from app.services.gmail_source import GmailEmailSource
from app.services.gmail_sync import GmailSyncService
from app.services.ingestion import IngestionService

logger = logging.getLogger(__name__)


class MissingGmailHistoryStateError(RuntimeError):
    """A listener was started before Gmail watch state was initialized."""


@dataclass(frozen=True)
class GmailHistorySyncSummary:
    history_records: int = 0
    messages_found: int = 0
    created: int = 0
    already_existed: int = 0
    recovered_from_stale_history: bool = False


class GmailHistorySyncService:
    """Process all Gmail history after the last fully completed history ID."""

    def __init__(
        self,
        gmail_client: GmailClient,
        ingestion_service: IngestionService,
        state_repository: GmailStateRepository,
        recovery_sync_service: GmailSyncService,
        recovery_max_results: int = 10,
    ) -> None:
        self.gmail_client = gmail_client
        self.ingestion_service = ingestion_service
        self.state_repository = state_repository
        self.recovery_sync_service = recovery_sync_service
        self.recovery_max_results = recovery_max_results

    def sync(self, notification_history_id: str) -> GmailHistorySyncSummary:
        state = self.state_repository.get()
        if state is None:
            raise MissingGmailHistoryStateError(
                "Register Gmail watch before starting the listener"
            )

        if _history_id_number(notification_history_id) <= _history_id_number(
            state.last_history_id
        ):
            # This includes the immediate notification published by users.watch
            # and normal Pub/Sub redelivery after a completed synchronization.
            return GmailHistorySyncSummary()

        try:
            pages = self._load_all_history_pages(state.last_history_id)
        except StaleGmailHistoryError:
            return self._recover_from_stale_history()

        message_ids = _message_added_ids(pages)
        created = 0
        already_existed = 0
        for message_id in message_ids:
            message = self.gmail_client.get_message(message_id)
            result = self.ingestion_service.ingest(GmailEmailSource(message))
            if result.created:
                created += 1
            else:
                already_existed += 1

        newest_history_id = next(
            (
                str(page["historyId"])
                for page in reversed(pages)
                if page.get("historyId") is not None
            ),
            notification_history_id,
        )
        self.state_repository.update_last_history_id(newest_history_id)
        return GmailHistorySyncSummary(
            history_records=sum(len(page.get("history", [])) for page in pages),
            messages_found=len(message_ids),
            created=created,
            already_existed=already_existed,
        )

    def _load_all_history_pages(self, start_history_id: str) -> list[dict[str, Any]]:
        pages: list[dict[str, Any]] = []
        page_token: str | None = None
        seen_page_tokens: set[str] = set()

        while True:
            page = self.gmail_client.list_history_page(
                start_history_id=start_history_id,
                page_token=page_token,
            )
            pages.append(page)
            next_page_token = page.get("nextPageToken")
            if not next_page_token:
                return pages
            if next_page_token in seen_page_tokens:
                raise RuntimeError("Gmail history pagination repeated a page token")
            seen_page_tokens.add(next_page_token)
            page_token = str(next_page_token)

    def _recover_from_stale_history(self) -> GmailHistorySyncSummary:
        logger.warning(
            "Stored Gmail history is stale; running bounded inbox recovery"
        )
        recovery = self.recovery_sync_service.sync(self.recovery_max_results)
        if recovery.failed:
            raise RuntimeError("Bounded Gmail recovery synchronization failed")

        current_history_id = self.gmail_client.get_current_history_id()
        self.state_repository.update_last_history_id(current_history_id)
        return GmailHistorySyncSummary(
            messages_found=recovery.fetched,
            created=recovery.created,
            already_existed=recovery.already_existed,
            recovered_from_stale_history=True,
        )


def _message_added_ids(pages: list[dict[str, Any]]) -> list[str]:
    message_ids: list[str] = []
    seen: set[str] = set()
    for page in pages:
        for history_record in page.get("history", []):
            for addition in history_record.get("messagesAdded", []):
                message_id = (addition.get("message") or {}).get("id")
                if message_id and message_id not in seen:
                    seen.add(message_id)
                    message_ids.append(message_id)
    return message_ids


def _history_id_number(history_id: str) -> int:
    try:
        value = int(history_id)
    except (TypeError, ValueError) as exc:
        raise ValueError("Gmail historyId must be an integer string") from exc
    if value < 0:
        raise ValueError("Gmail historyId must not be negative")
    return value
