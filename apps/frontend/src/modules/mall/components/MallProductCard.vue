<template>
  <article class="product-card card fade-in">
    <div class="visual" :class="{ feature: item.videoUrl }">
      <span class="status">{{ item.status || "ACTIVE" }}</span>
      <span v-if="item.videoUrl" class="story">Video story</span>
      <div class="halo"></div>
      <div class="serial">SKU {{ item.id }}</div>
    </div>

    <div class="body">
      <div class="meta">
        <span>Merchant {{ item.merchantId ?? "--" }}</span>
        <span>{{ item.videoUrl ? "Playable" : "Classic" }}</span>
      </div>
      <h3>{{ item.name }}</h3>
      <p>{{ truncate(item.description, 120) || "This listing is waiting for a richer description." }}</p>

      <div class="foot">
        <strong>{{ formatCurrency(item.price) }}</strong>
        <div class="actions">
          <RouterLink :to="`/mall/products/${item.id}`">Details</RouterLink>
          <button type="button" @click="$emit('add', item)">Add</button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { formatCurrency, truncate } from "../utils/format";

defineProps({
  item: {
    type: Object,
    required: true
  }
});

defineEmits(["add"]);
</script>

<style scoped>
.product-card {
  overflow: hidden;
  display: grid;
}

.visual {
  position: relative;
  min-height: 156px;
  padding: 16px;
  background:
    linear-gradient(140deg, rgba(232, 106, 93, 0.12), rgba(42, 157, 143, 0.18)),
    linear-gradient(180deg, #fffaf3, #eef7ff);
}

.visual.feature {
  background:
    linear-gradient(140deg, rgba(232, 106, 93, 0.16), rgba(42, 157, 143, 0.22)),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.9), transparent 30%),
    linear-gradient(180deg, #fff7ee, #e6f7f1);
}

.status,
.story,
.serial {
  position: relative;
  z-index: 1;
}

.status,
.story {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  font-family: "Space Grotesk", sans-serif;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.85);
}

.story {
  margin-left: 8px;
  color: var(--accent-2);
}

.serial {
  position: absolute;
  left: 16px;
  bottom: 16px;
  font-family: "Space Grotesk", sans-serif;
  font-size: 12px;
  color: rgba(26, 28, 31, 0.66);
}

.halo {
  position: absolute;
  right: -24px;
  top: -32px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0));
}

.body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--muted);
  font-size: 12px;
  font-family: "Space Grotesk", sans-serif;
}

h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 20px;
}

p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
}

.foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.foot strong {
  font-family: "Space Grotesk", sans-serif;
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.actions a,
.actions button {
  border-radius: 999px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
}

.actions button {
  background: var(--ink);
  color: #fff;
  border-color: transparent;
}

@media (max-width: 768px) {
  .foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
