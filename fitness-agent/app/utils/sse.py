import json
import asyncio
import uuid
from typing import AsyncGenerator


def make_message(msg_type: str, **kwargs) -> str:
    data = {"type": msg_type, **kwargs}
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"


async def event_stream(gen: AsyncGenerator) -> AsyncGenerator[str, None]:
    message_id = str(uuid.uuid4())
    yield make_message("start", messageId=message_id)

    ping_interval = 15
    last_ping = asyncio.get_event_loop().time()

    try:
        async for chunk in gen:
            # Raw SSE passthrough (e.g. save_workout events from agent)
            if isinstance(chunk, str) and chunk.startswith("__SSE_RAW__"):
                yield chunk[11:]
                continue

            now = asyncio.get_event_loop().time()
            if now - last_ping >= ping_interval:
                yield make_message("ping")
                last_ping = now
            yield make_message("chunk", content=chunk)
    except asyncio.CancelledError:
        yield make_message("error", message="请求已取消")
        return
    except Exception as e:
        yield make_message("error", message=str(e))
        return

    yield make_message("end", messageId=message_id)
