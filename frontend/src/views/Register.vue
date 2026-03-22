<template>
  <div class="auth card">
    <h2>Create Account</h2>
    <p class="muted">Join the conversation.</p>
    <form @submit.prevent="submit">
      <input v-model="form.username" placeholder="Username" />
      <input v-model="form.password" type="password" placeholder="Password" />
      <input v-model="form.email" type="email" placeholder="Email" />
      <button class="accent" type="submit">Register</button>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">Registration success. Check your email to activate.</p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import api from "../api/http";

const form = reactive({
  username: "",
  password: "",
  email: ""
});
const error = ref("");
const success = ref(false);

const submit = async () => {
  error.value = "";
  success.value = false;
  const { data } = await api.post("/auth/register", form);
  if (data.code === 0) {
    success.value = true;
    return;
  }
  const detail = data.data || {};
  error.value = detail.usernameMsg || detail.passwordMsg || detail.emailMsg || data.msg;
};
</script>

<style scoped>
.auth {
  max-width: 420px;
  margin: 40px auto;
  padding: 28px;
  display: grid;
  gap: 12px;
}

.auth h2 {
  font-family: "Space Grotesk", sans-serif;
  margin: 0;
}

.muted {
  color: var(--muted);
}

form {
  display: grid;
  gap: 12px;
}

input {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 14px;
}

.accent {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}

.error {
  color: #b83a2e;
  font-size: 13px;
}

.success {
  color: #2a9d8f;
  font-size: 13px;
}
</style>
