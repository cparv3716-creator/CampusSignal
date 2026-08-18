from datetime import datetime, timezone

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.models.email import ProcessingStatus


class EmailCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    external_message_id: str = Field(min_length=1, max_length=255)
    sender: EmailStr
    recipients: list[EmailStr] = Field(min_length=1)
    subject: str = Field(min_length=1, max_length=998)
    body_text: str = Field(min_length=1)
    received_at: datetime
    source: str = Field(min_length=1, max_length=50)

    @field_validator("received_at")
    @classmethod
    def require_timezone(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("received_at must include a timezone offset")
        return value.astimezone(timezone.utc)


class CanonicalEmail(BaseModel):
    external_message_id: str
    sender: str
    recipients: list[str]
    subject: str
    body_text: str
    received_at: datetime
    source: str


class EmailRead(CanonicalEmail):
    model_config = ConfigDict(from_attributes=True)

    id: int
    processing_status: ProcessingStatus
    created_at: datetime

    @field_validator("received_at", "created_at")
    @classmethod
    def normalize_database_datetime(cls, value: datetime) -> datetime:
        # SQLite does not preserve timezone metadata. Stored values are always UTC.
        if value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value.astimezone(timezone.utc)
