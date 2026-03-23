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
    <div v-else-if="orders.length" class="order-list">
      <article v-for="order in orders" :key="order.orderNo" class="card order-card">
        <div>
          <div class="tag alt">{{ order.status }}</div>
          <h3>{{ order.orderNo }}</h3>
          <p class="muted">{{ formatDateTime(order.createdAt) }}</p>
        </div>
        <div class="order-side">
          <strong>{{ formatCurrency(order.totalAmount) }}</strong>
          <RouterLink class="ghost" :to="`/mall/orders/${order.orderNo}`">Details</RouterLink>
        </div>
      </article>
    </div>
    <div v-else class="empty card">
      <h3>No orders yet</h3>
      <p>现在还没有成交记录，去商品页下第一单会更有感觉。</p>
      <RouterLink class="ghost" to="/mall">Browse products</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { formatCurrency, formatDateTime } from "../utils/format";

const auth = useAuthStore();
const orders = ref([]);
const loading = ref(false);

const loadOrders = async () => {
  if (!auth.mallCanShop) return;
  loading.value = true;
  try {
    const { data } = await mallApi.get("/user/orders", { params: { size: 20 } });
    orders.value = data?.success ? data.data?.items || [] : [];
  } finally {
    loading.value = false;
  }
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
.order-card {
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
