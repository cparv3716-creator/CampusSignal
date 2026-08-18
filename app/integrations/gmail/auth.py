from pathlib import Path
from typing import Final

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow

GMAIL_READONLY_SCOPE: Final = "https://www.googleapis.com/auth/gmail.readonly"
GMAIL_SCOPES: Final = [GMAIL_READONLY_SCOPE]


def authorize_gmail(
    credentials_file: Path,
    token_file: Path,
) -> Credentials:
    """Load, refresh, or interactively create local Gmail OAuth credentials."""
    credentials: Credentials | None = None

    if token_file.is_file():
        credentials = Credentials.from_authorized_user_file(
            str(token_file),
            GMAIL_SCOPES,
        )

    if credentials and credentials.valid:
        return credentials

    if credentials and credentials.expired and credentials.refresh_token:
        credentials.refresh(Request())
    else:
        if not credentials_file.is_file():
            raise FileNotFoundError(
                f"Gmail OAuth credentials file not found: {credentials_file}"
            )
        flow = InstalledAppFlow.from_client_secrets_file(
            str(credentials_file),
            GMAIL_SCOPES,
        )
        credentials = flow.run_local_server(port=0)

    token_file.parent.mkdir(parents=True, exist_ok=True)
    token_file.write_text(credentials.to_json(), encoding="utf-8")
    return credentials
