<template>
  <section class="stack">
    <div class="hero card">
      <div>
        <div class="tag">Fresh Feed</div>
        <h1>Share ideas, not noise.</h1>
        <p>Explore what the community is discussing right now.</p>
      </div>
      <div class="controls">
        <button :class="orderMode === 0 ? 'active' : ''" @click="setOrder(0)">Latest</button>
        <button :class="orderMode === 1 ? 'active' : ''" @click="setOrder(1)">Hot</button>
        <button class="accent" @click="createPost">New Post</button>
      </div>
    </div>

    <div class="card composer">
      <h3>New Post</h3>
      <input v-model="draft.title" placeholder="Title" />
      <textarea v-model="draft.content" placeholder="Share something"></textarea>
      <div class="media-upload">
        <label class="upload-btn">
          <input type="file" multiple accept="image/*,video/*" @change="handlePostFiles" />
          Attach media
        </label>
        <span v-if="uploading" class="muted">Uploading...</span>
      </div>
      <div v-if="media.length" class="media-list">
        <div v-for="(m, index) in media" :key="m.url" class="media-chip">
          <span>{{ m.type === 'video' ? 'Video' : 'Image' }}</span>
          <a :href="m.url" target="_blank">Preview</a>
          <button @click="removeMedia(index)">Remove</button>
        </div>
      </div>
      <button class="accent" @click="submitPost">Publish</button>
      <p v-if="message" class="muted">{{ message }}</p>
    </div>

    <div class="grid">
      <PostCard v-for="item in posts" :key="item.post.id" :item="item" />
    </div>

    <div class="pager">
      <button :disabled="page === 1" @click="load(page - 1)">Prev</button>
      <span>Page {{ page }} / {{ total }}</span>
      <button :disabled="page >= total" @click="load(page + 1)">Next</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/http";
import PostCard from "../components/PostCard.vue";

const router = useRouter();
const posts = ref([]);
const page = ref(1);
const total = ref(1);
const orderMode = ref(0);
const draft = ref({ title: "", content: "" });
const message = ref("");
const media = ref([]);
const uploading = ref(false);

const load = async (p = 1) => {
  const { data } = await api.get("/posts", {
    params: { page: p, limit: 10, orderMode: orderMode.value }
  });
  if (data.code === 0) {
    posts.value = data.data.list || [];
    page.value = data.data.page.current;
    total.value = data.data.page.total || 1;
  }
};

const setOrder = (mode) => {
  orderMode.value = mode;
  load(1);
};

const createPost = () => {
  const el = document.querySelector(".composer");
  if (el) el.scrollIntoView({ behavior: "smooth" });
};

const submitPost = async () => {
  message.value = "";
  if (!draft.value.title) {
    message.value = "Title is required.";
    return;
  }
  if (!draft.value.content && media.value.length === 0) {
    message.value = "Content or media is required.";
    return;
  }
  const { data } = await api.post("/posts", { ...draft.value, media: media.value });
  if (data.code === 0) {
    message.value = "Posted.";
    draft.value = { title: "", content: "" };
    media.value = [];
    await load(1);
  } else {
    message.value = (data.data && data.data.reason) || data.msg || "Post failed.";
  }
};

const uploadFiles = async (files, usage) => {
  const uploaded = [];
  for (const file of files) {
    const form = new FormData();
    form.append("file", file);
    form.append("usage", usage);
    try {
      const { data } = await api.post("/media/upload", form, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      if (data.code === 0) {
        uploaded.push(data.data);
      } else {
        message.value = data.msg || "Upload failed.";
      }
    } catch (error) {
      message.value = "Upload failed.";
    }
  }
  return uploaded;
};

const handlePostFiles = async (event) => {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  uploading.value = true;
  const uploaded = await uploadFiles(files, "post");
  media.value = media.value.concat(uploaded);
  uploading.value = false;
  event.target.value = "";
};

const removeMedia = (index) => {
  media.value.splice(index, 1);
};

onMounted(() => load(1));
</script>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  gap: 20px;
}

.hero h1 {
  font-family: "Space Grotesk", sans-serif;
  margin: 12px 0 6px;
}

.hero p {
  color: var(--muted);
}

.controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.controls button {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

.controls .active {
  background: var(--ink);
  color: #fff;
}

.controls .accent {
  background: var(--accent);
  color: #fff;
  border-color: transparent;
}

.grid {
  display: grid;
  gap: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  padding: 12px;
}

.pager button {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
}

.media-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.upload-btn {
  position: relative;
  overflow: hidden;
  border: 1px dashed var(--border);
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
}

.upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.media-chip {
  display: flex;
  gap: 8px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  background: #fff;
}

.media-chip button {
  border: none;
  background: transparent;
  color: var(--accent);
  cursor: pointer;
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
