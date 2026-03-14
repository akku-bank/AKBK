from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    openai_api_key: str = Field(validation_alias="OPENAI_API_KEY")
    openai_model: str = Field(default="gpt-4o-mini", validation_alias="OPENAI_MODEL")
    openai_base_url: str = Field(
        default="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
        validation_alias="OPENAI_BASE_URL",
    )

    vector_db_host: str = Field(default="localhost", validation_alias="VECTOR_DB_HOST")
    vector_db_port: int = Field(default=5432, validation_alias="VECTOR_DB_PORT")
    vector_db_name: str = Field(default="vector_db", validation_alias="VECTOR_DB_NAME")
    vector_db_user: str = Field(default="app_user", validation_alias="DB_USERNAME")
    vector_db_password: str = Field(default="app_pw", validation_alias="DB_PASSWORD")

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