<template>
  <section class="page-stack">
    <div class="page-head card">
      <div>
        <div class="tag">Support</div>
        <h1>After-sale and chat</h1>
        <p class="muted">售后申请与客服会话都已经接到了 `support-service`。</p>
      </div>
    </div>

    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>售后和客服记录属于私有业务数据，需要商城 `USER` 角色登录态。</p>
      <RouterLink class="ghost" to="/login?redirect=/mall/support">Login to contact support</RouterLink>
    </div>

    <template v-else>
      <div class="support-layout">
        <section class="card ticket-panel">
          <div class="section-head">
            <div>
              <div class="tag alt">After-sale</div>
              <h3>Create request</h3>
            </div>
          </div>

          <label>
            <span>Order number</span>
            <input v-model="ticketForm.orderNo" placeholder="Enter order number" />
          </label>
          <label>
            <span>SKU ID (optional)</span>
            <input v-model="ticketForm.skuId" placeholder="Submit a per-item after-sale if needed" />
          </label>
          <label>
            <span>Quantity (optional)</span>
            <input
              v-model="ticketForm.quantity"
              type="number"
              min="1"
              placeholder="Default is the full purchased quantity for that SKU"
            />
          </label>
          <label>
            <span>Reason</span>
            <textarea v-model="ticketForm.reason" rows="5" placeholder="Describe the issue, product condition, or refund reason."></textarea>
          </label>
          <button type="button" class="solid" @click="createTicket">Submit after-sale</button>

          <div class="ticket-list">
            <article v-for="ticket in tickets" :key="ticket.id" class="ticket-item">
              <div class="line">
                <strong>{{ ticket.orderNo }}</strong>
                <span class="tag alt">{{ ticket.status }}</span>
              </div>
              <p v-if="ticket.skuId || ticket.quantity" class="muted">
                SKU: {{ ticket.skuId || "ALL" }} · Qty: {{ ticket.quantity || "ALL" }}
              </p>
              <p>{{ ticket.reason }}</p>
              <span class="muted">{{ formatDateTime(ticket.createdAt) }}</span>
            </article>
          </div>
        </section>

        <section class="card chat-panel">
          <div class="section-head">
            <div>
              <div class="tag alt">Chat</div>
              <h3>Support session</h3>
            </div>
            <button type="button" class="ghost" @click="startSession">Start session</button>
          </div>

          <div v-if="sessions.length" class="session-list">
            <button
              v-for="session in sessions"
              :key="session.sessionId"
              type="button"
              class="session-pill"
              :class="{ active: session.sessionId === currentSessionId }"
              @click="selectSession(session.sessionId)"
            >
              <span>{{ session.sessionId.slice(0, 8) }}</span>
              <small>{{ session.status }}</small>
            </button>
          </div>
          <p v-else class="muted">No chat session yet. Start one to talk with support.</p>

          <div class="message-list">
            <article v-for="item in messages" :key="item.id" class="message-item" :class="String(item.senderRole || '').toLowerCase()">
              <strong>{{ item.senderRole }}</strong>
              <p>{{ item.content }}</p>
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </article>
          </div>

          <label>
            <span>Message</span>
            <textarea v-model="chatText" rows="4" :disabled="!currentSessionId" placeholder="Type the next support message."></textarea>
          </label>
          <button type="button" class="solid" :disabled="!currentSessionId" @click="sendMessage">Send</button>
        </section>
      </div>

      <p v-if="message" class="muted">{{ message }}</p>
    </template>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";
import { formatDateTime } from "../utils/format";

const auth = useAuthStore();
const route = useRoute();

const message = ref("");
const tickets = ref([]);
const sessions = ref([]);
const messages = ref([]);
const currentSessionId = ref("");
const chatText = ref("");
const ticketForm = reactive({
  orderNo: "",
  skuId: "",
  quantity: "",
  reason: ""
});

const loadTickets = async () => {
  const { data } = await mallApi.get("/user/after-sale");
  tickets.value = data?.success ? data.data || [] : [];
};

const loadSessions = async () => {
  const { data } = await mallApi.get("/user/support/sessions");
  sessions.value = data?.success ? data.data || [] : [];
  if (!currentSessionId.value && sessions.value.length) {
    currentSessionId.value = sessions.value[0].sessionId;
    await loadMessages(currentSessionId.value);
  }
};

