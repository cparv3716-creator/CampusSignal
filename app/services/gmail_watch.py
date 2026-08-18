from dataclasses import dataclass
from datetime import datetime

from app.integrations.gmail.client import GmailClient
from app.repositories.gmail_state import GmailStateRepository


@dataclass(frozen=True)
class GmailWatchRegistration:
    history_id: str
    expires_at: datetime
    last_processed_history_id: str


class GmailWatchService:
    """Register or renew an INBOX-only Gmail watch and persist its state."""

    def __init__(
        self,
        gmail_client: GmailClient,
        state_repository: GmailStateRepository,
        topic_name: str,
    ) -> None:
        self.gmail_client = gmail_client
        self.state_repository = state_repository
        self.topic_name = topic_name

    def register(self) -> GmailWatchRegistration:
        result = self.gmail_client.watch_inbox(self.topic_name)
        state = self.state_repository.record_watch(
            history_id=result.history_id,
            expires_at=result.expires_at,
        )
        return GmailWatchRegistration(
            history_id=result.history_id,
            expires_at=result.expires_at,
            last_processed_history_id=state.last_history_id,
        )
