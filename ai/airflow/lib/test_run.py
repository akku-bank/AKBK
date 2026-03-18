import sys
import os

lib_dir = os.path.dirname(os.path.abspath(__file__))
if lib_dir not in sys.path:
    sys.path.insert(0, lib_dir)

from graph import quiz_app
from models.database import engine, Base, SessionLocal, Quiz

if __name__ == "__main__":
    # 1. 임시 테이블 생성 (Flyway 미가동 시)
    Base.metadata.create_all(bind=engine)
    
    # 2. 초기 상태 설정
    initial_state = {
        "difficulty": "hard", # 상 난이도: 고교 수준/과거 경제 실화
        "quiz_data": None,
        "error_history": [],
        "retry_count": 0,
        "is_valid": False
    }
    
    print("🚀 아꾸뱅꾸 퀴즈 생성 워크플로우 가동...")
    result = quiz_app.invoke(initial_state)

    # 3. 검증 체인 결과 확인
    print("\n" + "="*50)
    print("🔍 [검증 체인 결과 보고서]")
    print(f"📊 최종 통과 여부: {'✅ 통과' if result['is_valid'] else '❌ 실패'}")
    print(f"🔄 총 재시도 횟수: {result['retry_count']}회")
    
    if result["error_history"]:
        print("\n⚠️ 발생했던 오류 내역:")
        for i, error in enumerate(result["error_history"], 1):
            print(f"  {i}. {error}")
    print("="*50)

    # 4. DB 저장 데이터 확인 (직접 조회)
    if result["is_valid"]:
        db = SessionLocal()
        # 가장 최근에 저장된 퀴즈 1건 조회
        latest_quiz = db.query(Quiz).order_by(Quiz.created_at.desc()).first()
        
        if latest_quiz:
            print("\n💾 [DB 실시간 저장 데이터]")
            print(f"🆔 ID: {latest_quiz.id}")
            print(f"📌 주제: {latest_quiz.topic}")
            print(f"🏢 난이도: {latest_quiz.difficulty}")
            print(f"🧩 문제 JSON: {latest_quiz.problem_json}")
            print(f"💡 정답: {latest_quiz.correct_choice_no}번")
            print(f"📝 해설: {latest_quiz.explanation[:50]}...")
        db.close()