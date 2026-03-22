<template>
  <section class="stack">
    <div class="card panel">
      <h2>Admin Data</h2>
      <p class="muted">UV & DAU stats</p>
    </div>

    <div class="card panel">
      <h3>UV</h3>
      <form @submit.prevent="fetchUv">
        <input v-model="uv.start" type="date" />
        <input v-model="uv.end" type="date" />
        <button class="accent" type="submit">Query</button>
      </form>
      <p v-if="uv.result !== null" class="muted">UV: {{ uv.result }}</p>
    </div>

    <div class="card panel">
      <h3>DAU</h3>
      <form @submit.prevent="fetchDau">
        <input v-model="dau.start" type="date" />
        <input v-model="dau.end" type="date" />
        <button class="accent" type="submit">Query</button>
      </form>
      <p v-if="dau.result !== null" class="muted">DAU: {{ dau.result }}</p>
    </div>
  </section>
</template>

<script setup>
import { reactive } from "vue";
import api from "../api/http";

const uv = reactive({ start: "", end: "", result: null });
const dau = reactive({ start: "", end: "", result: null });

const fetchUv = async () => {
  const { data } = await api.get("/admin/data/uv", { params: uv });
  if (data.code === 0) uv.result = data.data.uv;
};

const fetchDau = async () => {
  const { data } = await api.get("/admin/data/dau", { params: dau });
  if (data.code === 0) dau.result = data.data.dau;
};
</script>

<style scoped>
.stack { display: grid; gap: 16px; }
.panel { padding: 20px; display: grid; gap: 12px; }
.panel h2, .panel h3 { margin: 0; font-family: "Space Grotesk", sans-serif; }
form { display: flex; gap: 10px; flex-wrap: wrap; }
input { border: 1px solid var(--border); border-radius: 12px; padding: 8px 12px; }
.accent { border: none; border-radius: 999px; padding: 8px 14px; background: var(--accent); color: #fff; }
.muted { color: var(--muted); }
</style>
