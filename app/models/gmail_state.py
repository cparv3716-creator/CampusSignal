from datetime import datetime, timezone

from sqlalchemy import DateTime, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class GmailWatchState(Base):
    """Singleton state for one locally monitored Gmail mailbox."""

    __tablename__ = "gmail_watch_state"

    id: Mapped[int] = mapped_column(primary_key=True, default=1)
    last_history_id: Mapped[str] = mapped_column(String(64), nullable=False)
    watch_history_id: Mapped[str] = mapped_column(String(64), nullable=False)
    watch_expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False,
    )
