from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="CAMPUS_SIGNAL_",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "CampusSignal API"
    database_url: str = "sqlite:///./campus_signal.db"
    gmail_credentials_file: Path = Field(
        default=Path("credentials.json"),
        validation_alias="GMAIL_CREDENTIALS_FILE",
    )
    gmail_token_file: Path = Field(
        default=Path("token.json"),
        validation_alias="GMAIL_TOKEN_FILE",
    )
    gmail_user_id: str = Field(default="me", validation_alias="GMAIL_USER_ID")
    google_cloud_project: str | None = Field(
        default=None,
        validation_alias="GOOGLE_CLOUD_PROJECT",
    )
    gmail_pubsub_topic: str | None = Field(
        default=None,
        validation_alias="GMAIL_PUBSUB_TOPIC",
    )
    gmail_pubsub_subscription: str | None = Field(
        default=None,
        validation_alias="GMAIL_PUBSUB_SUBSCRIPTION",
    )
    def gmail_topic_path(self) -> str:
        return self._qualified_pubsub_name(self.gmail_pubsub_topic, "topics")

    def gmail_subscription_path(self) -> str:
        return self._qualified_pubsub_name(
            self.gmail_pubsub_subscription,
            "subscriptions",
        )

    def _qualified_pubsub_name(self, value: str | None, resource: str) -> str:
        setting_name = (
            "GMAIL_PUBSUB_TOPIC"
            if resource == "topics"
            else "GMAIL_PUBSUB_SUBSCRIPTION"
        )
        if not value:
            raise ValueError(f"{setting_name} is not configured")
        if value.startswith("projects/"):
            return value
        if not self.google_cloud_project:
            raise ValueError(
                f"GOOGLE_CLOUD_PROJECT is required when {setting_name} is not "
                "fully qualified"
            )
        return f"projects/{self.google_cloud_project}/{resource}/{value}"


@lru_cache
def get_settings() -> Settings:
    return Settings()
