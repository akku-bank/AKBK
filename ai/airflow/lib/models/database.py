from sqlalchemy import create_engine, Column, String, Integer, JSON, Text, TIMESTAMP, func
from sqlalchemy.orm import sessionmaker
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.ext.declarative import declarative_base
import uuid
from config import DATABASE_URL # 상대 경로 제거

Base = declarative_base()

class Quiz(Base):
    __tablename__ = 'quizzes'
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    topic = Column(String(50), nullable=False)
    difficulty = Column(String(10), nullable=False)
    problem_json = Column(JSON, nullable=False)
    correct_choice_no = Column(Integer, nullable=False)
    explanation = Column(Text, nullable=False)
    created_at = Column(TIMESTAMP, nullable=False, server_default=func.now())

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)