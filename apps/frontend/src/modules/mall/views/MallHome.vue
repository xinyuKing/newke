<template>
  <section class="mall-stack">
    <div class="mall-hero card fade-in">
      <div class="hero-copy">
        <div class="tag">Mall District</div>
        <h1>From community momentum to checkout, all in one project.</h1>
        <p>
          This mall frontend is no longer a separate site shell. It now lives inside the same Vue app,
          shares the same navigation, and reaches the backend through the unified gateway.
        </p>

        <form class="shop-search" @submit.prevent="applySearch">
          <input v-model="draftKeyword" type="search" placeholder="Search products, keywords, or review themes" />
          <button type="submit">Search</button>
        </form>

        <div class="hero-actions">
          <RouterLink class="solid" to="/mall">Browse all</RouterLink>
          <RouterLink class="ghost" :to="auth.mallCanShop ? '/mall/orders' : '/login?redirect=/mall/orders'">
            {{ auth.mallCanShop ? "My orders" : "Login for orders" }}
          </RouterLink>
        </div>

        <p v-if="message" class="muted">{{ message }}</p>
      </div>

      <div class="hero-panel">
        <div class="panel-head">
          <span class="tag alt">Trending</span>
          <strong>Quick picks</strong>
        </div>
        <button
          v-for="item in recommendations"
          :key="item.id"
          type="button"
          class="mini-card"
          @click="openProduct(item.id)"
        >
          <span>{{ item.name }}</span>
          <strong>{{ formatCurrency(item.price) }}</strong>
        </button>
      </div>
    </div>

    <div class="toolbar">
      <div>
        <strong>{{ keyword ? `Search: ${keyword}` : "Featured products" }}</strong>
        <p class="muted">
          {{ keyword ? "Showing product matches from the mall services." : "Public products can be browsed without login." }}
        </p>
      </div>
      <button class="ghost" type="button" @click="refresh">Refresh</button>
    </div>

    <section class="forum-bridge card">
      <div class="bridge-head">
        <div>
          <div class="tag alt">Forum Pulse</div>
          <h3>Hot discussions from the same integrated community</h3>
          <p class="muted">Mall visitors can jump straight into the forum threads that are driving attention.</p>
        </div>
        <RouterLink class="ghost inline-link" to="/">Back to Forum</RouterLink>
      </div>

      <div v-if="hotPosts.length" class="forum-list">
        <button
          v-for="item in hotPosts"
          :key="item.post.id"
          type="button"
          class="forum-item"
          @click="router.push(`/post/${item.post.id}`)"
        >
          <strong>{{ item.post.title }}</strong>
          <p>{{ truncate(item.post.content, 96) || "Open the thread to see the full discussion." }}</p>
          <span>{{ item.likeCount || 0 }} likes | {{ item.commentCount || 0 }} comments</span>
        </button>
      </div>
      <div v-else class="empty card">
        <h3>No hot discussions yet</h3>
        <p>Once the forum data is ready, this block will show the hottest linked conversations.</p>
      </div>
    </section>

    <div v-if="loading" class="empty card">Loading products...</div>
    <template v-else-if="products.length">
      <div class="product-grid">
        <MallProductCard v-for="item in products" :key="item.id" :item="item" @add="quickAdd" />
      </div>
      <div v-if="hasMoreProducts" class="more-actions">
        <button type="button" class="ghost" :disabled="loadingMoreProducts" @click="loadMoreProducts">
          {{ loadingMoreProducts ? "Loading more..." : "Load more products" }}
        </button>
      </div>
    </template>
    <div v-else class="empty card">
      <h3>No products found</h3>
      <p>Try another keyword, or clear the search and browse everything currently active.</p>
      <button type="button" class="ghost" @click="clearSearch">Clear search</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import api from "../../forum/api/http";
import mallApi from "../api/mall";
import MallProductCard from "../components/MallProductCard.vue";
import { useAuthStore } from "../../../stores/auth";
import { formatCurrency, truncate } from "../utils/format";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const PRODUCT_PAGE_SIZE = 12;

const products = ref([]);
const recommendations = ref([]);
const hotPosts = ref([]);
const loading = ref(false);
const loadingMoreProducts = ref(false);
const keyword = ref("");
const draftKeyword = ref("");
const message = ref("");
const hasMoreProducts = ref(false);
const nextProductCursorTime = ref(null);
const nextProductCursorId = ref(null);
const productRequestToken = ref(0);

const normalizeItems = (payload) => (Array.isArray(payload) ? payload : payload?.items || []);

const mergeProducts = (current, incoming) => {
  const merged = [...current];
  const seen = new Set(current.map((item) => item.id));
  incoming.forEach((item) => {
    if (!seen.has(item.id)) {
      seen.add(item.id);
      merged.push(item);
    }
  });
  return merged;
};

const resetProductFeed = () => {
  products.value = [];
  hasMoreProducts.value = false;
  nextProductCursorTime.value = null;
  nextProductCursorId.value = null;
};

const updateProductCursor = (payload) => {
  hasMoreProducts.value = Boolean(payload?.hasNext);
  nextProductCursorTime.value = payload?.nextCursorTime || null;
  nextProductCursorId.value = payload?.nextCursorId ?? null;
};

