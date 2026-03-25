<template>
  <section class="page-stack">
    <div class="page-head card">
      <div>
        <div class="tag">Cart</div>
        <h1>Your cart</h1>
        <p class="muted">购物车直接对接 `cart-service`，结算后会转成商城订单。</p>
      </div>
      <RouterLink class="ghost" to="/mall">Keep browsing</RouterLink>
    </div>

    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>论坛页面可以匿名浏览，但购物车和下单需要商城 `USER` 角色的 JWT 登录态。</p>
      <RouterLink class="ghost" to="/login?redirect=/mall/cart">Login to continue</RouterLink>
    </div>

    <template v-else>
      <div v-if="loading" class="empty card">Loading cart...</div>
      <div v-else-if="rows.length" class="cart-layout">
        <section class="card cart-list">
          <article v-for="item in rows" :key="item.skuId" class="cart-item">
            <div class="item-copy">
              <span class="tag alt">SKU {{ item.skuId }}</span>
              <h3>{{ resolveItemName(item) }}</h3>
              <p>{{ resolveItemDescription(item) }}</p>
              <p v-if="item.productAvailable === false" class="item-warning">
                This item is no longer available for checkout. Remove it to continue.
              </p>
              <strong>{{ formatCurrency(item.priceSnapshot) }}</strong>
            </div>

            <div class="item-actions">
              <div class="qty-box">
                <button
                  type="button"
                  :disabled="cartLocked || item.productAvailable === false"
                  @click="updateQuantity(item, item.quantity - 1)"
                >
                  -
                </button>
                <input
                  :value="item.quantity"
                  :disabled="cartLocked || item.productAvailable === false"
                  type="number"
                  min="1"
                  @change="syncQuantity(item, $event)"
                />
                <button
                  type="button"
                  :disabled="cartLocked || item.productAvailable === false"
                  @click="updateQuantity(item, item.quantity + 1)"
                >
                  +
                </button>
              </div>
              <button type="button" class="ghost" :disabled="cartLocked" @click="removeItem(item.skuId)">Remove</button>
            </div>
          </article>
        </section>

        <aside class="card summary">
          <h3>Summary</h3>
          <div class="sum-line">
            <span>Items</span>
            <strong>{{ totalQuantity }}</strong>
          </div>
          <div class="sum-line">
            <span>Total</span>
            <strong>{{ formatCurrency(totalAmount) }}</strong>
          </div>
          <button type="button" class="solid" :disabled="checkoutBlocked" @click="checkout">
            {{ checkoutButtonLabel }}
          </button>
          <p v-if="statusMessage" class="muted">{{ statusMessage }}</p>
        </aside>
      </div>
      <div v-else class="empty card">
        <h3>Your cart is empty</h3>
        <p>先去商品页逛一圈，看到合适的再把它加进来。</p>
        <RouterLink class="ghost" to="/mall">Browse products</RouterLink>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRouter } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { createIdempotencyKey, formatCurrency } from "../utils/format";
import {
  clearPendingIdempotencyKey,
  getPendingIdempotencyKey,
  resolveIdempotentRequestFailure,
  setPendingIdempotencyKey
} from "../utils/idempotency";

const auth = useAuthStore();
const router = useRouter();

const items = ref([]);
const productMap = ref({});
const loading = ref(false);
const message = ref("");
const checkoutSubmitting = ref(false);
const checkoutKey = ref("");
const PENDING_CHECKOUT_MESSAGE =
  "A checkout request is still pending. Retry checkout to reuse the same request. Cart edits stay locked until it resolves.";
const UNAVAILABLE_ITEMS_MESSAGE = "Remove unavailable items before checkout.";

const checkoutStorageMeta = computed(() => ({
  scope: "cart-checkout",
  owner: auth.mallToken?.slice(-12) || "shopper",
  subject: "active"
}));

const resetCheckoutKey = () => {
  checkoutKey.value = "";
  clearPendingIdempotencyKey(checkoutStorageMeta.value);
};

const ensureCheckoutKey = () => {
  if (checkoutKey.value) return checkoutKey.value;
  const storedKey = getPendingIdempotencyKey(checkoutStorageMeta.value);
  checkoutKey.value = storedKey || createIdempotencyKey("cart");
  setPendingIdempotencyKey(checkoutStorageMeta.value, checkoutKey.value);
  return checkoutKey.value;
};

const checkoutPending = computed(() => Boolean(checkoutKey.value));

const cartLocked = computed(() => checkoutSubmitting.value || checkoutPending.value);

const hasUnavailableItems = computed(() => rows.value.some((item) => item.productAvailable === false));

const checkoutBlocked = computed(() => checkoutSubmitting.value || (hasUnavailableItems.value && !checkoutPending.value));

const checkoutButtonLabel = computed(() => {
  if (checkoutSubmitting.value) return "Submitting...";
  if (hasUnavailableItems.value && !checkoutPending.value) return "Remove unavailable items";
  if (checkoutPending.value) return "Retry checkout";
  return "Checkout";
});

const statusMessage = computed(() => {
  if (message.value) return message.value;
  if (checkoutPending.value) return PENDING_CHECKOUT_MESSAGE;
  if (hasUnavailableItems.value) return UNAVAILABLE_ITEMS_MESSAGE;
  return "";
});

const rows = computed(() =>
  items.value.map((item) => ({
    ...item,
    product: productMap.value[item.skuId]
  }))
);

const totalQuantity = computed(() =>
  rows.value.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
);

const totalAmount = computed(() =>
  rows.value.reduce((sum, item) => sum + Number(item.priceSnapshot || 0) * Number(item.quantity || 0), 0)
);

const clearProductMap = () => {
  productMap.value = {};
};

