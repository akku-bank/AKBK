# AI Module

이 폴더는 프로젝트의 AI 관련 소스 코드와 모델 파일을 관리하는 공간입니다.

## AI 서버 폴더 구조 및 역할
현재 AI 서버는 FastAPI 기반 AI Gateway 구조로 구성되어 있으며,

요청 수신 → 정책 검사 → 질문 분류 → RAG/LLM 실행 → 응답 반환 흐름을 기준으로 폴더가 분리되어 있습니다.

### 전체 구조
```
ai/
├ api/v1/                    # FastAPI API 레이어 (외부 요청 진입점)
│  ├ __init__.py
│  └ chat.py                     # 채팅 관련 API 엔드포인트
├ clients/                   # 외부 시스템/모델 호출 클라이언트
│  ├ langgraph_client.py         # LangGraph workflow 실행 클라이언트
│  ├ llm_client.py               # 공통 LLM 호출 (분류 / 임베딩 / 생성)
│  └ vector_db_client.py         # pgvector 기반 벡터 검색 클라이언트
├ core/
│  └ config.py               # 환경변수 및 공통 설정 관리
├ db/                            # DB 초기화 / seed / 스키마 관련 파일
├ repositories/                  # DB 접근 레이어 (데이터 저장/조회)
│  ├ chat_log_repository.py      # 채팅 로그 저장/조회
│  └ quiz_repository.py          # 퀴즈 데이터 저장/조회
├ schemas/                   # 요청/응답/내부 상태 데이터 모델
│  ├ intent.py                   # 질문 분류 결과 모델
│  ├ policy.py                   # 정책 판단 결과 모델
│  ├ request.py                  # API 요청 스키마
│  ├ response.py                 # API 응답 스키마
│  └ state.py                    # 내부 오케스트레이션 상태 모델
├ services/                  # 비즈니스 로직 및 AI 처리 흐름
│  ├ chat_service.py             # 전체 채팅 처리 오케스트레이션
│  ├ intent_service.py           # LLM 기반 질문 분류 처리
│  ├ policy_service.py           # 정책 검사 (ex. 크레딧 제한)
│  └ rag_service.py              # RAG 처리 (임베딩 → 검색 → 답변 생성)
├ workflows/                 # LangGraph workflow 정의 영역
│  └ __init__.py
└ main.py                    # FastAPI 서버 엔트리포인트
```

### 폴더별 역할
- `api/v1/`
    - 외부 HTTP 요청을 받는 **API 레이어**
    - FastAPI router를 통해 엔드포인트를 정의하며, 실제 비즈니스 로직은 service 계층에 위임합니다.
    - `chat.py`
        - 채팅 관련 API 엔드포인트 정의
        - ex. `POST /v1/chat/message`

- `clients/`
    - 외부 시스템, 모델, 저장소와 직접 통신하는 **클라이언트 레이어**
    - Service 계층이 어떤 기능을 수행할지 결정하면, 실제 호출은 clients 계층에서 담당합니다.
    - `langgraph_client.py`
        - LangGraph workflow 실행 클라이언트
        - 현재는 mock 응답 반환 용도로 사용하고 있습니다.
    - `llm_client.py`
        - 공통 LLM 호출 클라이언트
        - 질문 분류용 LLM 호출
        - RAG용 임베딩 생성
        - RAG용 텍스트 생성
    - `vector_db_client.py`
        - pgvector 기반 벡터 검색 클라이언트
        - 질의 임베딩을 받아 유사 문서를 검색하고 정규화된 결과 반환합니다.

- `core/`
    - 공통 설정과 환경변수를 관리하는 **설정 레이어**
    - `config.py`
        - OpenAI/GMS 관련 설정
        - Vector DB 접속 정보
        - 공통 설정값 로드
        - 환경변수 기반 settings 객체 제공

- `db/` 
    - DB 초기화, seed 데이터, 스키마 관련 파일을 두는 영역
    - 현재는 하위 초기화 파일들을 관리하는 용도로 사용됩니다.

