<template>
  <section class="stack">
    <div class="card search-card">
      <h2>Search</h2>
      <p class="muted">Results for "{{ keyword }}"</p>
    </div>
    <div class="grid">
      <PostCard v-for="item in posts" :key="item.post.id" :item="item" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";
import PostCard from "../components/PostCard.vue";

const route = useRoute();
const posts = ref([]);
const keyword = ref(route.query.keyword || "");

const load = async () => {
  if (!keyword.value) return;
  const { data } = await api.get("/search", { params: { keyword: keyword.value, page: 1, limit: 10 } });
  if (data.code === 0) {
    posts.value = data.data.list || [];
  }
};

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value || "";
    load();
  }
);

onMounted(load);
</script>

<style scoped>
.stack {
  display: grid;
  gap: 16px;
}

.search-card {
  padding: 20px;
}

.search-card h2 {
  margin: 0 0 6px;
  font-family: "Space Grotesk", sans-serif;
}

.grid {
  display: grid;
  gap: 16px;
}

.muted {
  color: var(--muted);
}
</style>
