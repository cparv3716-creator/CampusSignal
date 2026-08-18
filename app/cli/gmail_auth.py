from app.core.config import get_settings
from app.integrations.gmail.auth import authorize_gmail


def main() -> int:
    settings = get_settings()
    try:
        authorize_gmail(
            credentials_file=settings.gmail_credentials_file,
            token_file=settings.gmail_token_file,
        )
    except FileNotFoundError as exc:
        print(f"Gmail authentication failed: {exc}")
        return 1
    except Exception as exc:
        print(f"Gmail authentication failed: {type(exc).__name__}")
        return 1

    print(f"Gmail authentication complete. Token saved to {settings.gmail_token_file}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
