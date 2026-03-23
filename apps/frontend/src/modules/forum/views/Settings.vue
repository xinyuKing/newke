<template>
  <section class="stack">
    <div class="card panel">
      <h3>Update Password</h3>
      <form @submit.prevent="updatePassword">
        <input v-model="passwordForm.oldPassword" type="password" placeholder="Current password" />
        <input v-model="passwordForm.newPassword" type="password" placeholder="New password" />
        <button class="accent" type="submit">Save</button>
        <p v-if="passwordMsg" class="msg">{{ passwordMsg }}</p>
      </form>
    </div>

    <div class="card panel">
      <h3>Update Avatar URL</h3>
      <form @submit.prevent="updateAvatar">
        <input v-model="avatarUrl" placeholder="https://..." />
        <button class="accent" type="submit">Update</button>
        <p v-if="avatarMsg" class="msg">{{ avatarMsg }}</p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import api from "../api/http";

const passwordForm = reactive({
  oldPassword: "",
  newPassword: ""
});
const avatarUrl = ref("");
const passwordMsg = ref("");
const avatarMsg = ref("");

const updatePassword = async () => {
  passwordMsg.value = "";
  const { data } = await api.put("/users/password", passwordForm);
  if (data.code === 0) {
    passwordMsg.value = "Password updated.";
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
  } else {
    passwordMsg.value = data.msg || "Update failed";
  }
};

const updateAvatar = async () => {
  avatarMsg.value = "";
  const { data } = await api.put("/users/header", { headerUrl: avatarUrl.value });
  if (data.code === 0) {
    avatarMsg.value = "Avatar updated.";
  } else {
    avatarMsg.value = data.msg || "Update failed";
  }
};
</script>

<style scoped>
.stack {
  display: grid;
  gap: 18px;
}

.panel {
  padding: 20px;
  display: grid;
  gap: 12px;
}

.panel h3 {
  font-family: "Space Grotesk", sans-serif;
  margin: 0;
}

form {
  display: grid;
  gap: 10px;
}

input {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
}

.accent {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: var(--accent);
  color: #fff;
}

.msg {
  color: var(--muted);
  font-size: 13px;
}
</style>
