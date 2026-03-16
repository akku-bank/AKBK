from models.schema import QuizState 

def validate_quiz_node(state: QuizState) -> QuizState:
    """생성된 퀴즈가 비즈니스 로직 및 DB 제약 조건에 맞는지 검증"""
    quiz = state.get("quiz_data")
    errors = []

    # 1. 데이터 존재 유무 확인
    if not quiz:
        errors.append("AI가 퀴즈 데이터를 생성하지 못했습니다.")
        return {**state, "is_valid": False, "error_history": state["error_history"] + errors}

    # 2. 보기(Options) 개수 검증 (서비스 기획상 4개 고정)
    if len(quiz.options) != 4:
        errors.append(f"보기가 {len(quiz.options)}개입니다. 반드시 4개여야 합니다.")

    # 3. 정답 번호 유효성 검증 (1~4 범위 확인)
    if not (1 <= quiz.correct_choice_no <= 4):
        errors.append(f"정답 번호({quiz.correct_choice_no})가 유효 범위를 벗어났습니다.")

    # 4. 난이도별 특화 검증 (상 난이도: 과거 사건 포함 여부)
    if state["difficulty"] == "hard":
        # 질문에 숫자가 있거나(연도 등), 해설에 특정 경제 사건 키워드가 있는지 확인
        has_historical_context = any(char.isdigit() for char in quiz.question) or \
                                 any(k in quiz.explanation for k in ["년", "위기", "사건", "사태", "경제"])
        
        if not has_historical_context:
            errors.append("상 난이도 필수 조건인 '과거 역사적 사실' 기반의 내용이 부족합니다.")

    # 5. 최종 결과 업데이트
    # 에러 리스트가 비어있어야 통과(True)입니다.
    is_valid = len(errors) == 0
    
    return {
        **state,
        "is_valid": is_valid,
        "error_history": state["error_history"] + errors
    }