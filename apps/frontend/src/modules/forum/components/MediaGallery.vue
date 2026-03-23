<template>
  <div v-if="itemsToRender.length" :class="['media-grid', compact ? 'compact' : '']">
    <div v-for="(item, index) in itemsToRender" :key="index" class="media-item">
      <img v-if="item.type === 'image'" :src="item.url" :alt="item.name || 'image'" loading="lazy" />
      <video v-else controls preload="metadata" :src="item.url"></video>
    </div>
    <div v-if="hiddenCount > 0" class="media-more">+{{ hiddenCount }}</div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  items: {
    type: [Array, String],
    default: () => []
  },
  max: {
    type: Number,
    default: 0
  },
  compact: {
    type: Boolean,
    default: false
  }
});

const normalizeType = (item) => {
  if (item.type === "video" || item.type === "image") {
    return item.type;
  }
  const url = item.url || "";
  return /\.(mp4|webm|ogg)(\?|#|$)/i.test(url) ? "video" : "image";
};

const normalizedItems = computed(() => {
  let list = props.items;
  if (typeof list === "string") {
    try {
      list = JSON.parse(list);
    } catch (e) {
      list = [];
    }
  }
  if (!Array.isArray(list)) return [];
  return list
    .filter((item) => item && item.url)
    .map((item) => ({
      url: item.url,
      name: item.name,
      type: normalizeType(item)
    }));
});

const itemsToRender = computed(() => {
  if (!props.max || props.max <= 0) return normalizedItems.value;
  return normalizedItems.value.slice(0, props.max);
});

const hiddenCount = computed(() => normalizedItems.value.length - itemsToRender.value.length);
</script>

<style scoped>
.media-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin-top: 12px;
}

.media-grid.compact {
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
}

.media-item {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #0f0f0f;
}

.media-item img,
.media-item video {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}

.media-grid.compact .media-item img,
.media-grid.compact .media-item video {
  height: 110px;
}

.media-more {
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #f1ede6;
  color: var(--muted);
  font-size: 14px;
  height: 140px;
}

.media-grid.compact .media-more {
  height: 110px;
}
</style>
