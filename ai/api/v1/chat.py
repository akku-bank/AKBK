from fastapi import APIRouter

router = APIRouter(
    prefix="/chat",
    tags=["chat"]
)

@router.post("/message")
async def send_message():
    return {"message": "ok"}