const loadRecommendations = async () => {
  const { data } = await mallApi.get("/products/recommend", { params: { size: 4 } });
  if (data?.success) {
    recommendations.value = data.data || [];
  }
};

const loadHotPosts = async () => {
  try {
    const { data } = await api.get("/posts", {
      params: { page: 1, limit: 4, orderMode: 1 }
    });
    hotPosts.value = data?.code === 0 ? data.data?.list || [] : [];
  } catch (error) {
    hotPosts.value = [];
  }
};

const loadProducts = async ({ append = false } = {}) => {
  if (append) {
    if (loading.value || loadingMoreProducts.value || !hasMoreProducts.value) return;
    loadingMoreProducts.value = true;
  } else {
    loading.value = true;
    resetProductFeed();
  }
  const requestToken = ++productRequestToken.value;
  try {
    const endpoint = keyword.value ? "/products/search" : "/products/cursor";
    const params = {
      size: PRODUCT_PAGE_SIZE
    };
    if (keyword.value) {
      params.q = keyword.value;
    }
    if (append) {
      params.cursorTime = nextProductCursorTime.value;
      params.cursorId = nextProductCursorId.value;
    }
    const { data } = await mallApi.get(endpoint, {
      params
    });
    if (requestToken !== productRequestToken.value) return;
    if (!data?.success) {
      if (!append) {
        resetProductFeed();
      }
      return;
    }
    const nextItems = normalizeItems(data.data);
    products.value = append ? mergeProducts(products.value, nextItems) : nextItems;
    updateProductCursor(data.data);
  } finally {
    if (append) {
      loadingMoreProducts.value = false;
      return;
    }
    if (requestToken === productRequestToken.value) {
      loading.value = false;
    }
  }
};

const loadMoreProducts = async () => {
  await loadProducts({ append: true });
};

const applySearch = () => {
  const next = draftKeyword.value.trim();
  router.push({
    path: "/mall",
    query: next ? { q: next } : {}
  });
};

const clearSearch = () => {
  draftKeyword.value = "";
  router.push("/mall");
};

const openProduct = (id) => {
  router.push(`/mall/products/${id}`);
};

const quickAdd = async (item) => {
  message.value = "";
  if (!auth.mallCanShop) {
    router.push({ path: "/login", query: { redirect: `/mall/products/${item.id}` } });
    return;
  }
  const { data } = await mallApi.post("/user/cart/items", {
    skuId: item.id,
    quantity: 1
  });
  message.value = data?.success ? `${item.name} added to cart.` : data?.message || "Add to cart failed.";
};

const refresh = async () => {
  await Promise.all([loadProducts(), loadRecommendations(), loadHotPosts()]);
};

watch(
  () => route.query.q,
  async (value) => {
    keyword.value = typeof value === "string" ? value.trim() : "";
    draftKeyword.value = keyword.value;
    await loadProducts();
  },
  { immediate: true }
);

onMounted(() => {
  loadRecommendations();
  loadHotPosts();
});
</script>

<style scoped>
.mall-stack {
  display: grid;
  gap: 20px;
}

.mall-hero {
  display: grid;
  grid-template-columns: 1.3fr 0.9fr;
  gap: 18px;
  padding: 26px;
  overflow: hidden;
  background:
    radial-gradient(circle at 0% 0%, rgba(232, 106, 93, 0.14), transparent 35%),
    radial-gradient(circle at 100% 0%, rgba(42, 157, 143, 0.18), transparent 30%),
    #fff;
}

.hero-copy h1 {
  margin: 12px 0 10px;
  font-family: "Space Grotesk", sans-serif;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.05;
}

.hero-copy p {
  margin: 0 0 16px;
  color: var(--muted);
  line-height: 1.7;
}

.shop-search {
  display: flex;
  gap: 10px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
}

.shop-search input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  padding: 8px 12px;
  font-size: 14px;
}

.shop-search button,
.hero-actions a,
.ghost {
  font-family: "Space Grotesk", sans-serif;
}

.shop-search button,
.hero-actions a {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions .solid {
  background: var(--ink);
  color: #fff;
}

.hero-actions .ghost,
.ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--ink);
}

.hero-panel {
  display: grid;
  gap: 10px;
  align-content: start;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

.mini-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  text-align: left;
}

.mini-card span {
  font-weight: 600;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.toolbar strong,
.bridge-head h3 {
  font-family: "Space Grotesk", sans-serif;
}

.toolbar strong {
  font-size: 20px;
}

.toolbar p {
  margin: 6px 0 0;
}

.forum-bridge {
  display: grid;
  gap: 16px;
  padding: 22px;
}

.bridge-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.bridge-head h3 {
  margin: 8px 0 0;
}

.forum-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.forum-item {
  display: grid;
  gap: 8px;
  text-align: left;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  padding: 16px;
  cursor: pointer;
}

.forum-item strong {
  font-family: "Space Grotesk", sans-serif;
}

.forum-item p,
.forum-item span {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.inline-link {
  text-decoration: none;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(270px, 1fr));
  gap: 18px;
}

.more-actions {
  display: flex;
  justify-content: center;
}

.more-actions .ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.empty {
  display: grid;
  gap: 10px;
  justify-items: start;
  padding: 28px;
}

.empty h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
}

.empty p {
  margin: 0;
  color: var(--muted);
}

@media (max-width: 900px) {
  .mall-hero {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .bridge-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
