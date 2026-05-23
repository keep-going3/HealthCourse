import os
import shutil
import chromadb
from chromadb.config import Settings as ChromaSettings
from typing import Optional
from app.config import settings
from app.utils.logger import logger


class RagService:
    _instance: Optional["RagService"] = None

    def __init__(self):
        self.client = chromadb.PersistentClient(
            path=settings.chroma_path,
            settings=ChromaSettings(anonymized_telemetry=False)
        )
        self.collection = self._get_or_create_collection()

    @classmethod
    def get_instance(cls) -> "RagService":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    @classmethod
    def reset_instance(cls):
        cls._instance = None

    def _get_or_create_collection(self):
        try:
            return self.client.get_collection("fitness_knowledge")
        except (ValueError, chromadb.errors.NotFoundError):
            return self.client.create_collection("fitness_knowledge")

    def search(self, query: str, n_results: int = 5) -> list[dict]:
        results = self.collection.query(
            query_texts=[query],
            n_results=n_results,
        )

        documents = []
        if results["documents"]:
            for i, doc in enumerate(results["documents"][0]):
                score = results["distances"][0][i] if results["distances"] else 0
                similarity = 1 - score
                if similarity >= settings.rag_score_threshold:
                    documents.append({
                        "content": doc,
                        "score": similarity,
                        "source": results["metadatas"][0][i].get("source", "unknown")
                        if results["metadatas"] else "unknown",
                    })

        return documents

    def reload(self, new_path: str) -> int:
        """热更新知识库，原子切换"""
        if not os.path.exists(new_path):
            raise FileNotFoundError(f"新知识库目录不存在: {new_path}")

        temp_path = settings.chroma_path + "_tmp"
        if os.path.exists(temp_path):
            shutil.rmtree(temp_path)

        # 复制到临时目录
        shutil.copytree(new_path, temp_path)

        # 重新初始化
        old_client = self.client
        try:
            new_service = RagService.__new__(RagService)
            new_service.client = chromadb.PersistentClient(
                path=temp_path,
                settings=ChromaSettings(anonymized_telemetry=False)
            )
            new_service.collection = new_service._get_or_create_collection()
            count = new_service.collection.count()
            # 原子切换
            self.client = new_service.client
            self.collection = new_service.collection
            settings.chroma_path = temp_path
            return count
        except Exception as e:
            logger.error(f"热更新失败: {e}")
            self.client = old_client
            raise
