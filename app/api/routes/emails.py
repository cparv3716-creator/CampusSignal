from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.api.dependencies import get_db_session
from app.repositories.email import EmailRepository
from app.schemas.email import EmailCreate, EmailRead
from app.services.email_sources import ManualEmailSource
from app.services.ingestion import IngestionService

router = APIRouter(prefix="/emails", tags=["emails"])


@router.post(
    "",
    response_model=EmailRead,
    status_code=status.HTTP_201_CREATED,
    summary="Ingest a manually supplied raw email",
)
def ingest_manual_email(
    payload: EmailCreate,
    response: Response,
    session: Session = Depends(get_db_session),
) -> EmailRead:
    source = ManualEmailSource(payload)
    service = IngestionService(EmailRepository(session))
    result = service.ingest(source)

    if not result.created:
        response.status_code = status.HTTP_200_OK

    return EmailRead.model_validate(result.email)
