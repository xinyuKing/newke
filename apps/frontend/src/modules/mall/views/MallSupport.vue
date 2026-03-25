<template>
  <section class="page-stack">
    <div class="page-head card">
      <div>
        <div class="tag">Support</div>
        <h1>After-sale and chat</h1>
        <p class="muted">售后申请与客服会话都已经接到 `support-service`。</p>
      </div>
    </div>

    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>售后和客服记录属于私有业务数据，需要带有 `USER` 角色的登录态。</p>
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

          <div v-if="ticketForm.orderNo.trim()" class="order-preview">
            <p v-if="orderPreviewLoading" class="muted">Loading order items...</p>
            <p v-else-if="orderPreviewError" class="muted">{{ orderPreviewError }}</p>
            <template v-else-if="orderPreview">
              <div class="preview-head">
                <strong>{{ orderPreview.orderNo }}</strong>
                <span class="tag alt">{{ orderPreview.status }}</span>
              </div>
              <p class="muted">
                {{ formatDateTime(orderPreview.createdAt) }} · {{ orderPreview.items?.length || 0 }} items
              </p>

              <div class="scope-list">
                <button
                  type="button"
                  class="scope-pill"
                  :class="{ active: !ticketForm.skuId }"
                  @click="selectWholeOrder"
                >
                  <strong>Whole order</strong>
                  <span>Apply after-sale to every item in this order</span>
                </button>
                <button
                  v-for="item in orderPreview.items || []"
                  :key="item.skuId"
                  type="button"
                  class="scope-pill"
                  :class="{ active: String(item.skuId) === String(ticketForm.skuId || '') }"
                  @click="selectOrderItem(item)"
                >
                  <strong>{{ resolveOrderItemName(item) }}</strong>
                  <span>SKU {{ item.skuId }} · Qty {{ item.quantity }}</span>
                </button>
              </div>

              <p v-if="selectedOrderItem" class="muted">
                {{ resolveOrderItemDescription(selectedOrderItem) }}
              </p>
            </template>
          </div>

          <label>
            <span>{{ selectedOrderItem ? `Quantity (max ${selectedOrderItem.quantity})` : "Quantity (optional)" }}</span>
            <input
              v-model="ticketForm.quantity"
              type="number"
              min="1"
              :max="selectedOrderItem?.quantity || undefined"
              :disabled="!ticketForm.skuId"
              :placeholder="
                selectedOrderItem
                  ? 'Default is the full purchased quantity for this item'
                  : 'Pick a specific item first if you want a partial claim'
              "
            />
          </label>
          <label>
            <span>Reason</span>
            <textarea
              v-model="ticketForm.reason"
              rows="5"
              placeholder="Describe the issue, product condition, or refund reason."
            ></textarea>
          </label>
          <button type="button" class="solid" @click="createTicket">Submit after-sale</button>

          <div class="ticket-list">
            <article v-for="ticket in tickets" :key="ticket.id" class="ticket-item">
              <div class="line">
                <strong>{{ ticket.orderNo }}</strong>
                <span class="tag alt">{{ ticket.status }}</span>
              </div>
              <p class="muted">
                {{ ticketTargetLabel(ticket) }}
                <template v-if="ticket.skuId || ticket.quantity">
                  · Qty: {{ ticket.quantity || "ALL" }}
                </template>
              </p>
              <p v-if="ticket.productDescription" class="muted">{{ ticket.productDescription }}</p>
              <p>{{ ticket.reason }}</p>
              <div v-if="ticket.evidenceNote || safeEvidenceUrls(ticket).length || hasBlockedEvidenceUrls(ticket)" class="evidence-view">
                <p v-if="ticket.evidenceNote" class="muted">Evidence note: {{ ticket.evidenceNote }}</p>
                <div v-if="safeEvidenceUrls(ticket).length" class="evidence-links">
                  <a
                    v-for="url in safeEvidenceUrls(ticket)"
                    :key="url"
                    :href="url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {{ url }}
                  </a>
                </div>
                <p v-if="hasBlockedEvidenceUrls(ticket)" class="muted">Some unsupported links were hidden for safety.</p>
              </div>
              <div v-if="isEvidenceEditable(ticket)" class="evidence-form">
                <label>
                  <span>Evidence note</span>
                  <textarea
                    v-model="ensureEvidenceDraft(ticket).note"
                    rows="3"
                    placeholder="Add proof details, package condition, or what the support team should verify."
                  ></textarea>
                </label>
                <label>
                  <span>Evidence URLs</span>
                  <textarea
                    v-model="ensureEvidenceDraft(ticket).urls"
                    rows="3"
                    placeholder="Paste photo or video links, one per line or separated by commas."
                  ></textarea>
                </label>
                <button type="button" class="ghost" @click="submitEvidence(ticket)">Submit evidence</button>
              </div>
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
            <textarea
              v-model="chatText"
              rows="4"
              :disabled="!currentSessionId"
              placeholder="Type the next support message."
            ></textarea>
          </label>
          <button type="button" class="solid" :disabled="!currentSessionId" @click="sendMessage">Send</button>
        </section>
      </div>

      <p v-if="message" class="muted">{{ message }}</p>
    </template>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch, computed } from "vue";
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
const orderPreview = ref(null);
const orderPreviewLoading = ref(false);
const orderPreviewError = ref("");
const orderPreviewRequestToken = ref(0);
const evidenceDrafts = reactive({});
const safeEvidenceProtocols = new Set(["http:", "https:"]);
const ticketForm = reactive({
  orderNo: "",
  skuId: "",
  quantity: "",
  reason: ""
});

