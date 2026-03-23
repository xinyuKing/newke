<template>
  <div class="auth card">
    <h2>One Login, Two Worlds</h2>
    <p class="muted">
      Sign in to the forum first, then we will bootstrap the mall JWT in the same flow so both sides of
      the integrated site are ready to use.
    </p>
    <form @submit.prevent="submit">
      <input v-model="form.username" placeholder="Username" />
      <input v-model="form.password" type="password" placeholder="Password" />
      <div class="captcha">
        <input v-model="form.code" placeholder="Captcha" />
        <img :src="captchaUrl" alt="captcha" @click="refreshCaptcha" />
      </div>
      <label class="remember">
        <input type="checkbox" v-model="form.rememberme" />
        Remember me
      </label>
      <button class="accent" type="submit">Login</button>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="notice" class="notice">{{ notice }}</p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "../api/http";
import { useAuthStore } from "../../../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const error = ref("");
const notice = ref("");
const captchaBase = import.meta.env.VITE_KAPTCHA_BASE || "/community/kaptcha";
const captchaUrl = ref(`${captchaBase}?ts=${Date.now()}`);
const form = reactive({
  username: "",
  password: "",
  code: "",
  rememberme: false
});

const refreshCaptcha = () => {
  captchaUrl.value = `${captchaBase}?ts=${Date.now()}`;
};

const submit = async () => {
  error.value = "";
  notice.value = "";
  const { data } = await api.post("/session/login", form);
  if (data.code !== 0) {
    const detail = data.data || {};
    error.value = detail.usernameMsg || detail.passwordMsg || detail.codeMsg || data.msg;
    refreshCaptcha();
    return;
  }

  const mallResult = await auth.initMallSession({
    username: form.username,
    password: form.password
  });
  await auth.fetchMe();

  const redirectPath =
    typeof route.query.redirect === "string" && route.query.redirect
      ? route.query.redirect
      : "/";

  if (!mallResult.ok) {
    if (redirectPath.startsWith("/mall")) {
      error.value = `Forum login succeeded, but mall session setup failed: ${mallResult.message}`;
      return;
    }
    notice.value = `Forum login is ready. Mall session setup failed: ${mallResult.message}`;
  }

  router.push(redirectPath);
};
</script>

<style scoped>
.auth {
  max-width: 460px;
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

.captcha {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha img {
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--border);
  cursor: pointer;
}

.remember {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--muted);
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

.notice {
  color: var(--accent-2);
  font-size: 13px;
}
</style>
