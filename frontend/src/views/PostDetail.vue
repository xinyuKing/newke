<template>
  <section v-if="post" class="stack">
    <div class="card post-card">
      <div class="post-header">
        <div>
          <div class="tag">{{ user?.username }}</div>
          <h2>{{ post.title }}</h2>
          <div class="meta">{{ formatDate(post.createTime) }}</div>
        </div>
        <div class="actions">
          <button @click="toggleLike">{{ likeStatus === 1 ? 'Liked' : 'Like' }}</button>
          <span>{{ likeCount }} likes</span>
        </div>
      </div>
    <div class="post-body" v-html="post.content"></div>
    <MediaGallery :items="post.media" />
    </div>

    <div class="card reply-card">
      <h3>Reply</h3>
      <textarea v-model="commentText" placeholder="Write your comment"></textarea>
      <div class="media-upload">
        <label class="upload-btn">
          <input type="file" multiple accept="image/*,video/*" @change="handleCommentFiles" />
          Attach media
        </label>
        <span v-if="commentUploading" class="muted">Uploading...</span>
      </div>
      <div v-if="commentMedia.length" class="media-list">
        <div v-for="(m, index) in commentMedia" :key="m.url" class="media-chip">
          <span>{{ m.type === 'video' ? 'Video' : 'Image' }}</span>
          <a :href="m.url" target="_blank">Preview</a>
          <button @click="removeCommentMedia(index)">Remove</button>
        </div>
      </div>
      <p v-if="commentMessage" class="muted">{{ commentMessage }}</p>
      <button class="accent" @click="submitComment">Post Comment</button>
    </div>

    <div class="comments">
      <h3>Comments ({{ rows }})</h3>
      <div v-for="c in comments" :key="c.comment.id" class="card comment-card">
        <div class="comment-header">
          <strong>{{ c.user?.username }}</strong>
          <span>{{ formatDate(c.comment.createTime) }}</span>
        </div>
        <p v-html="c.comment.content"></p>
        <MediaGallery :items="c.comment.media" compact />
        <div class="comment-actions">
          <button @click="likeComment(c.comment.id, c.comment.userId)">
            {{ c.likeStatus === 1 ? 'Liked' : 'Like' }} ({{ c.likeCount }})
          </button>
        </div>
        <div class="replies" v-if="c.replys && c.replys.length">
          <div v-for="r in c.replys" :key="r.reply.id" class="reply">
            <span class="reply-user">{{ r.user?.username }}</span>
            <span v-if="r.target"> → {{ r.target?.username }}</span>
            <p v-html="r.reply.content"></p>
            <MediaGallery :items="r.reply.media" compact />
          </div>
        </div>
        <div class="reply-form">
          <input v-model="replyText[c.comment.id]" placeholder="Reply..." />
          <button @click="submitReply(c.comment.id, c.comment.userId)">Send</button>
        </div>
        <div class="reply-media">
          <label class="upload-btn small">
            <input type="file" multiple accept="image/*,video/*" @change="(e) => handleReplyFiles(c.comment.id, e)" />
            Attach media
          </label>
          <span v-if="replyUploading[c.comment.id]" class="muted">Uploading...</span>
          <div v-if="replyMedia[c.comment.id] && replyMedia[c.comment.id].length" class="media-list">
            <div v-for="(m, idx) in replyMedia[c.comment.id]" :key="m.url" class="media-chip">
              <span>{{ m.type === 'video' ? 'Video' : 'Image' }}</span>
              <a :href="m.url" target="_blank">Preview</a>
              <button @click="removeReplyMedia(c.comment.id, idx)">Remove</button>
            </div>
          </div>
          <p v-if="replyMessage[c.comment.id]" class="muted">{{ replyMessage[c.comment.id] }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";
import MediaGallery from "../components/MediaGallery.vue";

const route = useRoute();
const post = ref(null);
const user = ref(null);
const likeCount = ref(0);
const likeStatus = ref(0);
const comments = ref([]);
const rows = ref(0);
const commentText = ref("");
const replyText = reactive({});
const commentMessage = ref("");
const replyMessage = reactive({});
const commentMedia = ref([]);
const commentUploading = ref(false);
const replyMedia = reactive({});
const replyUploading = reactive({});

const load = async () => {
  const { data } = await api.get(`/posts/${route.params.id}`, { params: { page: 1, limit: 10 } });
  if (data.code === 0) {
    post.value = data.data.post;
    user.value = data.data.user;
    likeCount.value = data.data.likeCount;
    likeStatus.value = data.data.likeStatus;
    comments.value = data.data.comments || [];
    rows.value = data.data.rows || 0;
  }
};

const toggleLike = async () => {
  const { data } = await api.post("/likes", {
    entityType: 1,
    entityId: post.value.id,
    targetId: post.value.userId,
    postId: post.value.id
  });
  if (data.code === 0) {
    likeCount.value = data.data.likeCount;
    likeStatus.value = data.data.likeStatus;
  }
};

const likeComment = async (commentId, targetId) => {
  const { data } = await api.post("/likes", {
    entityType: 2,
    entityId: commentId,
    targetId,
    postId: post.value.id
  });
  if (data.code === 0) {
    await load();
  }
};

const submitComment = async () => {
  const hasText = commentText.value.trim().length > 0;
  if (!hasText && commentMedia.value.length === 0) return;
  const { data } = await api.post("/comments", {
    discussPostId: post.value.id,
    entityType: 1,
    entityId: post.value.id,
    targetId: 0,
    content: commentText.value,
    media: commentMedia.value
  });
  if (data.code === 0) {
    commentText.value = "";
    commentMedia.value = [];
    commentMessage.value = "";
    await load();
  } else {
    commentMessage.value = (data.data && data.data.reason) || data.msg || "Comment rejected.";
  }
};

const submitReply = async (commentId, targetId) => {
  const text = replyText[commentId] || "";
  const hasText = text.trim().length > 0;
  const mediaList = replyMedia[commentId] || [];
  if (!hasText && mediaList.length === 0) return;
  const { data } = await api.post("/comments", {
    discussPostId: post.value.id,
    entityType: 2,
    entityId: commentId,
    targetId,
    content: text,
    media: mediaList
  });
  if (data.code === 0) {
    replyText[commentId] = "";
    replyMedia[commentId] = [];
    replyMessage[commentId] = "";
    await load();
  } else {
    replyMessage[commentId] = (data.data && data.data.reason) || data.msg || "Reply rejected.";
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
      }
    } catch (error) {
      // ignore individual upload errors
    }
  }
  return uploaded;
};

