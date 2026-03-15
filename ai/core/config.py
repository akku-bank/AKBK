import os


def _require_env(key: str, default: str | None = None) -> str:
    # 필수 설정값을 읽고, 없으면 명확한 예외를 발생시킵니다.
    value = os.getenv(key, default)
    if value is None or not str(value).strip():
        raise ValueError(f"환경변수 '{key}'가 설정되지 않았습니다.")
    return str(value).strip()


class Settings:
    # 실행 환경마다 달라지는 값만 환경변수로 관리합니다.
    VECTOR_DB_HOST = os.getenv("VECTOR_DB_HOST", "localhost").strip()
    VECTOR_DB_PORT = int(os.getenv("VECTOR_DB_PORT", "5432"))
    VECTOR_DB_NAME = os.getenv("VECTOR_DB_NAME", "vector_db").strip()
    VECTOR_DB_USER = os.getenv("DB_USERNAME", "app_user").strip()
    VECTOR_DB_PASSWORD = os.getenv("DB_PASSWORD", "app_pw").strip()

    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", os.getenv("GMS_API_KEY", "")).strip()
    OPENAI_BASE_URL = os.getenv(
        "OPENAI_BASE_URL",
        "https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    ).strip()

    @classmethod
    def vector_db_dsn(cls) -> str:
        # psycopg2 연결에 사용할 DSN 문자열을 생성합니다.
        return (
            f"host={cls.VECTOR_DB_HOST} "
            f"port={cls.VECTOR_DB_PORT} "
            f"dbname={cls.VECTOR_DB_NAME} "
            f"user={cls.VECTOR_DB_USER} "
            f"password={cls.VECTOR_DB_PASSWORD}"
        )

    @classmethod
    def require_openai_api_key(cls) -> str:
        # LLM 구현 단계에서 사용할 API 키를 검증합니다.
        return _require_env("OPENAI_API_KEY", cls.OPENAI_API_KEY)
