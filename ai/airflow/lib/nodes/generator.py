import random
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from config import GMS_API_KEY, GMS_BASE_URL, OPENAI_MODEL
from models.schema import QuizOutput, QuizState

# GMS 서버 설정 적용
llm = ChatOpenAI(
    model=OPENAI_MODEL,
    openai_api_key=GMS_API_KEY,
    base_url=GMS_BASE_URL,
    temperature=0.7
)
structured_llm = llm.with_structured_output(QuizOutput)

def generate_quiz_node(state: QuizState) -> QuizState:
    difficulty = state["difficulty"]
    easy_topics = ["용돈 아껴 쓰기", "저금통에 돈 모으기", "편의점 심부름", "필요한 것과 갖고 싶은 것 구분하기", "용돈"]
    
    if difficulty == "hard":
        system_msg = (
            "너는 중학교 고학년에서 고등학생용 경제 전문가야. 반드시 'IMF 외환위기', '대공황', '서브프라임 모기지' 등 "
            "역사적 사실이나 과거 경제적 실화를 기반으로 문제를 구성해줘. 이때, 중고등학생의 한국 경제 교과서에 자주 나올법한 내용을 다루면 더 좋아. 단 선택지가 쉽게 주어지지 않았으면 좋겠어. 예를 들어 하나의 선택지만 긍정적이라거나, 하나의 선택지만 부정적이여서 답이 쉽게 유추되는 경우처럼 말이야."
        )
    elif difficulty == "medium":
        system_msg = (
            "너는 초등학교 고학년과 중학생을 위한 경제 선생님이야. 수요와 공급, 이자, 인플레이션 등 "
            "교과서에 나오는 개념을 실생활 예시(중고거래, 은행 저축 등)와 연결해서 문제를 내줘."
        )
    else:
        topic = random.choice(easy_topics)
        system_msg = (
            f"너는 미취학~초등 저학년의 경제 친구야. 이번 주제는 '{topic}'이야. "
            "아이들이 돈의 개념을 쉽고 재미있게 배울 수 있도록 이해하기 어려운 용어보다는 쉬운 단어만 사용해서 문제를 출제해줘."
        )

    feedback_msg = ""
    if state["error_history"]:
        last_error = state["error_history"][-1]
        feedback_msg = f"\n\n⚠️ 이전 생성 실패 사유: {last_error}\n위 사항을 반드시 수정해서 다시 만들어줘."

    user_msg = f"난이도 '{difficulty}'에 맞는 경제 퀴즈를 1개 생성해줘. 보기는 반드시 4개여야 해.{feedback_msg}"

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_msg),
        ("user", user_msg)
    ])

    response = (prompt | structured_llm).invoke({})
    return {**state, "quiz_data": response, "retry_count": state["retry_count"] + 1}