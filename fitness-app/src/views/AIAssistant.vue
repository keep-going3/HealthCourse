<template>
  <div class="chat-page">
    <van-nav-bar title="AI 健身教练" left-text="训练" @click-left="$router.push('/workout')" />

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-for="msg in messages" :key="msg.id" class="message-row"
        :class="msg.role === 'user' ? 'user-row' : 'assistant-row'">
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble" :class="msg.role">
          <div v-if="msg.role === 'assistant' && msg.isStreaming" class="streaming-content">
            <span class="streaming-text" v-html="renderContent(msg.content)"></span>
            <span class="cursor">▊</span>
          </div>
          <div v-else v-html="renderContent(msg.content)"></div>
        </div>
      </div>
      <div v-if="!messages.length" class="empty-hint">
        <p>我是你的 AI 健身教练，可以：</p>
        <p>• 分析你的训练数据</p>
        <p>• 解答健身知识</p>
        <p>• 解析训练记录（如"今天深蹲80kg 4x8"）</p>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <van-field v-model="inputMsg" placeholder="输入消息..."
        :disabled="isStreaming" autocomplete="off"
        @keydown.enter.prevent="sendMessage">
        <template #button>
          <van-button type="primary" size="small" :loading="isStreaming"
            @click="sendMessage">
            发送
          </van-button>
        </template>
      </van-field>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { showToast } from 'vant';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();
const messages = ref([]);
const inputMsg = ref('');
const isStreaming = ref(false);
const messageListRef = ref(null);
const retryCount = ref(0);
const maxRetries = 3;

// 生成唯一 ID
let msgIdCounter = Date.now();
const genId = () => msgIdCounter++;

// 简单的换行处理
const renderContent = (text) => {
  if (!text) return '';
  return text.replace(/\n/g, '<br>');
};

const scrollToBottom = async () => {
  await nextTick();
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
};

const sendMessage = async () => {
  const text = inputMsg.value.trim();
  if (!text || isStreaming.value) return;

  // 添加用户消息
  messages.value.push({ role: 'user', content: text, id: genId() });
  inputMsg.value = '';
  scrollToBottom();

  // 准备 AI 消息占位
  const aiMsg = { role: 'assistant', content: '', id: genId(), isStreaming: true };
  messages.value.push(aiMsg);
  isStreaming.value = true;
  scrollToBottom();

  let success = false;
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    if (attempt > 0) {
      showToast('连接断开，正在重试...');
      // 等待 1 秒重试
      await new Promise((r) => setTimeout(r, 1000));
    }

    try {
      const response = await fetch('/api/agent/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${userStore.token}`,
        },
        body: JSON.stringify({ message: text }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop();

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith('data:')) continue;

          try {
            const data = JSON.parse(trimmed.slice(5));
            if (data.type === 'chunk') {
              aiMsg.content += data.content;
              // 触发响应式更新
              messages.value = [...messages.value];
              scrollToBottom();
            } else if (data.type === 'ping') {
              // 心跳，忽略
            } else if (data.type === 'error') {
              showToast(data.message || 'AI 服务异常');
              aiMsg.isStreaming = false;
              messages.value = [...messages.value];
              success = true;
              break;
            } else if (data.type === 'end') {
              aiMsg.isStreaming = false;
              messages.value = [...messages.value];
              scrollToBottom();
              success = true;
              break;
            }
          } catch {
            // 解析失败的行跳过
          }
        }
        if (success) break;
      }

      if (success) break;
    } catch (err) {
      console.warn(`SSE attempt ${attempt + 1} failed:`, err);
      // 重置 AI 消息，下一轮重试
      aiMsg.content = '';
      if (attempt === maxRetries - 1) {
        aiMsg.content = '连接失败，请稍后重试。';
        aiMsg.isStreaming = false;
        messages.value = [...messages.value];
        showToast('AI 服务暂时不可用');
      }
    }
  }

  isStreaming.value = false;
  scrollToBottom();
};

onMounted(() => {
  scrollToBottom();
});
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 46px); /* 减去顶部 nav 高度 */
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: #f7f8fa;
}
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.user-row { flex-direction: row-reverse; }
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: white;
  flex-shrink: 0;
}
.user-row .avatar { background: #1989fa; }
.assistant-row .avatar { background: #07c160; }
.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.bubble.user {
  background: #1989fa;
  color: white;
  border-bottom-right-radius: 4px;
}
.bubble.assistant {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
}
.streaming-content {
  display: inline;
}
.streaming-text {
  white-space: pre-wrap;
}
.cursor {
  display: inline-block;
  color: #07c160;
  animation: blink 0.8s step-end infinite;
  margin-left: 2px;
}
@keyframes blink {
  50% { opacity: 0; }
}
.empty-hint {
  text-align: center;
  color: #969799;
  margin-top: 80px;
  font-size: 14px;
  line-height: 2;
}
.input-area {
  padding: 8px 12px;
  background: white;
  border-top: 1px solid #f0f0f0;
}
</style>
