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
              <h3>{{ item.product?.name || `Product ${item.skuId}` }}</h3>
              <p>{{ item.product?.description || "Product details are loading from the product service." }}</p>
              <strong>{{ formatCurrency(item.priceSnapshot) }}</strong>
            </div>

            <div class="item-actions">
              <div class="qty-box">
                <button type="button" @click="updateQuantity(item, item.quantity - 1)">-</button>
                <input :value="item.quantity" type="number" min="1" @change="syncQuantity(item, $event)" />
                <button type="button" @click="updateQuantity(item, item.quantity + 1)">+</button>
              </div>
              <button type="button" class="ghost" @click="removeItem(item.skuId)">Remove</button>
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
          <button type="button" class="solid" @click="checkout">Checkout</button>
          <p v-if="message" class="muted">{{ message }}</p>
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
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { createIdempotencyKey, formatCurrency } from "../utils/format";

const auth = useAuthStore();
const router = useRouter();

const items = ref([]);
const productMap = ref({});
const loading = ref(false);
const message = ref("");

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

const hydrateProducts = async (skuIds) => {
  const uniqueIds = [...new Set(skuIds.filter(Boolean))];
  const nextMap = { ...productMap.value };
  await Promise.all(
    uniqueIds.map(async (skuId) => {
      if (nextMap[skuId]) return;
      const { data } = await mallApi.get(`/products/${skuId}`);
      if (data?.success) {
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
    await hydrateProducts(items.value.map((item) => item.skuId));
  } finally {
    loading.value = false;
  }
};

const updateQuantity = async (item, quantity) => {
  message.value = "";
  const nextQuantity = Math.max(1, Number(quantity) || 1);
  const { data } = await mallApi.put(`/user/cart/items/${item.skuId}`, {
    quantity: nextQuantity
  });
  if (data?.success) {
    items.value = items.value.map((current) =>
      current.skuId === item.skuId ? { ...current, quantity: nextQuantity } : current
    );
    return;
  }
  message.value = data?.message || "Update quantity failed.";
};

const syncQuantity = (item, event) => {
  updateQuantity(item, event.target.value);
};

const removeItem = async (skuId) => {
  message.value = "";
  const { data } = await mallApi.delete(`/user/cart/items/${skuId}`);
  if (data?.success) {
    items.value = items.value.filter((item) => item.skuId !== skuId);
    return;
  }
  message.value = data?.message || "Remove item failed.";
};

const checkout = async () => {
  message.value = "";
  const { data } = await mallApi.post("/user/cart/checkout", {
    idempotencyKey: createIdempotencyKey("cart")
  });
  if (data?.success && data.data?.orderNo) {
    router.push(`/mall/orders/${data.data.orderNo}`);
    return;
  }
  message.value = data?.message || "Checkout failed.";
};

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