- `repositories/`
    - DB 읽기/쓰기를 담당하는 **저장소 레이어**
    - Service 계층이 직접 SQL이나 DB 세부 구현을 알지 않도록 분리합니다.
    - `chat_log_repository.py`
        - 채팅 로그 저장/조회
    - `quiz_repository.py`
        - 퀴즈 데이터 저장/조회

- `schemas/`
    - 요청/응답/내부 상태/정책 결과 등 **데이터 구조를 정의하는 레이어**
    - `request.py`
        - API 요청 스키마 정의
    - `response.py`
        - API 응답 스키마 정의
    - `state.py`
        - 내부 오케스트레이션 상태 모델
        - 정책 판단 결과, 질문 분류 결과, route, hint level 등을 저장
    - `policy.py`
        - 정책 판단 결과 모델
        - PolicyDecision, PolicyReason, PolicyResult 정의
    - `intent.py`
        - 질문 분류 결과 모델
        - IntentType, ClassificationResult 정의

- `services/`
    - AI Gateway의 핵심 비즈니스 로직과 흐름 제어를 담당하는 **서비스 레이어**
    - `chat_service.py`
        - 전체 채팅 처리 흐름 오케스트레이션
        - credit pre-check
        - 질문 분류 결과 반영
        - 정책 차단 여부 판단
        - LangGraph/RAG/LLM 실행 연결
    - `policy_service.py`
        - Hard Gate 정책 검사
        - 현재는 크레딧 소진 여부 검사 담당
    - `intent_service.py`
        - 질문 분류 orchestration
        - `llm_client.py` 를 사용해
            - 금융 관련 여부
            - 정답 요구 여부
            - intent(DEFINE / HINT / EXPLAIN / OTHER) 를 분류하고 내부 모델로 변환
    - `rag_service.py`
        - RAG orchestration
        - 질의 정제
        - 임베딩 생성 요청
        - 벡터 검색
        - 상위 컨텍스트 선택
        - 프롬프트 생성
        - 최종 답변 생성
        - 결과 포맷팅

- `workflows/`
    - LangGraph workflow 정의를 두는 영역
    - 현재는 초기화 단계이며, 이후 채팅 응답 그래프 및 퀴즈 생성 그래프를 이 계층에 배치할 예정입니다.

- `main.py`
    - FastAPI 애플리케이션의 **엔트리포인트**
    - 앱 생성 및 router 등록을 담당합니다.

## 현재 처리 흐름
현재 채팅 요청의 기본 처리 흐름은 아래와 같습니다.
```
POST /v1/chat/message
        ↓
chat.py (API)
        ↓
ChatService
        ↓
PolicyService (credit pre-check)
        ↓
IntentService (LLM 기반 질문 분류)
        ↓
정책 차단 여부 판단
        ↓
LangGraphClient / RAG / 이후 route 실행
        ↓
응답 반환
```

### 설계 원칙
현재 구조는 아래 원칙을 기준으로 분리했다.
1. API는 얇게 유지
    - 엔드포인트는 service를 호출만 하고, 비즈니스 로직은 넣지 않는다.
2. 도메인 흐름은 service에서 관리
    - 정책 판단, 질문 분류, RAG 실행 흐름은 service 계층에서 담당한다.
3. 외부 호출은 client로 분리
    - LLM, LangGraph, Vector DB 호출은 client 계층에서 담당한다.
4. DB 접근은 repository로 분리
    - 저장/조회 로직이 service에 섞이지 않도록 한다.
5. 상태와 결과는 schema로 명시
    - 내부 상태와 정책/분류 결과를 명확한 모델로 관리한다.

### 향후 확장 예정
현재 구조는 이후 다음 기능을 확장하기 쉽도록 설계되었다.
- Route Selection (DICT / RAG / LLM / DENY)
- Credit 차감 로직
- Output Guard / Rewrite loop
- Dictionary service
- LangGraph workflow 본격 연결
- RAG 고도화
- DB 로그 저장 확장