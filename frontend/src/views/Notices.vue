<template>
  <section class="stack">
    <div class="card panel">
      <h2>Notifications</h2>
      <p class="muted">Unread: {{ unreadCount }}</p>
    </div>
    <div class="grid">
      <div v-for="item in summaries" :key="item.topic" class="card notice">
        <div>
          <strong>{{ item.title }}</strong>
          <p class="muted">Total {{ item.count }} · Unread {{ item.unreadCount }}</p>
        </div>
        <RouterLink :to="`/notices/${item.topic}`">View</RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import api from "../api/http";

const summaries = ref([]);
const unreadCount = ref(0);

const load = async () => {
  const { data } = await api.get("/notices");
  if (data.code === 0) {
    const result = [];
    const map = {
      commentNotice: { title: "Comments", topic: "comment" },
      likeNotice: { title: "Likes", topic: "like" },
      followNotice: { title: "Follows", topic: "follow" }
    };
    Object.keys(map).forEach((key) => {
      const notice = data.data[key];
      if (notice) {
        result.push({
          title: map[key].title,
          topic: map[key].topic,
          count: notice.count,
          unreadCount: notice.unreadCount
        });
      }
    });
    summaries.value = result;
    unreadCount.value = data.data.noticeUnreadCount || 0;
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

.notice {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.muted {
  color: var(--muted);
}
</style>