let orderPreviewTimer = null;

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

const selectedOrderItem = computed(() => {
  const skuId = normalizeOptionalLong(ticketForm.skuId);
  if (!skuId || !Array.isArray(orderPreview.value?.items)) {
    return null;
  }
  return orderPreview.value.items.find((item) => item.skuId === skuId) || null;
});

const loadTickets = async () => {
  const { data } = await mallApi.get("/user/after-sale");
  tickets.value = data?.success ? data.data || [] : [];
  tickets.value.forEach((ticket) => {
    ensureEvidenceDraft(ticket);
  });
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

const loadOrderPreview = async (rawOrderNo) => {
  const orderNo = typeof rawOrderNo === "string" ? rawOrderNo.trim() : "";
  const requestToken = ++orderPreviewRequestToken.value;
  if (!orderNo) {
    orderPreview.value = null;
    orderPreviewError.value = "";
    orderPreviewLoading.value = false;
    ticketForm.skuId = "";
    ticketForm.quantity = "";
    return;
  }
  orderPreviewLoading.value = true;
  orderPreviewError.value = "";
  try {
    const { data } = await mallApi.get(`/user/orders/${orderNo}`);
    if (requestToken !== orderPreviewRequestToken.value) return;
    if (data?.success && data.data) {
      orderPreview.value = data.data;
      const currentSkuId = normalizeOptionalLong(ticketForm.skuId);
      const stillExists = data.data.items?.some((item) => item.skuId === currentSkuId);
      if (!stillExists) {
        ticketForm.skuId = "";
        ticketForm.quantity = "";
      }
      if (selectedOrderItem.value && normalizeOptionalInt(ticketForm.quantity) > selectedOrderItem.value.quantity) {
        ticketForm.quantity = String(selectedOrderItem.value.quantity);
      }
      return;
    }
    orderPreview.value = null;
    orderPreviewError.value = data?.message || "Order preview unavailable.";
    ticketForm.skuId = "";
    ticketForm.quantity = "";
  } catch (error) {
    if (requestToken !== orderPreviewRequestToken.value) return;
    orderPreview.value = null;
    orderPreviewError.value = "Order preview unavailable.";
    ticketForm.skuId = "";
    ticketForm.quantity = "";
  } finally {
    if (requestToken === orderPreviewRequestToken.value) {
      orderPreviewLoading.value = false;
    }
  }
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

const selectWholeOrder = () => {
  ticketForm.skuId = "";
  ticketForm.quantity = "";
};

const selectOrderItem = (item) => {
  ticketForm.skuId = String(item?.skuId || "");
  const currentQuantity = normalizeOptionalInt(ticketForm.quantity);
  if (currentQuantity && item?.quantity && currentQuantity > item.quantity) {
    ticketForm.quantity = String(item.quantity);
  }
};

const resolveOrderItemName = (item) => item?.productName || `Product ${item?.skuId}`;

const resolveOrderItemDescription = (item) =>
  item?.productDescription || "Product snapshot unavailable for this order item.";

const ticketTargetLabel = (ticket) => {
  if (!ticket?.skuId) {
    return "Whole order";
  }
  return ticket.productName || `SKU ${ticket.skuId}`;
};

const createTicket = async () => {
  message.value = "";
  const orderNo = ticketForm.orderNo.trim();
  if (!orderNo) {
    message.value = "Order number required.";
    return;
  }
  if (selectedOrderItem.value) {
    const quantity = normalizeOptionalInt(ticketForm.quantity);
    if (quantity && quantity > selectedOrderItem.value.quantity) {
      message.value = `Quantity exceeds the purchased amount for ${resolveOrderItemName(selectedOrderItem.value)}.`;
      return;
    }
  }
  const { data } = await mallApi.post("/user/after-sale", {
    orderNo,
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

const ensureEvidenceDraft = (ticket) => {
  if (!ticket?.id) {
    return { note: "", urls: "" };
  }
  if (!evidenceDrafts[ticket.id]) {
    evidenceDrafts[ticket.id] = {
      note: ticket.evidenceNote || "",
      urls: Array.isArray(ticket.evidenceUrls) ? ticket.evidenceUrls.join("\n") : ""
    };
  }
  return evidenceDrafts[ticket.id];
};

const isEvidenceEditable = (ticket) =>
  ticket && ["INIT", "WAIT_PROOF", "REVIEWING"].includes(String(ticket.status || ""));

const toSafeEvidenceUrl = (value) => {
  const trimmed = typeof value === "string" ? value.trim() : "";
  if (!trimmed) {
    return "";
  }
  try {
    const parsed = new URL(trimmed);
    return safeEvidenceProtocols.has(parsed.protocol) ? parsed.href : "";
  } catch {
    return "";
  }
};

const safeEvidenceUrls = (ticket) =>
  (Array.isArray(ticket?.evidenceUrls) ? ticket.evidenceUrls : [])
    .map((url) => toSafeEvidenceUrl(url))
    .filter(Boolean);

const hasBlockedEvidenceUrls = (ticket) =>
  (Array.isArray(ticket?.evidenceUrls) ? ticket.evidenceUrls : []).some((url) => !toSafeEvidenceUrl(url));

const submitEvidence = async (ticket) => {
  message.value = "";
  const draft = ensureEvidenceDraft(ticket);
  const evidenceNote = draft.note.trim();
  const evidenceUrls = draft.urls
    .split(/[\n,]/)
    .map((value) => value.trim())
    .filter(Boolean);
  const { data } = await mallApi.put(`/user/after-sale/${ticket.id}/evidence`, {
    evidenceNote,
    evidenceUrls
  });
  if (data?.success) {
    await loadTickets();
    message.value = "Evidence submitted.";
    return;
  }
  message.value = data?.message || "Submit evidence failed.";
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

watch(
  () => ticketForm.orderNo,
  (value) => {
    if (orderPreviewTimer) {
      clearTimeout(orderPreviewTimer);
    }
    orderPreviewTimer = setTimeout(() => {
      loadOrderPreview(value);
    }, 250);
  },
  { immediate: true }
);

watch(selectedOrderItem, (item) => {
  if (!item) {
    ticketForm.quantity = "";
    return;
  }
  const quantity = normalizeOptionalInt(ticketForm.quantity);
  if (quantity && quantity > item.quantity) {
    ticketForm.quantity = String(item.quantity);
  }
});

onBeforeUnmount(() => {
  if (orderPreviewTimer) {
    clearTimeout(orderPreviewTimer);
  }
});

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
.line,
.preview-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.order-preview,
.scope-list {
  display: grid;
  gap: 10px;
}

.scope-list {
  grid-template-columns: 1fr;
}

.scope-pill {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.scope-pill.active {
  border-color: transparent;
  background: rgba(42, 157, 143, 0.1);
}

.scope-pill span {
  color: var(--muted);
  font-size: 13px;
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

.evidence-view,
.evidence-form,
.evidence-links {
  display: grid;
  gap: 8px;
}

.evidence-links a {
  color: var(--accent-2);
  word-break: break-all;
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
