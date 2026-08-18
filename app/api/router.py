from fastapi import APIRouter

from app.api.routes import emails, health

api_router = APIRouter()
api_router.include_router(health.router)
api_router.include_router(emails.router, prefix="/api/v1")
