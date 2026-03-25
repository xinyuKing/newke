<template>
  <section class="page-stack">
    <div class="page-head card">
      <div>
        <div class="tag">Orders</div>
        <h1>Order timeline</h1>
        <p class="muted">订单列表直接来自 `order-service` 的用户游标分页接口。</p>
      </div>
      <RouterLink class="ghost" to="/mall">Continue shopping</RouterLink>
    </div>

    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>先使用商城 `USER` 角色登录，再查看历史订单和物流流转。</p>
      <RouterLink class="ghost" to="/login?redirect=/mall/orders">Login to view orders</RouterLink>
    </div>

    <div v-else-if="loading" class="empty card">Loading orders...</div>
    <template v-else-if="orders.length">
      <div class="order-list">
        <div v-if="checkoutNotice" class="card notice">
          {{ checkoutNotice }}
        </div>
        <article v-for="order in orders" :key="order.orderNo" class="card order-card">
          <div>
            <div class="tag alt">{{ order.status }}</div>
            <h3>{{ order.orderNo }}</h3>
            <p class="muted">Merchant {{ order.merchantId ?? "--" }} · {{ formatDateTime(order.createdAt) }}</p>
          </div>
          <div class="order-side">
            <strong>{{ formatCurrency(order.totalAmount) }}</strong>
            <RouterLink class="ghost" :to="`/mall/orders/${order.orderNo}`">Details</RouterLink>
          </div>
        </article>
      </div>
      <div v-if="hasMoreOrders" class="more-actions">
        <button type="button" class="ghost" :disabled="loadingMoreOrders" @click="loadMoreOrders">
          {{ loadingMoreOrders ? "Loading more..." : "Load more orders" }}
        </button>
      </div>
    </template>
    <div v-else class="empty card">
      <h3>No orders yet</h3>
      <p>现在还没有成交记录，去商品页下第一单会更有感觉。</p>
      <RouterLink class="ghost" to="/mall">Browse products</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { formatCurrency, formatDateTime } from "../utils/format";

const auth = useAuthStore();
const route = useRoute();
const ORDER_PAGE_SIZE = 20;
const orders = ref([]);
const loading = ref(false);
const loadingMoreOrders = ref(false);
const hasMoreOrders = ref(false);
const nextOrderCursorTime = ref(null);
const nextOrderCursorId = ref(null);
const orderRequestToken = ref(0);

const checkoutNotice = computed(() => {
  if (typeof route.query.created !== "string") return "";
  const orderNos = route.query.created
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  if (orderNos.length < 2) return "";
  return `Checkout created ${orderNos.length} merchant orders.`;
});

const mergeOrders = (current, incoming) => {
  const merged = [...current];
  const seen = new Set(current.map((item) => item.orderNo));
  incoming.forEach((item) => {
    if (!seen.has(item.orderNo)) {
      seen.add(item.orderNo);
      merged.push(item);
    }
  });
  return merged;
};

const resetOrderFeed = () => {
  orders.value = [];
  hasMoreOrders.value = false;
  nextOrderCursorTime.value = null;
  nextOrderCursorId.value = null;
};

const updateOrderCursor = (payload) => {
  hasMoreOrders.value = Boolean(payload?.hasNext);
  nextOrderCursorTime.value = payload?.nextCursorTime || null;
  nextOrderCursorId.value = payload?.nextCursorId ?? null;
};

const loadOrders = async ({ append = false } = {}) => {
  if (!auth.mallCanShop) return;
  if (append) {
    if (loading.value || loadingMoreOrders.value || !hasMoreOrders.value) return;
    loadingMoreOrders.value = true;
  } else {
    loading.value = true;
    resetOrderFeed();
  }
  const requestToken = ++orderRequestToken.value;
  try {
    const params = {
      size: ORDER_PAGE_SIZE
    };
    if (append) {
      params.cursorTime = nextOrderCursorTime.value;
      params.cursorId = nextOrderCursorId.value;
    }
    const { data } = await mallApi.get("/user/orders", { params });
    if (requestToken !== orderRequestToken.value) return;
    if (!data?.success) {
      if (!append) {
        resetOrderFeed();
      }
      return;
    }
    const nextItems = data.data?.items || [];
    orders.value = append ? mergeOrders(orders.value, nextItems) : nextItems;
    updateOrderCursor(data.data);
  } finally {
    if (append) {
      loadingMoreOrders.value = false;
      return;
    }
    if (requestToken === orderRequestToken.value) {
      loading.value = false;
    }
  }
};

const loadMoreOrders = async () => {
  await loadOrders({ append: true });
};

onMounted(loadOrders);
</script>

<style scoped>
.page-stack {
  display: grid;
  gap: 18px;
}

.page-head,
.empty,
.order-card,
.notice {
  padding: 22px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.page-head h1,
.empty h3,
.order-card h3 {
  margin: 10px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.order-list {
  display: grid;
  gap: 14px;
}

.more-actions {
  display: flex;
  justify-content: center;
}

.notice {
  color: var(--muted);
}

.order-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.order-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.order-side strong {
  font-family: "Space Grotesk", sans-serif;
  font-size: 20px;
}

.ghost {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--ink);
  text-decoration: none;
}

.ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

@media (max-width: 768px) {
  .page-head,
  .order-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-side {
    justify-items: start;
  }
}
</style>
