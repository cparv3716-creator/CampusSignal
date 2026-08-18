import argparse

from app.core.config import get_settings
from app.db.session import create_database_engine, create_session_factory, init_db
from app.integrations.gmail.auth import authorize_gmail
from app.integrations.gmail.client import GmailApiClient
from app.repositories.email import EmailRepository
from app.services.gmail_sync import GmailSyncService, GmailSyncSummary
from app.services.ingestion import IngestionService


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch recent Gmail inbox messages into CampusSignal once."
    )
    parser.add_argument(
        "--max-results",
        type=int,
        default=10,
        help="Number of inbox messages to fetch (1-500; default: 10).",
    )
    args = parser.parse_args()
    if not 1 <= args.max_results <= 500:
        parser.error("--max-results must be between 1 and 500")
    return args


def _print_summary(summary: GmailSyncSummary) -> None:
    print(f"Fetched: {summary.fetched}")
    print(f"Created: {summary.created}")
    print(f"Already existed: {summary.already_existed}")
    print(f"Failed: {summary.failed}")


def main() -> int:
    args = _parse_args()
    settings = get_settings()

    try:
        credentials = authorize_gmail(
            credentials_file=settings.gmail_credentials_file,
            token_file=settings.gmail_token_file,
        )
        gmail_client = GmailApiClient.from_credentials(
            credentials=credentials,
            user_id=settings.gmail_user_id,
        )
    except FileNotFoundError as exc:
        print(f"Gmail setup failed: {exc}")
        _print_summary(GmailSyncSummary(failed=1))
        return 1
    except Exception as exc:
        print(f"Gmail setup failed: {type(exc).__name__}")
        _print_summary(GmailSyncSummary(failed=1))
        return 1

    engine = create_database_engine(settings.database_url)
    session_factory = create_session_factory(engine)
    init_db(engine)
    session = session_factory()
    try:
        repository = EmailRepository(session)
        synchronizer = GmailSyncService(
            gmail_client=gmail_client,
            ingestion_service=IngestionService(repository),
        )
        summary = synchronizer.sync(max_results=args.max_results)
    finally:
        session.close()
        engine.dispose()

    _print_summary(summary)
    return 1 if summary.failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
