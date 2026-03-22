<template>
  <div>
    <AppHeader />
    <main class="container main">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { RouterView } from "vue-router";
import { useAuthStore } from "./stores/auth";
import api from "./api/http";
import AppHeader from "./components/AppHeader.vue";
import AppFooter from "./components/AppFooter.vue";

const auth = useAuthStore();

onMounted(() => {
  auth.fetchMe();
  api.post("/track");
});
</script>

<style scoped>
.main {
  padding: 32px 0 20px;
  min-height: 70vh;
}
</style>