const loadMessages = async (sessionId) => {
  if (!sessionId) {
    messages.value = [];
    return;
  }
  const { data } = await mallApi.get(`/user/support/session/${sessionId}/messages`);
  messages.value = data?.success ? data.data || [] : [];
};

const selectSession = async (sessionId) => {
  currentSessionId.value = sessionId;
  await loadMessages(sessionId);
};

const startSession = async () => {
  message.value = "";
  const { data } = await mallApi.post("/user/support/session");
  if (data?.success && data.data?.sessionId) {
    currentSessionId.value = data.data.sessionId;
    await loadSessions();
    await loadMessages(currentSessionId.value);
    message.value = "Support session started.";
    return;
  }
  message.value = data?.message || "Start session failed.";
};

const sendMessage = async () => {
  message.value = "";
  const content = chatText.value.trim();
  if (!currentSessionId.value || !content) {
    message.value = "Select a session and enter a message first.";
    return;
  }
  const { data } = await mallApi.post("/user/support/message", {
    sessionId: currentSessionId.value,
    content
  });
  if (data?.success) {
    chatText.value = "";
    await loadMessages(currentSessionId.value);
    return;
  }
  message.value = data?.message || "Send message failed.";
};

const createTicket = async () => {
  message.value = "";
  const { data } = await mallApi.post("/user/after-sale", {
    orderNo: ticketForm.orderNo,
    skuId: normalizeOptionalLong(ticketForm.skuId),
    quantity: normalizeOptionalInt(ticketForm.quantity),
    reason: ticketForm.reason
  });
  if (data?.success) {
    ticketForm.skuId = "";
    ticketForm.quantity = "";
    ticketForm.reason = "";
    await loadTickets();
    message.value = "After-sale request submitted.";
    return;
  }
  message.value = data?.message || "Create after-sale request failed.";
};

watch(
  () => route.query.orderNo,
  (value) => {
    if (typeof value === "string" && value.trim()) {
      ticketForm.orderNo = value.trim();
    }
  },
  { immediate: true }
);

const normalizeOptionalLong = (value) => {
  if (value === null || value === undefined || String(value).trim() === "") {
    return undefined;
  }
  const parsed = Number.parseInt(String(value), 10);
  return Number.isNaN(parsed) ? undefined : parsed;
};

const normalizeOptionalInt = (value) => {
  if (value === null || value === undefined || String(value).trim() === "") {
    return undefined;
  }
  const parsed = Number.parseInt(String(value), 10);
  return Number.isNaN(parsed) ? undefined : parsed;
};

onMounted(async () => {
  if (!auth.mallCanShop) return;
  await Promise.all([loadTickets(), loadSessions()]);
});
</script>

<style scoped>
.page-stack {
  display: grid;
  gap: 18px;
}

.page-head,
.ticket-panel,
.chat-panel,
.empty {
  padding: 22px;
}

.page-head h1,
.ticket-panel h3,
.chat-panel h3,
.empty h3 {
  margin: 10px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.support-layout {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 18px;
}

.ticket-panel,
.chat-panel {
  display: grid;
  gap: 14px;
}

.section-head,
.line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  color: var(--muted);
  font-size: 13px;
}

input,
textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
}

.ticket-list,
.message-list {
  display: grid;
  gap: 10px;
}

.ticket-item,
.message-item {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fffdfa;
}

.ticket-item p,
.message-item p {
  margin: 8px 0;
  line-height: 1.7;
}

.session-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.session-pill,
.solid,
.ghost {
  font-family: "Space Grotesk", sans-serif;
}

.session-pill {
  display: inline-grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.session-pill.active {
  border-color: transparent;
  background: var(--ink);
  color: #fff;
}

.message-item.user {
  background: rgba(42, 157, 143, 0.08);
}

.message-item.support {
  background: rgba(232, 106, 93, 0.08);
}

.message-item span {
  color: var(--muted);
  font-size: 12px;
}

.solid,
.ghost {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  padding: 10px 14px;
  text-decoration: none;
  cursor: pointer;
}

.solid {
  border: none;
  background: var(--ink);
  color: #fff;
}

.ghost {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--ink);
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

@media (max-width: 920px) {
  .support-layout {
    grid-template-columns: 1fr;
  }
}
</style>
