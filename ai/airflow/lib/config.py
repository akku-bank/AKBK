import os
from dotenv import load_dotenv

# 현재 파일 위치 기준으로 최상단 .env 로드
env_path = os.path.join(os.path.dirname(__file__), '../../../.env')
load_dotenv(env_path)

GMS_API_KEY = os.getenv("GMS_API_KEY")
OPENAI_MODEL = "gpt-4o-mini"
GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT")
DB_NAME = os.getenv("DB_NAME")
DB_USERNAME = os.getenv("DB_USERNAME")
DB_PASSWORD = os.getenv("DB_PASSWORD")

DATABASE_URL = f"postgresql://{DB_USERNAME}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"