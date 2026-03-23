<template>
  <section class="stack">
    <div class="card panel">
      <h2>Messages</h2>
      <p class="muted">Unread: {{ unreadCount }}</p>
    </div>
    <div class="grid">
      <div v-for="item in conversations" :key="item.conversation.id" class="card convo">
        <div>
          <strong>{{ item.target?.username }}</strong>
          <p class="muted">{{ item.conversation.content }}</p>
        </div>
        <div class="meta">
          <span>Unread {{ item.unreadCount }}</span>
          <RouterLink :to="`/messages/${item.conversation.conversationId}`">Open</RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import api from "../api/http";

const conversations = ref([]);
const unreadCount = ref(0);

const load = async () => {
  const { data } = await api.get("/messages/conversations", { params: { page: 1, limit: 10 } });
  if (data.code === 0) {
    conversations.value = data.data.list || [];
    unreadCount.value = data.data.letterUnreadCount || 0;
  }
};

onMounted(load);
</script>

<style scoped>
.stack {
  display: grid;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel h2 {
  margin: 0 0 6px;
  font-family: "Space Grotesk", sans-serif;
}

.grid {
  display: grid;
  gap: 12px;
}

.convo {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.meta {
  text-align: right;
  display: grid;
  gap: 6px;
}

.muted {
  color: var(--muted);
}
</style>
