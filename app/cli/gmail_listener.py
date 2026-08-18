import logging

from app.core.config import get_settings
from app.db.session import create_database_engine, create_session_factory, init_db
from app.integrations.gmail.auth import authorize_gmail
from app.integrations.gmail.client import GmailApiClient
from app.integrations.pubsub.subscriber import PubSubPullSubscriber
from app.repositories.email import EmailRepository
from app.repositories.gmail_state import GmailStateRepository
from app.services.gmail_history import GmailHistorySyncService
from app.services.gmail_notifications import (
    GmailNotification,
    GmailNotificationProcessor,
)
from app.services.gmail_sync import GmailSyncService
from app.services.ingestion import IngestionService


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    settings = get_settings()
    try:
        subscription_path = settings.gmail_subscription_path()
        credentials = authorize_gmail(
            credentials_file=settings.gmail_credentials_file,
            token_file=settings.gmail_token_file,
        )
        gmail_client = GmailApiClient.from_credentials(
            credentials=credentials,
            user_id=settings.gmail_user_id,
        )
    except FileNotFoundError as exc:
        print(f"Gmail listener setup failed: {exc}")
        return 1
    except Exception as exc:
        print(f"Gmail listener setup failed: {type(exc).__name__}")
        return 1

    engine = create_database_engine(settings.database_url)
    init_db(engine)
    session_factory = create_session_factory(engine)

    def handle_notification(notification: GmailNotification) -> None:
        session = session_factory()
        try:
            email_repository = EmailRepository(session)
            ingestion_service = IngestionService(email_repository)
            summary = GmailHistorySyncService(
                gmail_client=gmail_client,
                ingestion_service=ingestion_service,
                state_repository=GmailStateRepository(session),
                recovery_sync_service=GmailSyncService(
                    gmail_client=gmail_client,
                    ingestion_service=ingestion_service,
                ),
            ).sync(notification.history_id)
        finally:
            session.close()

        print(
            "History processed: "
            f"messages={summary.messages_found}, "
            f"created={summary.created}, "
            f"existing={summary.already_existed}, "
            f"recovered={summary.recovered_from_stale_history}"
        )

    processor = GmailNotificationProcessor(handle_notification)
    print(f"Listening on {subscription_path}")
    try:
        PubSubPullSubscriber(subscription_path).listen(processor.process)
    except Exception as exc:
        print(f"Pub/Sub listener stopped: {type(exc).__name__}")
        return 1
    finally:
        engine.dispose()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
