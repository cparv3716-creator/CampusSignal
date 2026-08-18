from dataclasses import dataclass

from app.integrations.gmail.client import GmailClient
from app.services.gmail_source import GmailEmailSource
from app.services.ingestion import IngestionService


@dataclass(frozen=True)
class GmailSyncSummary:
    fetched: int = 0
    created: int = 0
    already_existed: int = 0
    failed: int = 0


class GmailSyncService:
    """Coordinate one Gmail inbox fetch through the existing ingestion service."""

    def __init__(
        self,
        gmail_client: GmailClient,
        ingestion_service: IngestionService,
    ) -> None:
        self.gmail_client = gmail_client
        self.ingestion_service = ingestion_service

    def sync(self, max_results: int = 10) -> GmailSyncSummary:
        try:
            message_ids = self.gmail_client.list_recent_inbox_message_ids(max_results)
        except Exception:
            return GmailSyncSummary(failed=1)

        created = 0
        already_existed = 0
        failed = 0

        for message_id in message_ids:
            try:
                message = self.gmail_client.get_message(message_id)
                result = self.ingestion_service.ingest(GmailEmailSource(message))
            except Exception:
                failed += 1
                continue

            if result.created:
                created += 1
            else:
                already_existed += 1

        return GmailSyncSummary(
            fetched=len(message_ids),
            created=created,
            already_existed=already_existed,
            failed=failed,
        )
