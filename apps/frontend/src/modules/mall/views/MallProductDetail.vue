<template>
  <section class="detail-stack">
    <div v-if="loading" class="detail-shell card">Loading product...</div>
    <template v-else-if="product">
      <div class="detail-shell card fade-in">
        <div class="detail-main">
          <div class="eyebrow">
            <span class="tag">Mall Product</span>
            <span class="muted">SKU {{ product.id }}</span>
          </div>
          <h1>{{ product.name }}</h1>
          <p class="description">{{ product.description || "No detailed description has been published yet." }}</p>

          <div class="chips">
            <span class="chip">Merchant {{ product.merchantId ?? "--" }}</span>
            <span class="chip">{{ product.status || "ACTIVE" }}</span>
            <span class="chip">{{ summary?.reviewCount || 0 }} reviews</span>
          </div>

          <video v-if="product.videoUrl" class="video-player" controls :src="product.videoUrl"></video>

          <div class="summary card">
            <h3>Review Snapshot</h3>
            <p>{{ summary?.summary || "No review summary yet. The first buyer review will make this section less lonely." }}</p>
          </div>
        </div>

        <aside class="purchase card">
          <div class="price">{{ formatCurrency(product.price) }}</div>
          <p class="muted">同账号体系下登录后，可直接加购、直购和提交评价。</p>

          <label class="qty-field">
            <span>Quantity</span>
            <input v-model.number="quantity" type="number" min="1" />
          </label>

          <div class="purchase-actions">
            <button type="button" class="solid" @click="addToCart">Add to cart</button>
            <button type="button" class="ghost" @click="buyNow">Buy now</button>
          </div>

          <p v-if="message" class="muted">{{ message }}</p>
          <RouterLink class="jump-link" to="/mall/cart">Go to cart</RouterLink>
        </aside>
      </div>

      <div class="review-layout">
        <section class="card review-block">
          <div class="section-head">
            <div>
              <div class="tag alt">Community feedback</div>
              <h3>Latest Reviews</h3>
            </div>
            <span class="score">{{ formatStars(averageRating) }}</span>
          </div>

          <div v-if="reviews.length" class="review-list">
            <article v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-meta">
                <strong>{{ formatStars(review.rating) }}</strong>
                <span>user {{ review.userId }}</span>
                <span>{{ formatDateTime(review.createdAt) }}</span>
              </div>
              <p>{{ review.content }}</p>
            </article>
          </div>
          <div v-else class="empty-note">No reviews yet. The first comment still gets the whole stage.</div>
        </section>

        <section class="card review-form">
          <div class="section-head">
            <div>
              <div class="tag alt">Write</div>
              <h3>Leave a Review</h3>
            </div>
          </div>

          <template v-if="auth.mallCanShop">
            <label>
              <span>Rating</span>
              <select v-model.number="reviewForm.rating">
                <option :value="5">5</option>
                <option :value="4">4</option>
                <option :value="3">3</option>
                <option :value="2">2</option>
                <option :value="1">1</option>
              </select>
            </label>

            <label>
              <span>Content</span>
              <textarea v-model="reviewForm.content" rows="6" placeholder="讲讲使用感受、推荐点或者避坑点。"></textarea>
            </label>

            <button type="button" class="solid" @click="submitReview">Submit review</button>
          </template>
          <template v-else>
            <p class="muted">商城评价需要 JWT 登录态，先登录后就可以和论坛页面共存使用。</p>
            <RouterLink class="ghost action-link" :to="`/login?redirect=/mall/products/${route.params.id}`">Login to review</RouterLink>
          </template>
        </section>
      </div>
    </template>
    <div v-else class="detail-shell card">Product not found.</div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { createIdempotencyKey, formatCurrency, formatDateTime, formatStars } from "../utils/format";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const product = ref(null);
const summary = ref(null);
const reviews = ref([]);
const quantity = ref(1);
const loading = ref(false);
const message = ref("");
const reviewForm = reactive({
  rating: 5,
  content: ""
});

const averageRating = computed(() => {
  if (!reviews.value.length) return 0;
  const total = reviews.value.reduce((sum, item) => sum + (item.rating || 0), 0);
  return Math.round(total / reviews.value.length);
});

const ensureMallLogin = () => {
  if (auth.mallCanShop) return true;
  if (auth.mallLoggedIn) {
    message.value = "Current mall role does not have shopper permissions.";
    return false;
  }
  router.push({ path: "/login", query: { redirect: route.fullPath } });
  return false;
};

