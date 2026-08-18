from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.email import RawEmail
from app.schemas.email import CanonicalEmail


class EmailRepository:
    """Database operations used by email ingestion."""

    def __init__(self, session: Session) -> None:
        self.session = session

    def get_by_external_message_id(self, external_message_id: str) -> RawEmail | None:
        statement = select(RawEmail).where(
            RawEmail.external_message_id == external_message_id
        )
        return self.session.scalar(statement)

    def add(self, email: CanonicalEmail) -> RawEmail:
        record = RawEmail(**email.model_dump())
        self.session.add(record)
        return record

    def commit(self) -> None:
        self.session.commit()

    def rollback(self) -> None:
        self.session.rollback()

    def refresh(self, email: RawEmail) -> None:
        self.session.refresh(email)
