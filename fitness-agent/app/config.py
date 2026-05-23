import os
import sys
from dotenv import load_dotenv

load_dotenv()


class Settings:
    aliyun_api_key: str = os.getenv("aliyun-key") or os.getenv("ALIYUN_API_KEY") or ""
    llm_model: str = os.getenv("LLM_MODEL", "qwen-max")
    llm_base_url: str = os.getenv("LLM_BASE_URL",
                                  "https://dashscope.aliyuncs.com/compatible-mode/v1")
    llm_timeout: int = int(os.getenv("LLM_TIMEOUT", "60"))
    chroma_path: str = os.getenv("CHROMA_PATH", "./chroma_db")
    rag_score_threshold: float = float(os.getenv("RAG_SCORE_THRESHOLD", "0.7"))
    max_actions_count: int = int(os.getenv("MAX_ACTIONS_COUNT", "20"))
    kb_reload_token: str = os.getenv("KB_RELOAD_TOKEN", "")

    def validate(self):
        errors = []
        if not self.aliyun_api_key:
            errors.append("ALIYUN_API_KEY 未设置")
        if not os.path.exists(self.chroma_path):
            errors.append(f"知识库目录不存在 ({self.chroma_path})，请先执行 scripts/build_kb.py")
        if errors:
            for e in errors:
                print(f"[ERROR] {e}", file=sys.stderr)
            sys.exit(1)


settings = Settings()
