# AI Module

이 디렉터리는 FastAPI 기반 AI 서버입니다. 현재 구조는 스프링 백엔드가 Kafka에 실을 채팅 요청 메시지를 HTTP로 재현해서 테스트하는 형태를 기준으로 정리되어 있습니다.

현재 채팅 흐름의 핵심은 다음과 같습니다.

1. `POST /v1/chat/message`로 요청 수신
2. `ChatService`에서 요청 스키마를 내부 상태로 변환
3. `PolicyService`로 크레딧 선검사
4. `IntentService`로 의도 분류
5. `RoutingService`로 `DICT / RAG / LLM / DENY` 결정
6. 각 서비스 실행
7. `GuardService`로 출력 검사 및 필요 시 재작성 루프
8. Kafka 응답 payload 형태의 응답 반환

## 디렉터리 구조

```text
ai/
├ api/v1/                    # FastAPI 라우터
│  ├ __init__.py
│  └ chat.py                 # POST /v1/chat/message
├ clients/                   # 외부 시스템 / 모델 호출 클라이언트
│  ├ langgraph_client.py     # workflow 실행 래퍼
│  ├ llm_client.py           # LLM 호출 / 임베딩 호출
│  └ vector_db_client.py     # pgvector 검색 호출
├ core/
│  └ config.py               # 환경변수 / 설정
├ db/                        # 정적 데이터 / seed
├ repositories/              # DB 접근 레이어
├ schemas/                   # 요청 / 응답 / 내부 상태 스키마
│  ├ guard.py
│  ├ intent.py
│  ├ policy.py
│  ├ request.py
│  ├ response.py
│  └ state.py
├ services/                  # 비즈니스 로직
│  ├ chat_service.py         # 채팅 전체 오케스트레이션
│  ├ dictionary_service.py   # 금융 사전 응답
│  ├ guard_service.py        # 출력 안전성 검사
│  ├ intent_service.py       # 의도 분류
│  ├ policy_service.py       # 정책 선검사
│  ├ rag_service.py          # RAG 내부 오케스트레이션
│  └ routing_service.py      # route 결정
├ workflows/                 # LangGraph workflow 확장 포인트
└ main.py                    # FastAPI 앱 진입점
```

## 파일 책임

### `api/v1/`
- HTTP 요청을 받고 서비스로 위임합니다.
- 현재 채팅 엔드포인트는 `POST /v1/chat/message`입니다.

### `clients/`
- 외부 호출만 담당합니다.
- `llm_client.py`
  - 텍스트 생성
  - 의도 분류 호출
  - 임베딩 생성
- `vector_db_client.py`
  - 벡터 검색 호출
- `langgraph_client.py`
  - 현재는 workflow 실행 래퍼입니다.
  - 실제 LangGraph graph를 붙이면 이 파일이 실행 진입점이 됩니다.

### `schemas/`
- 외부 계약과 내부 상태를 분리합니다.
- `request.py`
  - Kafka 요청 payload를 HTTP로 재현하는 입력 스키마
- `response.py`
  - Kafka 응답 payload 형태의 출력 스키마
- `state.py`
  - 채팅 오케스트레이션 내부 상태 객체
- `policy.py`, `intent.py`, `guard.py`
  - 정책 / 의도 / Guard 관련 타입

### `services/`
- 비즈니스 로직을 담당합니다.
- `chat_service.py`
  - 요청을 내부 상태로 변환
  - `birth_date -> age_group` 변환
  - 정책 검사
  - 의도 분류
  - route 선택
  - `DICT / RAG / LLM / DENY` 실행 제어
  - Guard 검사 및 재작성 루프
  - 차감 크레딧 계산
- `dictionary_service.py`
  - 금융 용어 정의 응답 생성
- `intent_service.py`
  - 메시지의 금융 관련성 / cheating 여부 / intent 분류
- `policy_service.py`
  - 잔여 크레딧 기준 선검사
- `routing_service.py`
  - `DEFINE / HINT / EXPLAIN / OTHER`를 route로 변환
