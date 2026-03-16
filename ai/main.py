import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from ai.api.v1 import router as v1_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

def create_app() -> FastAPI:
    app = FastAPI(
        title="ai-gateway",
        version="0.1.0",
        lifespan=lifespan,
    )
    app.include_router(v1_router, prefix="/v1")
    return app

app = create_app()


@app.get("/health")
async def health():
    return {"status": "ok"}