const resolveItemName = (item) => item.productName || item.product?.name || `Product ${item.skuId}`;

const resolveItemDescription = (item) =>
  item.productDescription || item.product?.description || "Product snapshot unavailable in the cart.";

const hydrateProducts = async (cartItems) => {
  const uniqueIds = [
    ...new Set(
      (cartItems || [])
        .filter((item) => item?.skuId && (!item?.productName || !item?.productDescription))
        .map((item) => item.skuId)
    )
  ];
  const nextMap = { ...productMap.value };
  await Promise.allSettled(
    uniqueIds.map(async (skuId) => {
      if (nextMap[skuId]) return;
      const { data } = await mallApi.get(`/products/${skuId}`);
      if (data?.success && data.data) {
        nextMap[skuId] = data.data;
      }
    })
  );
  productMap.value = nextMap;
};

const loadCart = async () => {
  if (!auth.mallCanShop) return;
  loading.value = true;
  try {
    const { data } = await mallApi.get("/user/cart/items");
    items.value = data?.success ? data.data || [] : [];
    clearProductMap();
    if (!items.value.length) {
      resetCheckoutKey();
    }
    await hydrateProducts(items.value);
  } finally {
    loading.value = false;
  }
};

const updateQuantity = async (item, quantity) => {
  message.value = "";
  if (cartLocked.value) {
    message.value = PENDING_CHECKOUT_MESSAGE;
    return;
  }
  const nextQuantity = Math.max(1, Number(quantity) || 1);
  const { data } = await mallApi.put(`/user/cart/items/${item.skuId}`, {
    quantity: nextQuantity
  });
  if (data?.success) {
    items.value = items.value.map((current) =>
      current.skuId === item.skuId ? { ...current, quantity: nextQuantity } : current
    );
    resetCheckoutKey();
    return;
  }
  message.value = data?.message || "Update quantity failed.";
};

const syncQuantity = (item, event) => {
  updateQuantity(item, event.target.value);
};

const removeItem = async (skuId) => {
  message.value = "";
  if (cartLocked.value) {
    message.value = PENDING_CHECKOUT_MESSAGE;
    return;
  }
  const { data } = await mallApi.delete(`/user/cart/items/${skuId}`);
  if (data?.success) {
    items.value = items.value.filter((item) => item.skuId !== skuId);
    if (!items.value.length) {
      productMap.value = {};
    }
    resetCheckoutKey();
    return;
  }
  message.value = data?.message || "Remove item failed.";
};

const checkout = async () => {
  message.value = "";
  if (hasUnavailableItems.value && !checkoutPending.value) {
    message.value = UNAVAILABLE_ITEMS_MESSAGE;
    return;
  }
  if (checkoutSubmitting.value) return;
  checkoutSubmitting.value = true;
  const idempotencyKey = ensureCheckoutKey();
  try {
    const response = await mallApi.post("/user/cart/checkout", {
      idempotencyKey
    });
    const { data } = response;
    const orderNos = data?.data?.orderNos || [];
    const primaryOrderNo = data?.data?.orderNo || orderNos[0];
    if (data?.success && orderNos.length > 1) {
      resetCheckoutKey();
      router.push({ path: "/mall/orders", query: { created: orderNos.join(",") } });
      return;
    }
    if (data?.success && primaryOrderNo) {
      resetCheckoutKey();
      router.push(`/mall/orders/${primaryOrderNo}`);
      return;
    }
    const failure = resolveIdempotentRequestFailure(response, "Checkout failed.", PENDING_CHECKOUT_MESSAGE);
    if (!failure.ambiguous) {
      resetCheckoutKey();
    }
    message.value = failure.message;
  } catch (error) {
    message.value = PENDING_CHECKOUT_MESSAGE;
  } finally {
    checkoutSubmitting.value = false;
  }
};

watch(
  checkoutStorageMeta,
  (meta) => {
    if (!auth.mallCanShop) {
      resetCheckoutKey();
      return;
    }
    checkoutKey.value = getPendingIdempotencyKey(meta);
  },
  { immediate: true }
);

onMounted(loadCart);
</script>

<style scoped>
.page-stack {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  padding: 22px;
}

.page-head h1,
.summary h3,
.empty h3 {
  margin: 10px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.cart-layout {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 18px;
}

.cart-list,
.summary,
.empty {
  padding: 20px;
}

.cart-list {
  display: grid;
  gap: 14px;
}

.cart-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: #fffdfa;
}

.item-copy h3 {
  margin: 10px 0 8px;
  font-family: "Space Grotesk", sans-serif;
}

.item-copy p {
  margin: 0 0 10px;
  color: var(--muted);
  line-height: 1.7;
}

.item-warning {
  margin-top: -2px;
  color: #b54708;
  font-size: 0.92rem;
}

.item-actions {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.qty-box {
  display: inline-grid;
  grid-template-columns: 36px 72px 36px;
  border: 1px solid var(--border);
  border-radius: 999px;
  overflow: hidden;
}

.qty-box button,
.qty-box input,
.solid,
.ghost {
  font-family: "Space Grotesk", sans-serif;
}

.qty-box button {
  border: none;
  background: transparent;
  cursor: pointer;
}

.qty-box input {
  width: 72px;
  border: none;
  text-align: center;
  background: transparent;
}

.summary {
  display: grid;
  gap: 14px;
  align-content: start;
}

.sum-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.solid,
.ghost {
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

.ghost {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--ink);
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

.qty-box button:disabled,
.qty-box input:disabled,
.solid:disabled,
.ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 920px) {
  .page-head,
  .cart-layout,
  .cart-item {
    grid-template-columns: 1fr;
  }

  .item-actions {
    justify-items: start;
  }
}
</style>
