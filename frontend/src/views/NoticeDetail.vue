<template>
  <section class="stack">
    <div class="card panel">
      <h2>Notice: {{ topic }}</h2>
    </div>
    <div class="grid">
      <div v-for="item in notices" :key="item.notice.id" class="card notice">
        <div>
          <strong>{{ item.user?.username }}</strong>
          <p class="muted">Entity {{ item.entityType }} ? {{ item.entityId }}</p>
        </div>
        <RouterLink v-if="item.postId" :to="`/post/${item.postId}`">Open</RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";

const route = useRoute();
const topic = ref(route.params.topic);
const notices = ref([]);

const load = async () => {
  const { data } = await api.get(`/notices/${topic.value}`, { params: { page: 1, limit: 10 } });
  if (data.code === 0) {
    notices.value = data.data.list || [];
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
  margin: 0;
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
