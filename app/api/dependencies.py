from collections.abc import Generator

from fastapi import Request
from sqlalchemy.orm import Session


def get_db_session(request: Request) -> Generator[Session, None, None]:
    """Provide one SQLAlchemy session for an HTTP request."""
    session = request.app.state.session_factory()
    try:
        yield session
    finally:
        session.close()
