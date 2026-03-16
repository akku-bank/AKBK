from models.schema import QuizState

def repair_quiz_node(state: QuizState) -> QuizState:
    """
    검증 실패 사유를 로깅하고, 다음 생성 단계에서 참고할 수 있도록 상태를 정돈
    """
    # 1. 가장 최근의 에러 메시지 가져오기
    if state["error_history"]:
        last_error = state["error_history"][-1]
    else:
        last_error = "알 수 없는 검증 오류가 발생했습니다."

    # 2. 콘솔이나 로그에 기록 (디버깅용)
    print(f"\n[수정 단계] 현재 재시도 횟수: {state['retry_count']}")
    print(f"[오류 내용] {last_error}")
    
    return state