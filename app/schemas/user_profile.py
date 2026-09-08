from datetime import datetime, timezone

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator


class UserProfileCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    email: EmailStr
    department: str = Field(min_length=1, max_length=255)
    programme: str = Field(min_length=1, max_length=255)
    year: int = Field(ge=1)
    semester: int | None = Field(default=None, ge=1)
    interests: list[str] = Field(min_length=1)

    @field_validator("interests")
    @classmethod
    def normalize_interests(cls, value: list[str]) -> list[str]:
        normalized = []
        seen: set[str] = set()
        for interest in value:
            interest = interest.strip().lower()
            if not interest:
                raise ValueError("interests must not contain empty strings")
            if interest not in seen:
                normalized.append(interest)
                seen.add(interest)
        return normalized


class UserProfileRead(UserProfileCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime

    @field_validator("created_at")
    @classmethod
    def normalize_database_datetime(cls, value: datetime) -> datetime:
        # SQLite does not preserve timezone metadata. Stored values are always UTC.
        if value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value.astimezone(timezone.utc)
