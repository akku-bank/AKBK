from langgraph.graph import StateGraph, START, END
from models.schema import QuizState # 상대 경로 제거
from nodes.generator import generate_quiz_node
from nodes.validator import validate_quiz_node
from nodes.repair import repair_quiz_node
from nodes.persist import persist_quiz_node

def route_next(state: QuizState):
    if state["is_valid"]: return "persist"
    return "repair" if state["retry_count"] < 3 else END

workflow = StateGraph(QuizState)
workflow.add_node("generate", generate_quiz_node)
workflow.add_node("validate", validate_quiz_node)
workflow.add_node("repair", repair_quiz_node)
workflow.add_node("persist", persist_quiz_node)

workflow.add_edge(START, "generate")
workflow.add_edge("generate", "validate")
workflow.add_conditional_edges("validate", route_next, {"persist": "persist", "repair": "repair", END: END})
workflow.add_edge("repair", "generate")
workflow.add_edge("persist", END)

quiz_app = workflow.compile()