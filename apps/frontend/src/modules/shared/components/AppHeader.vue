<template>
  <header class="header">
    <div class="container header-inner">
      <div class="brand" @click="goHome">
        <span class="brand-mark">N</span>
        <div>
          <div class="brand-title">Newke</div>
          <div class="brand-sub">Community and commerce, finally on speaking terms</div>
        </div>
      </div>

      <form class="search" @submit.prevent="submitSearch">
        <input v-model="keyword" type="search" :placeholder="searchPlaceholder" />
        <button type="submit">{{ isMallRoute ? "Find" : "Search" }}</button>
      </form>

      <div class="nav-wrap">
        <nav class="nav">
          <RouterLink to="/">Forum</RouterLink>
          <RouterLink to="/mall">Mall</RouterLink>
          <RouterLink to="/messages">Messages</RouterLink>
          <RouterLink v-if="auth.mallCanShop" to="/mall/cart">Cart</RouterLink>
          <RouterLink v-if="auth.mallCanShop" to="/mall/orders">Orders</RouterLink>
          <RouterLink v-if="auth.mallCanShop" to="/mall/account">Account</RouterLink>
          <RouterLink v-if="auth.mallCanShop" to="/mall/support">Support</RouterLink>
          <RouterLink v-if="auth.user" :to="`/profile/${auth.user.id}`">Profile</RouterLink>
          <RouterLink v-if="!loggedIn" to="/login">Login</RouterLink>
          <RouterLink v-if="!loggedIn" to="/register">Register</RouterLink>
          <button v-if="loggedIn" class="ghost" type="button" @click="logout">Logout</button>
        </nav>

        <div v-if="loggedIn" class="session">
          <span class="chip">{{ auth.displayName }}</span>
          <span v-if="auth.mallRole" class="chip role">{{ auth.mallRole }}</span>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../../stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const keyword = ref("");

const isMallRoute = computed(() => route.path.startsWith("/mall"));
const loggedIn = computed(() => Boolean(auth.user || auth.mallLoggedIn));
const searchPlaceholder = computed(() =>
  isMallRoute.value ? "Search products and order vibes" : "Search posts and discussions"
);

watch(
  () => route.fullPath,
  () => {
    keyword.value = isMallRoute.value
      ? String(route.query.q || "")
      : String(route.query.keyword || "");
  },
  { immediate: true }
);

const submitSearch = () => {
  const value = keyword.value.trim();
  if (!value) return;
  if (isMallRoute.value) {
    router.push({ path: "/mall", query: { q: value } });
    return;
  }
  router.push({ path: "/search", query: { keyword: value } });
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
  grid-template-columns: 1.2fr 1fr 1.2fr;
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

.brand-title,
.nav,
.search button,
.chip,
.ghost {
  font-family: "Space Grotesk", sans-serif;
}

.brand-title {
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

.nav-wrap {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.nav {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  font-size: 14px;
  flex-wrap: wrap;
}

.session {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.chip {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(26, 28, 31, 0.06);
  font-size: 12px;
}

.chip.role {
  background: rgba(42, 157, 143, 0.12);
  color: var(--accent-2);
}

.ghost {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

@media (max-width: 960px) {
  .header-inner {
    grid-template-columns: 1fr;
  }

  .nav-wrap {
    justify-items: start;
  }

  .nav {
    justify-content: flex-start;
  }
}
</style>
