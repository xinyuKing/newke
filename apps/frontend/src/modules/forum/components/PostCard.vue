<template>
  <article class="card post-card fade-in">
    <div class="post-meta">
      <span class="tag">{{ item.user?.username || 'Unknown' }}</span>
      <span class="time">{{ formatDate(item.post?.createTime) }}</span>
    </div>
    <h3 class="post-title" @click="goDetail">{{ item.post?.title }}</h3>
    <p class="post-content" v-html="preview"></p>
    <MediaGallery :items="item.post?.media" :max="3" compact />
    <div class="post-footer">
      <span>Likes {{ item.likeCount || 0 }}</span>
      <span>Replies {{ item.post?.commentCount || 0 }}</span>
      <button class="ghost" @click="goDetail">Read</button>
    </div>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import MediaGallery from "./MediaGallery.vue";

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const preview = computed(() => {
  const content = props.item.post?.content || "";
  const text = content.replace(/<[^>]+>/g, "");
  return text.length > 120 ? `${text.slice(0, 120)}...` : text;
});

const formatDate = (value) => {
  if (!value) return "";
  const date = new Date(value);
  return date.toLocaleString();
};

const goDetail = () => {
  router.push(`/post/${props.item.post?.id}`);
};
</script>

<style scoped>
.post-card {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
}

.post-title {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 20px;
  cursor: pointer;
}

.post-content {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--muted);
}

.post-footer .ghost {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
}
</style>
