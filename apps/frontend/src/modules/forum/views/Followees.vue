<template>
  <section class="stack">
    <div class="card panel">
      <h2>Followees</h2>
    </div>
    <div class="grid">
      <div v-for="item in users" :key="item.user.id" class="card user-card">
        <img :src="item.user.headerUrl" alt="avatar" />
        <div>
          <strong>{{ item.user.username }}</strong>
          <p class="muted">Followed at {{ formatDate(item.followTime) }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";

const route = useRoute();
const users = ref([]);

const load = async () => {
  const { data } = await api.get(`/users/${route.params.id}/followees`, { params: { page: 1, limit: 20 } });
  if (data.code === 0) {
    users.value = data.data.list || [];
  }
};

const formatDate = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString();
};

onMounted(load);
</script>

<style scoped>
.stack { display: grid; gap: 16px; }
.panel { padding: 20px; }
.panel h2 { margin: 0; font-family: "Space Grotesk", sans-serif; }
.grid { display: grid; gap: 12px; }
.user-card { padding: 16px; display: flex; gap: 12px; align-items: center; }
.user-card img { width: 42px; height: 42px; border-radius: 12px; object-fit: cover; }
.muted { color: var(--muted); font-size: 12px; }
</style>
