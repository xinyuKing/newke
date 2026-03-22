<template>
  <header class="header">
    <div class="container header-inner">
      <div class="brand" @click="goHome">
        <span class="brand-mark">C</span>
        <div>
          <div class="brand-title">Community</div>
          <div class="brand-sub">Stories, questions, and craft</div>
        </div>
      </div>

      <form class="search" @submit.prevent="submitSearch">
        <input v-model="keyword" type="search" placeholder="Search posts" />
        <button type="submit">Search</button>
      </form>

      <nav class="nav">
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/messages">Messages</RouterLink>
        <RouterLink to="/notices">Notices</RouterLink>
        <RouterLink v-if="auth.user" :to="`/profile/${auth.user.id}`">Profile</RouterLink>
        <RouterLink v-if="auth.user" to="/settings">Settings</RouterLink>
        <RouterLink v-if="!auth.user" to="/login">Login</RouterLink>
        <RouterLink v-if="!auth.user" to="/register">Register</RouterLink>
        <button v-if="auth.user" class="ghost" @click="logout">Logout</button>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const router = useRouter();
const keyword = ref("");

const submitSearch = () => {
  if (!keyword.value.trim()) return;
  router.push({ path: "/search", query: { keyword: keyword.value } });
};

const goHome = () => {
  router.push("/");
};

const logout = async () => {
  await auth.logout();
  router.push("/");
};
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  align-items: center;
  gap: 16px;
  padding: 18px 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 600;
  font-family: "Space Grotesk", sans-serif;
}

.brand-title {
  font-family: "Space Grotesk", sans-serif;
  font-weight: 600;
  font-size: 18px;
}

.brand-sub {
  font-size: 12px;
  color: var(--muted);
}

.search {
  display: flex;
  gap: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 6px 8px;
}

.search input {
  border: none;
  outline: none;
  flex: 1;
  padding: 6px 10px;
  background: transparent;
  font-size: 14px;
}

.search button {
  border: none;
  background: var(--accent);
  color: #fff;
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
}

.nav {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  font-family: "Space Grotesk", sans-serif;
  font-size: 14px;
}

.nav .ghost {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .header-inner {
    grid-template-columns: 1fr;
  }
  .nav {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
