from dataclasses import dataclass

from sqlalchemy.exc import IntegrityError

from app.models.email import RawEmail
from app.repositories.email import EmailRepository
from app.services.email_sources import EmailSource


@dataclass(frozen=True)
class IngestionResult:
    email: RawEmail
    created: bool


class IngestionService:
    """Source-independent, idempotent raw-email ingestion."""

    def __init__(self, repository: EmailRepository) -> None:
        self.repository = repository

    def ingest(self, source: EmailSource) -> IngestionResult:
        incoming = source.fetch()
        existing = self.repository.get_by_external_message_id(
            incoming.external_message_id
        )
        if existing is not None:
            return IngestionResult(email=existing, created=False)

        record = self.repository.add(incoming)
        try:
            self.repository.commit()
        except IntegrityError:
            # The unique database constraint also protects concurrent requests.
            self.repository.rollback()
            existing = self.repository.get_by_external_message_id(
                incoming.external_message_id
            )
            if existing is None:
                raise
            return IngestionResult(email=existing, created=False)

        self.repository.refresh(record)
        return IngestionResult(email=record, created=True)
