# ai/services/dictionary_service.py
# DictionaryService는 금융 용어 정의 조회를 담당하는 서비스 계층입니다.
# 현재는 정적 JSON 파일에서 용어와 정의를 읽어서 처리하지만, 추후 DB 연동이나 외부 API 활용으로 확장할 수 있습니다.

import json
from pathlib import Path


class DictionaryService:
    # 금융 용어 정의는 정적 JSON 파일에서 읽어 옵니다.
    # 상위 계층에서 정의할 용어 목록을 넘겨준다고 가정하고, 이 서비스는 조회만 담당합니다.
    DICTIONARY_PATH = Path(__file__).resolve().parents[1] / "db" / "finance_dictionary.json"

    def __init__(self) -> None:
        # 서비스 초기화 시 사전 데이터를 한 번만 로드해서 재사용합니다.
        self.definitions = self._load_definitions()

    def generate_answer(self, terms: list[str]) -> dict[str, object]:
        # 상위 라우팅 단계에서 이미 정리된 용어 목록을 받아 DICT 응답을 생성합니다.
        definitions = self.get_definitions(terms)
        answer = self._format_answer(definitions)
        return {
            "route": "DICT",
            "terms": list(definitions.keys()),
            "answer": answer,
        }

    def get_definitions(self, terms: list[str]) -> dict[str, str]:
        # 중복이나 빈 값을 제거한 뒤 각 용어별 정의를 조회합니다.
        normalized_terms = self._normalize_terms(terms)
        return {
            term: self.lookup_definition(term)
            for term in normalized_terms
        }

    def lookup_definition(self, term: str) -> str:
        # 단일 용어 정의 조회가 필요할 때 사용할 수 있는 기본 조회 메서드입니다.
        normalized_term = term.strip().lower()
        definition = self.definitions.get(normalized_term)

        if definition is not None: # 정의가 없는 용어에 대해서 메시지를 반환합니다 > 추후 랭그래프 연동 시 수정 예정
            return definition
        return f"'{term}'에 대한 금융 용어 정의를 아직 준비하지 못했습니다."

    def _normalize_terms(self, terms: list[str]) -> list[str]:
        # 입력 용어를 공백 제거 및 중복 제거해서 조회 가능한 형태로 정리합니다.
        normalized_terms: list[str] = []
        seen_terms: set[str] = set()

        for term in terms:
            cleaned_term = term.strip()
            if not cleaned_term:
                continue

            normalized_key = cleaned_term.lower()
            if normalized_key in seen_terms:
                continue

            seen_terms.add(normalized_key)
            normalized_terms.append(cleaned_term)

        if not normalized_terms: # 모든 입력이 빈 값이거나 중복이었다면, 정의 조회가 불가능하므로 예외를 발생시킵니다. > 추후 랭그래프 연동 시 수정 예정
            raise ValueError("dictionary 조회용 terms는 최소 1개 이상 필요합니다.")

        return normalized_terms

    def _format_answer(self, definitions: dict[str, str]) -> str:
        # 여러 용어 정의를 한 응답 문자열로 합쳐 상위 계층에서 바로 사용할 수 있게 합니다.
        return "\n".join(
            f"{term}: {definition}"
            for term, definition in definitions.items()
        )

    def _load_definitions(self) -> dict[str, str]:
        # JSON 파일을 읽어서 term -> definition 사전 형태로 변환합니다.
        with self.DICTIONARY_PATH.open("r", encoding="utf-8") as file:
            payload = json.load(file)

        definitions: dict[str, str] = {}
        for item in payload.get("terms", []):
            term = str(item.get("term", "")).strip().lower()
            definition = str(item.get("definition", "")).strip()
            if term and definition:
                definitions[term] = definition

        if not definitions: # JSON 파일이 비어 있거나 유효한 데이터가 없으면, 사전 조회가 불가능하므로 예외를 발생시킵니다. > 추후 랭그래프 연동 시 수정 예정
            raise ValueError("금융 사전 데이터가 비어 있습니다.")

        return definitions
