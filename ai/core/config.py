from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # LLM / OpenAI / GMS
    openai_api_key: str
    openai_model: str = "gpt-4o-mini"
    openai_base_url: str = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

    # Vector DB
    vector_db_host: str = "localhost"
    vector_db_port: int = 5432
    vector_db_name: str = "vector_db"
    vector_db_user: str = "app_user"
    vector_db_password: str = "app_pw"

    def vector_db_dsn(self) -> str:
        return (
            f"host={self.vector_db_host} "
            f"port={self.vector_db_port} "
            f"dbname={self.vector_db_name} "
            f"user={self.vector_db_user} "
            f"password={self.vector_db_password}"
        )

    def require_openai_api_key(self) -> str:
        if not self.openai_api_key or not self.openai_api_key.strip():
            raise ValueError("환경변수 'OPENAI_API_KEY'가 설정되지 않았습니다.")
        return self.openai_api_key.strip()


settings = Settings()