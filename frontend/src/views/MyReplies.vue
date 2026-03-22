<template>
  <section class="stack">
    <div class="card panel">
      <h2>My Replies</h2>
    </div>
    <div class="grid">
      <div v-for="item in items" :key="item.comment.id" class="card reply-card">
        <p v-html="item.comment.content"></p>
        <RouterLink v-if="item.post" :to="`/post/${item.post.id}`">Go to post</RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";

const route = useRoute();
const items = ref([]);

const load = async () => {
  const { data } = await api.get(`/users/${route.params.id}/replies`, { params: { page: 1, limit: 10 } });
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
.grid { display: grid; gap: 12px; }
.reply-card { padding: 16px; }
</style>
