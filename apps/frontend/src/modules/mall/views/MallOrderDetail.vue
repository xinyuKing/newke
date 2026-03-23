<template>
  <section class="page-stack">
    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>订单详情、支付确认和收货操作都需要商城 `USER` 角色登录态。</p>
      <RouterLink class="ghost" :to="`/login?redirect=/mall/orders/${route.params.orderNo}`">Login to continue</RouterLink>
    </div>

    <template v-else>
      <div v-if="loading" class="empty card">Loading order...</div>
      <template v-else-if="detail">
        <div class="hero card fade-in">
          <div>
            <div class="tag">Order Detail</div>
            <h1>{{ detail.orderNo }}</h1>
            <p class="muted">{{ formatDateTime(detail.createdAt) }}</p>
          </div>
          <div class="hero-side">
            <strong>{{ formatCurrency(detail.totalAmount) }}</strong>
            <span class="tag alt">{{ detail.status }}</span>
          </div>
        </div>

        <div class="actions card">
          <button v-if="detail.status === 'CREATED'" type="button" class="solid" @click="payOrder">Pay order</button>
          <button v-if="detail.status === 'SHIPPED'" type="button" class="solid" @click="confirmReceipt">Confirm receipt</button>
          <RouterLink class="ghost" :to="`/mall/support?orderNo=${detail.orderNo}`">After-sale</RouterLink>
          <p v-if="message" class="muted">{{ message }}</p>
        </div>

        <div class="detail-layout">
          <section class="card items">
            <h3>Items</h3>
            <article v-for="item in detail.items || []" :key="item.skuId" class="item-row">
              <div>
                <strong>{{ productMap[item.skuId]?.name || `Product ${item.skuId}` }}</strong>
                <p class="muted">{{ productMap[item.skuId]?.description || "Description will appear when the product service returns data." }}</p>
              </div>
              <div class="item-side">
                <span>x{{ item.quantity }}</span>
                <strong>{{ formatCurrency(item.price) }}</strong>
              </div>
            </article>
          </section>

          <section class="card logistics">
            <h3>Tracking</h3>
            <div class="logi-head">
              <span>Carrier {{ detail.carrierCode || tracking?.carrierCode || "--" }}</span>
              <span>Tracking {{ detail.trackingNo || tracking?.trackingNo || "--" }}</span>
            </div>

            <div v-if="tracking?.events?.length" class="timeline">
              <article v-for="event in tracking.events" :key="`${event.time}-${event.status}`" class="timeline-item">
                <strong>{{ event.status }}</strong>
                <span>{{ event.location || "Location pending" }}</span>
                <span>{{ formatDateTime(event.time) }}</span>
              </article>
            </div>
            <div v-else class="empty-note">
              Tracking details will show here after shipment.
            </div>
          </section>
        </div>
      </template>
      <div v-else class="empty card">Order not found.</div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { formatCurrency, formatDateTime } from "../utils/format";

const auth = useAuthStore();
const route = useRoute();

const detail = ref(null);
const tracking = ref(null);
const loading = ref(false);
const message = ref("");
const productMap = reactive({});

const hydrateProducts = async (skuIds) => {
  const uniqueIds = [...new Set((skuIds || []).filter(Boolean))];
  await Promise.all(
    uniqueIds.map(async (skuId) => {
      if (productMap[skuId]) return;
      const { data } = await mallApi.get(`/products/${skuId}`);
      if (data?.success) {
        productMap[skuId] = data.data;
      }
    })
  );
};

const loadOrder = async () => {
  if (!auth.mallCanShop) return;
  loading.value = true;
  try {
    const { data } = await mallApi.get(`/user/orders/${route.params.orderNo}`);
    detail.value = data?.success ? data.data : null;
    if (detail.value?.items?.length) {
      await hydrateProducts(detail.value.items.map((item) => item.skuId));
    }
    if (detail.value?.trackingNo || detail.value?.status === "SHIPPED" || detail.value?.status === "COMPLETED") {
      const trackingResp = await mallApi.get(`/user/orders/${route.params.orderNo}/tracking`);
      tracking.value = trackingResp.data?.success ? trackingResp.data.data : null;
    } else {
      tracking.value = null;
    }
  } finally {
    loading.value = false;
  }
};

const payOrder = async () => {
  message.value = "";
  const { data } = await mallApi.post(`/user/orders/${route.params.orderNo}/pay`);
  if (data?.success) {
    message.value = "Order marked as paid.";
    await loadOrder();
    return;
  }
  message.value = data?.message || "Pay order failed.";
};

const confirmReceipt = async () => {
  message.value = "";
  const { data } = await mallApi.post(`/user/orders/${route.params.orderNo}/confirm`);
  if (data?.success) {
    message.value = "Receipt confirmed.";
    await loadOrder();
    return;
  }
  message.value = data?.message || "Confirm receipt failed.";
};

watch(
  () => route.params.orderNo,
  async () => {
    await loadOrder();
  }
);

onMounted(loadOrder);
</script>

<style scoped>
.page-stack {
  display: grid;
  gap: 18px;
}

.hero,
.actions,
.items,
.logistics,
.empty {
  padding: 22px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.hero h1,
.items h3,
.logistics h3,
.empty h3 {
  margin: 10px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.hero-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-side strong {
  font-family: "Space Grotesk", sans-serif;
  font-size: 24px;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.items,
.logistics {
  display: grid;
  gap: 14px;
}

.item-row,
.timeline-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
}

.item-row p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.item-side {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.logi-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--muted);
}

.timeline {
  display: grid;
  gap: 10px;
}

.timeline-item {
  flex-direction: column;
}

.solid,
.ghost {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  padding: 10px 14px;
  text-decoration: none;
}

.solid {
  border: none;
  background: var(--ink);
  color: #fff;
  cursor: pointer;
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

.empty-note {
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .hero,
  .detail-layout,
  .item-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side,
  .item-side {
    justify-items: start;
  }
}
</style>
