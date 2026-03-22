<template>
  <section class="stack">
    <div class="card panel">
      <h2>My Posts</h2>
    </div>
    <div class="grid">
      <PostCard v-for="item in items" :key="item.post.id" :item="{ post: item.post, user: null, likeCount: item.likeCount }" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";
import PostCard from "../components/PostCard.vue";

const route = useRoute();
const items = ref([]);

const load = async () => {
  const { data } = await api.get(`/users/${route.params.id}/posts`, { params: { page: 1, limit: 10 } });
  if (data.code === 0) {
    items.value = data.data.list || [];
  }
};

onMounted(load);
</script>

<style scoped>
.stack { display: grid; gap: 16px; }
.panel { padding: 20px; }
.panel h2 { margin: 0; font-family: "Space Grotesk", sans-serif; }
.grid { display: grid; gap: 16px; }
</style>
