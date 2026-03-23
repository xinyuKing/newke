<template>
  <section class="stack">
    <div class="card panel">
      <h2>Conversation with {{ target?.username }}</h2>
    </div>
    <div class="card panel">
      <div class="messages">
        <div v-for="item in letters" :key="item.letter.id" class="message">
          <strong>{{ item.fromUser?.username }}</strong>
          <p>{{ item.letter.content }}</p>
          <span class="muted">{{ formatDate(item.letter.createTime) }}</span>
        </div>
      </div>
      <form class="send" @submit.prevent="send">
        <input v-model="content" placeholder="Type a message" />
        <button class="accent" type="submit">Send</button>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";

const route = useRoute();
const letters = ref([]);
const target = ref(null);
const content = ref("");

const load = async () => {
  const { data } = await api.get(`/messages/conversations/${route.params.conversationId}`, {
    params: { page: 1, limit: 20 }
  });
  if (data.code === 0) {
    letters.value = data.data.list || [];
    target.value = data.data.target;
  }
};

const send = async () => {
  if (!content.value.trim() || !target.value) return;
  const { data } = await api.post("/messages", {
    toName: target.value.username,
    content: content.value
  });
  if (data.code === 0) {
    content.value = "";
    await load();
  }
};

const formatDate = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString();
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
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
}

.messages {
  display: grid;
  gap: 12px;
  max-height: 420px;
  overflow: auto;
}

.message {
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}

.send {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.send input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 10px 12px;
}

.send .accent {
  border: none;
  border-radius: 999px;
  padding: 8px 16px;
  background: var(--accent);
  color: #fff;
}

.muted {
  color: var(--muted);
  font-size: 12px;
}
</style>
