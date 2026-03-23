<template>
  <section v-if="profile" class="card profile">
    <div class="profile-header">
      <img :src="profile.user.headerUrl" alt="avatar" />
      <div>
        <h2>{{ profile.user.username }}</h2>
        <p class="muted">User ID {{ profile.user.id }}</p>
      </div>
      <button class="accent" @click="toggleFollow">
        {{ profile.hasFollowed ? 'Unfollow' : 'Follow' }}
      </button>
    </div>
    <div class="stats">
      <div>
        <strong>{{ profile.userLikeCount }}</strong>
        <span>Likes</span>
      </div>
      <div>
        <strong>{{ profile.followeeCount }}</strong>
        <span>Following</span>
      </div>
      <div>
        <strong>{{ profile.followerCount }}</strong>
        <span>Followers</span>
      </div>
    </div>
    <div class="links">
      <RouterLink :to="`/myposts/${profile.user.id}`">My Posts</RouterLink>
      <RouterLink :to="`/myreplies/${profile.user.id}`">My Replies</RouterLink>
      <RouterLink :to="`/followees/${profile.user.id}`">Followees</RouterLink>
      <RouterLink :to="`/followers/${profile.user.id}`">Followers</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/http";

const route = useRoute();
const profile = ref(null);

const load = async () => {
  const { data } = await api.get(`/users/${route.params.id}/profile`);
  if (data.code === 0) {
    profile.value = data.data;
  }
};

const toggleFollow = async () => {
  if (!profile.value) return;
  const endpoint = profile.value.hasFollowed ? "/unfollow" : "/follow";
  const { data } = await api.post(endpoint, {
    entityType: 3,
    entityId: profile.value.user.id
  });
  if (data.code === 0) {
    await load();
  }
};

onMounted(load);
</script>

<style scoped>
.profile {
  padding: 24px;
  display: grid;
  gap: 16px;
}

.profile-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  align-items: center;
}

.profile-header img {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  object-fit: cover;
}

.profile-header h2 {
  margin: 0 0 6px;
  font-family: "Space Grotesk", sans-serif;
}

.muted {
  color: var(--muted);
}

.accent {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: var(--accent);
  color: #fff;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stats div {
  background: #fff7ea;
  border-radius: 12px;
  padding: 12px;
  text-align: center;
}

.links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-family: "Space Grotesk", sans-serif;
}
</style>
