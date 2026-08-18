from app.core.config import get_settings
from app.db.session import create_database_engine, create_session_factory, init_db
from app.integrations.gmail.auth import authorize_gmail
from app.integrations.gmail.client import GmailApiClient
from app.repositories.gmail_state import GmailStateRepository
from app.services.gmail_watch import GmailWatchService


def main() -> int:
    settings = get_settings()
    try:
        topic_path = settings.gmail_topic_path()
        credentials = authorize_gmail(
            credentials_file=settings.gmail_credentials_file,
            token_file=settings.gmail_token_file,
        )
        gmail_client = GmailApiClient.from_credentials(
            credentials=credentials,
            user_id=settings.gmail_user_id,
        )
    except FileNotFoundError as exc:
        print(f"Gmail watch setup failed: {exc}")
        return 1
    except Exception as exc:
        print(f"Gmail watch setup failed: {type(exc).__name__}")
        return 1

    engine = create_database_engine(settings.database_url)
    init_db(engine)
    session = create_session_factory(engine)()
    try:
        registration = GmailWatchService(
            gmail_client=gmail_client,
            state_repository=GmailStateRepository(session),
            topic_name=topic_path,
        ).register()
    except Exception as exc:
        print(f"Gmail watch registration failed: {type(exc).__name__}")
        return 1
    finally:
        session.close()
        engine.dispose()

    print("Watch registered")
    print(f"History ID: {registration.history_id}")
    print(f"Expiration: {registration.expires_at.isoformat()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
