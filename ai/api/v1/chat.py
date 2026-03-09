from fastapi import APIRouter
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.services.chat_service import ChatService

router = APIRouter(
    prefix="/chat",
    tags=["chat"]
)

chat_service = ChatService()

@router.post("/message", response_model=ChatResponse)
async def send_message(request: ChatRequest) -> ChatResponse:
    return await chat_service.handle_message(request)