- `rag_service.py`
  - RAG 내부 흐름 담당
  - query 생성
  - 임베딩 생성 요청
  - 벡터 검색 요청
  - context 선택
  - prompt 생성
  - 답변 생성 요청
- `guard_service.py`
  - 정답 직접 노출, 보기 번호 언급, 과도한 유도 표현을 검사

## 현재 요청 / 응답 스키마

현재는 스프링 백엔드가 Kafka에 넣을 메시지를 HTTP로 재현해서 테스트합니다.

### 요청

```json
{
  "event_type": "CHAT_REQUEST",
  "event_id": "uuid",
  "user_id": "uuid",
  "quiz_id": "uuid",
  "message": "금리 뜻이 뭐야?",
  "remaining_credits": 100,
  "difficulty": "medium",
  "birth_date": "2015-03-10"
}
```

설명:
- `event_type`
  - 요청 이벤트 타입
- `event_id`
  - 요청-응답 매칭용 식별자
- `remaining_credits`
  - 현재 남은 크레딧
- `difficulty`
  - `easy | medium | hard`
- `birth_date`
  - 내부에서 `age_group`으로 변환

### 응답

```json
{
  "event_type": "CHAT_RESPONSE",
  "event_id": "uuid",
  "user_id": "uuid",
  "quiz_id": "uuid",
  "message": "금리 뜻이 뭐야?",
  "ai_reply": "금리는 ...",
  "deducted_credits": 5
}
```

설명:
- `ai_reply`
  - AI 최종 응답
- `deducted_credits`
  - 이번 요청 처리로 차감해야 하는 크레딧
  - 실제 잔여 크레딧 반영은 스프링 백엔드가 수행

## 현재 차감 정책

- `DICT`: 5
- `LLM`: 10
- `RAG`: 20
- `DENY`: 0

## 현재 채팅 처리 흐름

```text
POST /v1/chat/message
        ↓
chat.py
        ↓
ChatService
        ↓
ChatContextState 생성
        ↓
PolicyService
        ↓
LangGraphClient.run_workflow(...)
        ↓
ChatService._run_workflow(...)
        ↓
IntentService
        ↓
RoutingService
        ↓
DICT / RAG / LLM / DENY 실행
        ↓
GuardService
        ↓
ChatResponse 반환
```

## Route 기준

- `DEFINE` → `DICT`
- `HINT` → `RAG`
- `EXPLAIN` → 기본 `LLM`
- `OTHER` → `LLM`
- 정책 차단 또는 semantic deny → `DENY`

## Guard 동작

`DICT`, `DENY`는 그대로 반환합니다.

`RAG`, `LLM`은 Guard 검사를 거칩니다.

검사 항목:
- 정답 직접 노출
- 보기 번호 직접 언급
- 과도하게 직접적인 유도 표현

Guard 실패 시:
1. 재작성 프롬프트로 최대 2회 재생성
2. 계속 실패하면 안전한 fallback 메시지 반환

## 로깅

현재 주요 처리 단계는 로그로 확인할 수 있습니다.

- `chat.request_received`
- `chat.policy_checked`
- `langgraph.start`
- `langgraph.intent_classified`
- `langgraph.route_selected`
- `langgraph.dictionary_lookup`
- `langgraph.route_executed`
- `langgraph.guard_checked`
- `langgraph.guard_rewrite_attempt`
- `langgraph.guard_rewrite_checked`
- `langgraph.guard_fallback`
- `chat.response_ready`

## 주의사항

- 현재 `langgraph_client.py`는 실제 LangGraph graph 실행기가 아니라 workflow 실행 래퍼입니다.
- 실제 Kafka consumer / producer는 아직 붙지 않았고, 현재는 HTTP로 Kafka 메시지를 재현하는 단계입니다.
- `credits_spent_total` 기반 세부 정책은 아직 고정값 수준이며, 실제 누적 사용량 연동은 후속 작업 대상입니다.
