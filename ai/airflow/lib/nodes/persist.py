from models.database import SessionLocal, Quiz
from models.schema import QuizState

def persist_quiz_node(state: QuizState) -> QuizState:
    quiz_data = state["quiz_data"]
    db = SessionLocal()
    try:
        # PostgreSQL 스키마에 맞춰 데이터 적재
        new_quiz = Quiz(
            topic=quiz_data.topic,
            difficulty=state["difficulty"],
            problem_json={"question": quiz_data.question, "options": quiz_data.options},
            correct_answer=quiz_data.correct_choice_no,
            explanation=quiz_data.explanation
        )
        db.add(new_quiz)
        db.commit()
        print(f"✅ DB 저장 완료 (난이도: {state['difficulty']})")
    except Exception as e:
        db.rollback()
        print(f"❌ DB 저장 실패: {e}")
        raise e
    finally:
        db.close()
    return state