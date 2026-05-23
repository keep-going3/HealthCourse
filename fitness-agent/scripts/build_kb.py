#!/usr/bin/env python3
"""构建知识库：读取 data/literature/ 下的文档，分段后写入 ChromaDB"""

import os
import sys
import glob
from dotenv import load_dotenv

load_dotenv()

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.config import settings
from app.utils.logger import logger


def split_text(text: str, max_length: int = 500) -> list[str]:
    """简单的文本分段，按段落拆分后合并到 max_length"""
    paragraphs = text.strip().split("\n\n")
    chunks = []
    current = []
    current_len = 0

    for p in paragraphs:
        p = p.strip()
        if not p:
            continue
        if current_len + len(p) > max_length and current:
            chunks.append("\n\n".join(current))
            current = []
            current_len = 0
        current.append(p)
        current_len += len(p)

    if current:
        chunks.append("\n\n".join(current))

    return chunks


def main():
    literature_dir = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "data", "literature"
    )

    if not os.path.exists(literature_dir):
        logger.error(f"知识库目录不存在: {literature_dir}")
        logger.error("请先在 data/literature/ 下放置健身知识文档 (.txt 或 .md)")
        sys.exit(1)

    import chromadb
    from chromadb.config import Settings as ChromaSettings
    from openai import OpenAI

    client = OpenAI(
        api_key=settings.aliyun_api_key,
        base_url=settings.llm_base_url,
    )

    chroma_client = chromadb.PersistentClient(
        path=settings.chroma_path,
        settings=ChromaSettings(anonymized_telemetry=False)
    )

    try:
        collection = chroma_client.get_collection("fitness_knowledge")
        logger.info("发现已有知识库，将追加新文档")
    except (ValueError, chromadb.errors.NotFoundError):
        collection = chroma_client.create_collection("fitness_knowledge")
        logger.info("创建新知识库")

    files = glob.glob(os.path.join(literature_dir, "*.txt")) + \
            glob.glob(os.path.join(literature_dir, "*.md"))

    if not files:
        logger.warning(f"在 {literature_dir} 中未找到 .txt 或 .md 文件")
        return

    total_chunks = 0
    for filepath in files:
        filename = os.path.basename(filepath)
        logger.info(f"处理文件: {filename}")

        with open(filepath, "r", encoding="utf-8") as f:
            text = f.read()

        chunks = split_text(text)
        for i, chunk in enumerate(chunks):
            if not chunk.strip():
                continue

            doc_id = f"{filename}_{i}"
            # 检查是否已存在
            existing = collection.get(ids=[doc_id])
            if existing and existing["ids"]:
                continue

            collection.add(
                documents=[chunk],
                metadatas=[{"source": filename, "chunk": i}],
                ids=[doc_id],
            )
            total_chunks += 1

        logger.info(f"  → 拆分为 {len(chunks)} 段，新增 {total_chunks} 条")

    logger.info(f"知识库构建完成，共 {total_chunks} 条")


if __name__ == "__main__":
    main()
