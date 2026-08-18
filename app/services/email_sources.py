from typing import Protocol

from app.schemas.email import CanonicalEmail, EmailCreate


class EmailSource(Protocol):
    """Adapter contract for one normalized email supplied to ingestion."""

    def fetch(self) -> CanonicalEmail:
        """Return an email in the source-independent canonical shape."""
        ...


class ManualEmailSource:
    """Local adapter used by the prototype JSON endpoint."""

    def __init__(self, payload: EmailCreate) -> None:
        self.payload = payload

    def fetch(self) -> CanonicalEmail:
        return CanonicalEmail.model_validate(self.payload.model_dump(mode="python"))