const loadProduct = async () => {
  loading.value = true;
  try {
    const id = route.params.id;
    const [productResp, summaryResp, reviewResp] = await Promise.all([
      mallApi.get(`/products/${id}`),
      mallApi.get(`/products/${id}/review-summary`),
      mallApi.get(`/products/${id}/reviews`, { params: { page: 1, size: 8 } })
    ]);
    product.value = productResp.data?.success ? productResp.data.data : null;
    summary.value = summaryResp.data?.success ? summaryResp.data.data : null;
    reviews.value = reviewResp.data?.success ? reviewResp.data.data?.items || [] : [];
  } finally {
    loading.value = false;
  }
};

const addToCart = async () => {
  message.value = "";
  if (!ensureMallLogin()) return;
  const { data } = await mallApi.post("/user/cart/items", {
    skuId: product.value.id,
    quantity: Math.max(1, Number(quantity.value) || 1)
  });
  message.value = data?.success ? "Added to cart." : data?.message || "Add to cart failed.";
};

const buyNow = async () => {
  message.value = "";
  if (!ensureMallLogin()) return;
  const { data } = await mallApi.post("/user/orders/purchase", {
    skuId: product.value.id,
    quantity: Math.max(1, Number(quantity.value) || 1),
    idempotencyKey: createIdempotencyKey("buy")
  });
  const orderNos = data?.data?.orderNos || [];
  const primaryOrderNo = data?.data?.orderNo || orderNos[0];
  if (data?.success && primaryOrderNo) {
    router.push(`/mall/orders/${primaryOrderNo}`);
    return;
  }
  message.value = data?.message || "Create order failed.";
};

const submitReview = async () => {
  message.value = "";
  if (!ensureMallLogin()) return;
  const content = reviewForm.content.trim();
  if (!content) {
    message.value = "Review content is required.";
    return;
  }
  const { data } = await mallApi.post("/user/reviews", {
    productId: product.value.id,
    rating: reviewForm.rating,
    content
  });
  if (data?.success) {
    reviewForm.content = "";
    reviewForm.rating = 5;
    message.value = "Review submitted.";
    await loadProduct();
    return;
  }
  message.value = data?.message || "Review submit failed.";
};

watch(
  () => route.params.id,
  async () => {
    quantity.value = 1;
    await loadProduct();
  }
);

onMounted(loadProduct);
</script>

<style scoped>
.detail-stack {
  display: grid;
  gap: 18px;
}

.detail-shell {
  display: grid;
  grid-template-columns: 1.35fr 0.75fr;
  gap: 18px;
  padding: 24px;
}

.detail-main {
  display: grid;
  gap: 16px;
}

.eyebrow,
.chips,
.section-head,
.review-meta,
.purchase-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.detail-main h1 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: clamp(28px, 4vw, 42px);
}

.description {
  margin: 0;
  line-height: 1.8;
  color: var(--muted);
}

.chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(42, 157, 143, 0.08);
  color: var(--ink);
  font-size: 13px;
}

.video-player {
  width: 100%;
  border-radius: 18px;
  background: #111;
}

.summary,
.purchase {
  padding: 18px;
}

.summary h3,
.review-block h3,
.review-form h3 {
  margin: 8px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.summary p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.purchase {
  display: grid;
  gap: 14px;
  align-content: start;
}

.price {
  font-family: "Space Grotesk", sans-serif;
  font-size: 36px;
}

.qty-field {
  display: grid;
  gap: 6px;
}

.qty-field span,
label span {
  font-size: 13px;
  color: var(--muted);
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
}

.solid,
.ghost,
.jump-link {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  padding: 10px 14px;
  text-decoration: none;
  cursor: pointer;
}

.solid {
  border: none;
  background: var(--ink);
  color: #fff;
}

.ghost,
.jump-link {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--ink);
}

.jump-link {
  width: fit-content;
}

.review-layout {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.review-block,
.review-form {
  padding: 22px;
  display: grid;
  gap: 16px;
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

.score {
  font-family: "Space Grotesk", sans-serif;
  font-size: 22px;
}

.review-list {
  display: grid;
  gap: 14px;
}

.review-item {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fffdfa;
}

.review-item p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.review-meta {
  color: var(--muted);
  font-size: 13px;
}

.empty-note {
  color: var(--muted);
  line-height: 1.7;
}

.action-link {
  width: fit-content;
}

@media (max-width: 920px) {
  .detail-shell,
  .review-layout {
    grid-template-columns: 1fr;
  }
}
</style>