const handleCommentFiles = async (event) => {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  commentUploading.value = true;
  const uploaded = await uploadFiles(files, "comment");
  commentMedia.value = commentMedia.value.concat(uploaded);
  commentUploading.value = false;
  event.target.value = "";
};

const handleReplyFiles = async (commentId, event) => {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  replyUploading[commentId] = true;
  const uploaded = await uploadFiles(files, "comment");
  if (!replyMedia[commentId]) {
    replyMedia[commentId] = [];
  }
  replyMedia[commentId] = replyMedia[commentId].concat(uploaded);
  replyUploading[commentId] = false;
  event.target.value = "";
};

const removeCommentMedia = (index) => {
  commentMedia.value.splice(index, 1);
};

const removeReplyMedia = (commentId, index) => {
  if (!replyMedia[commentId]) return;
  replyMedia[commentId].splice(index, 1);
};

const formatDate = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString();
};

onMounted(load);
</script>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  padding: 24px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.post-header h2 {
  margin: 10px 0 6px;
  font-family: "Space Grotesk", sans-serif;
}

.meta {
  color: var(--muted);
  font-size: 13px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.actions button {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 999px;
  padding: 6px 12px;
}

.post-body {
  line-height: 1.7;
  margin-top: 18px;
}

.reply-card {
  padding: 20px;
  display: grid;
  gap: 12px;
}

.reply-card textarea {
  min-height: 120px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 12px;
}

.reply-card .accent {
  border: none;
  background: var(--accent);
  color: #fff;
  border-radius: 999px;
  padding: 8px 16px;
  width: fit-content;
}

.comments {
  display: grid;
  gap: 14px;
}

.comment-card {
  padding: 16px;
  display: grid;
  gap: 8px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 13px;
}

.comment-actions button {
  border: none;
  background: transparent;
  color: var(--accent);
  cursor: pointer;
}

.replies {
  background: #faf7f1;
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.reply {
  font-size: 13px;
}

.reply-form {
  display: flex;
  gap: 8px;
}

.reply-form input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 12px;
}

.reply-form button {
  border: none;
  background: var(--accent-2);
  color: #fff;
  border-radius: 999px;
  padding: 8px 14px;
}

.media-upload,
.reply-media {
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

.upload-btn.small {
  font-size: 12px;
  padding: 4px 10px;
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
  .post-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .actions {
    align-items: flex-start;
  }
}
</style>
