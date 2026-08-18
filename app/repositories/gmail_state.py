from datetime import datetime

from sqlalchemy.orm import Session

from app.models.gmail_state import GmailWatchState

GMAIL_STATE_ID = 1


class GmailStateRepository:
    """Persist watch metadata and the last fully processed history ID."""

    def __init__(self, session: Session) -> None:
        self.session = session

    def get(self) -> GmailWatchState | None:
        return self.session.get(GmailWatchState, GMAIL_STATE_ID)

    def record_watch(
        self,
        history_id: str,
        expires_at: datetime,
    ) -> GmailWatchState:
        state = self.get()
        if state is None:
            state = GmailWatchState(
                id=GMAIL_STATE_ID,
                last_history_id=history_id,
                watch_history_id=history_id,
                watch_expires_at=expires_at,
            )
            self.session.add(state)
        else:
            # A renewal must not skip history that has not been processed yet.
            state.watch_history_id = history_id
            state.watch_expires_at = expires_at

        self.session.commit()
        self.session.refresh(state)
        return state

    def update_last_history_id(self, history_id: str) -> GmailWatchState:
        state = self.get()
        if state is None:
            raise RuntimeError("Gmail watch state has not been initialized")
        state.last_history_id = history_id
        self.session.commit()
        self.session.refresh(state)
        return state
