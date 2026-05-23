"""Agent 工具函数"""
from app.services.rag_service import RagService


def search_knowledge(query: str, n_results: int = 5) -> list[dict]:
    rag = RagService.get_instance()
    return rag.search(query, n_results)
