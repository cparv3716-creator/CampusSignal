from datetime import datetime, timedelta, timezone
from pathlib import Path
import subprocess
import sys

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy import inspect, select
from sqlalchemy.exc import IntegrityError

from app.models import UserProfile
from app.schemas.user_profile import UserProfileCreate, UserProfileRead


@pytest.fixture
def valid_profile_payload() -> dict[str, object]:
    return {
        "email": "student@example.edu",
        "department": "Computer Science",
        "programme": "BTech",
        "year": 1,
        "interests": ["robotics", "music"],
    }


def test_valid_profile_schema(valid_profile_payload: dict[str, object]) -> None:
    profile = UserProfileCreate.model_validate(valid_profile_payload)

    assert profile.email == "student@example.edu"
    assert profile.department == "Computer Science"
    assert profile.programme == "BTech"
    assert profile.year == 1
    assert profile.semester is None
    assert profile.interests == ["robotics", "music"]


@pytest.mark.parametrize("semester", [None, 1, 8])
def test_valid_semester(
    valid_profile_payload: dict[str, object], semester: int | None
) -> None:
    profile = UserProfileCreate.model_validate(
        {**valid_profile_payload, "semester": semester}
    )

    assert profile.semester == semester


@pytest.mark.parametrize(
    ("interests", "expected"),
    [
        (["RoBoTiCs", "MUSIC"], ["robotics", "music"]),
        (["  robotics ", "\t music\n"], ["robotics", "music"]),
        (["music", "robotics", "music"], ["music", "robotics"]),
        ([" Music ", "ROBOTICS", "music", " robotics "], ["music", "robotics"]),
        (["Quantum Computing", "New Interest"], ["quantum computing", "new interest"]),
    ],
    ids=["lowercase", "whitespace", "duplicates", "combined", "extensible"],
)
def test_interest_normalization(
    valid_profile_payload: dict[str, object],
    interests: list[str],
    expected: list[str],
) -> None:
    profile = UserProfileCreate.model_validate(
        {**valid_profile_payload, "interests": interests}
    )

    assert profile.interests == expected


def test_department_and_programme_are_trimmed(
    valid_profile_payload: dict[str, object],
) -> None:
    profile = UserProfileCreate.model_validate(
        {**valid_profile_payload, "department": " CS ", "programme": " BTech\t"}
    )

    assert profile.department == "CS"
    assert profile.programme == "BTech"


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("email", "not-an-email"),
        ("department", ""),
        ("department", " \t"),
        ("programme", ""),
        ("programme", " \t"),
        ("year", 0),
        ("year", -1),
        ("semester", 0),
        ("semester", -1),
        ("interests", []),
        ("interests", [""]),
        ("interests", [" \t"]),
        ("interests", ["music", " "]),
        ("interests", [42]),
        ("interests", "music"),
        ("interests", None),
        ("unexpected", "value"),
    ],
)
def test_invalid_profile_schema(
    valid_profile_payload: dict[str, object], field: str, value: object
) -> None:
    with pytest.raises(ValidationError) as exc_info:
        UserProfileCreate.model_validate({**valid_profile_payload, field: value})

    assert exc_info.value.errors()[0]["loc"][0] == field


@pytest.mark.parametrize("field", ["email", "department", "programme", "year", "interests"])
def test_profile_requires_fields(
    valid_profile_payload: dict[str, object], field: str
) -> None:
    del valid_profile_payload[field]

    with pytest.raises(ValidationError) as exc_info:
        UserProfileCreate.model_validate(valid_profile_payload)

    assert exc_info.value.errors()[0]["loc"] == (field,)


def test_init_db_registers_user_profile_in_fresh_process(database_path: Path) -> None:
    # A fresh interpreter ensures test imports cannot register the model for init_db.
    result = subprocess.run(
        [
            sys.executable,
            "-c",
            "import sys; "
            "from sqlalchemy import inspect; "
            "from app.db.session import create_database_engine, init_db; "
            "engine = create_database_engine(sys.argv[1]); "
            "init_db(engine); "
            "assert inspect(engine).has_table('user_profiles'); "
            "engine.dispose()",
            f"sqlite:///{database_path.as_posix()}",
        ],
        capture_output=True,
        text=True,
        check=False,
        timeout=30,
    )

    assert result.returncode == 0, result.stderr


def test_profile_table_constraints(client: TestClient) -> None:
    inspector = inspect(client.app.state.engine)
    columns = {
        column["name"]: column
        for column in inspector.get_columns("user_profiles")
    }

    assert set(columns) == {
        "id", "email", "department", "programme", "year", "semester",
        "interests", "created_at",
    }
    for name, column in columns.items():
        assert column["nullable"] is (name == "semester")
    assert inspector.get_pk_constraint("user_profiles")["constrained_columns"] == ["id"]
    assert any(
        constraint["column_names"] == ["email"]
        for constraint in inspector.get_unique_constraints("user_profiles")
    )


def test_profile_persistence_and_read_schema(
    client: TestClient, valid_profile_payload: dict[str, object]
) -> None:
    payload = UserProfileCreate.model_validate(
        {**valid_profile_payload, "interests": [" Robotics ", "MUSIC", "robotics"]}
    )
    before = datetime.now(timezone.utc)
    with client.app.state.session_factory() as session:
        profile = UserProfile(**payload.model_dump())
        session.add(profile)
        session.commit()
        profile_id = profile.id

    with client.app.state.session_factory() as session:
        stored = session.scalar(select(UserProfile).where(UserProfile.id == profile_id))
        assert stored is not None
        assert stored.interests == ["robotics", "music"]
        result = UserProfileRead.model_validate(stored)

    assert result.id == profile_id
    assert result.model_dump(exclude={"id", "created_at"}) == payload.model_dump()
    assert result.created_at.tzinfo == timezone.utc
    assert before <= result.created_at <= datetime.now(timezone.utc)


def test_duplicate_profile_email_is_rejected(
    client: TestClient, valid_profile_payload: dict[str, object]
) -> None:
    with client.app.state.session_factory() as session:
        session.add(UserProfile(**valid_profile_payload))
        session.commit()
        session.add(UserProfile(**valid_profile_payload))
        with pytest.raises(IntegrityError):
            session.commit()
        session.rollback()


@pytest.mark.parametrize("field", ["email", "department", "programme", "year", "interests"])
def test_profile_database_rejects_null_required_fields(
    client: TestClient, valid_profile_payload: dict[str, object], field: str
) -> None:
    with client.app.state.session_factory() as session:
        session.add(UserProfile(**{**valid_profile_payload, field: None}))
        with pytest.raises(IntegrityError):
            session.commit()
        session.rollback()


def test_read_schema_normalizes_datetime(valid_profile_payload: dict[str, object]) -> None:
    result = UserProfileRead.model_validate(
        {
            **valid_profile_payload,
            "id": 1,
            "created_at": datetime(2026, 9, 8, 12, tzinfo=timezone(timedelta(hours=2))),
        }
    )

    assert result.created_at == datetime(2026, 9, 8, 10, tzinfo=timezone.utc)
    assert result.created_at.tzinfo == timezone.utc
