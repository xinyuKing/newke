<template>
  <section class="page-stack">
    <div class="page-head card">
      <div>
        <div class="tag">Account</div>
        <h1>Mall profile</h1>
        <p class="muted">用户资料和地址管理对接 `auth-service`，与论坛账号主身份保持对齐。</p>
      </div>
    </div>

    <div v-if="!auth.mallCanShop" class="empty card">
      <h3>Shopper role required</h3>
      <p>账户资料和地址簿属于商城私有能力，需要商城 `USER` 角色的 JWT 登录态。</p>
      <RouterLink class="ghost" to="/login?redirect=/mall/account">Login to manage account</RouterLink>
    </div>

    <template v-else>
      <div class="account-layout">
        <section class="card profile-card">
          <h3>Profile</h3>
          <label>
            <span>Username</span>
            <input :value="profileForm.username" disabled />
          </label>
          <label>
            <span>Nickname</span>
            <input v-model="profileForm.nickname" />
          </label>
          <label>
            <span>Avatar URL</span>
            <input v-model="profileForm.avatarUrl" />
          </label>
          <label>
            <span>Email</span>
            <input v-model="profileForm.email" type="email" />
          </label>
          <label>
            <span>Phone</span>
            <input v-model="profileForm.phone" />
          </label>
          <button type="button" class="solid" @click="saveProfile">Save profile</button>
        </section>

        <section class="card address-card">
          <div class="address-head">
            <h3>{{ editingAddressId ? "Edit address" : "Add address" }}</h3>
            <button v-if="editingAddressId" type="button" class="ghost" @click="resetAddressForm">Cancel edit</button>
          </div>

          <div class="address-grid">
            <label>
              <span>Receiver</span>
              <input v-model="addressForm.receiverName" />
            </label>
            <label>
              <span>Phone</span>
              <input v-model="addressForm.receiverPhone" />
            </label>
            <label>
              <span>Province</span>
              <input v-model="addressForm.province" />
            </label>
            <label>
              <span>City</span>
              <input v-model="addressForm.city" />
            </label>
            <label>
              <span>District</span>
              <input v-model="addressForm.district" />
            </label>
            <label>
              <span>Postal code</span>
              <input v-model="addressForm.postalCode" />
            </label>
            <label class="full">
              <span>Detail address</span>
              <input v-model="addressForm.detailAddress" />
            </label>
            <label>
              <span>Tag</span>
              <input v-model="addressForm.tag" placeholder="Home / Office" />
            </label>
            <label class="toggle">
              <input v-model="addressForm.isDefault" type="checkbox" />
              <span>Set as default</span>
            </label>
          </div>

          <button type="button" class="solid" @click="submitAddress">
            {{ editingAddressId ? "Update address" : "Add address" }}
          </button>
        </section>
      </div>

      <section class="card address-list">
        <div class="address-head">
          <h3>Saved addresses</h3>
          <span class="muted">{{ addresses.length }} records</span>
        </div>

        <div v-if="addresses.length" class="address-items">
          <article v-for="address in addresses" :key="address.id" class="address-item">
            <div>
              <div class="line">
                <strong>{{ address.receiverName }}</strong>
                <span v-if="address.isDefault" class="tag alt">Default</span>
              </div>
              <p>{{ address.province }}{{ address.city }}{{ address.district }} {{ address.detailAddress }}</p>
              <p class="muted">{{ address.receiverPhone }} · {{ address.tag || "No tag" }}</p>
            </div>
            <div class="address-actions">
              <button type="button" class="ghost" @click="editAddress(address)">Edit</button>
              <button v-if="!address.isDefault" type="button" class="ghost" @click="setDefault(address.id)">Default</button>
              <button type="button" class="ghost danger" @click="removeAddress(address.id)">Delete</button>
            </div>
          </article>
        </div>
        <div v-else class="empty-note">No address yet. Add one to make checkout smoother.</div>
      </section>

      <p v-if="message" class="muted">{{ message }}</p>
    </template>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import mallApi from "../api/mall";
import { useAuthStore } from "../../../stores/auth";

const auth = useAuthStore();

const message = ref("");
const addresses = ref([]);
const editingAddressId = ref(null);

const profileForm = reactive({
  username: "",
  nickname: "",
  avatarUrl: "",
  email: "",
  phone: ""
});

const createEmptyAddress = () => ({
  receiverName: "",
  receiverPhone: "",
  province: "",
  city: "",
  district: "",
  detailAddress: "",
  postalCode: "",
  tag: "",
  isDefault: false
});

const addressForm = reactive(createEmptyAddress());

const loadProfile = async () => {
  const { data } = await mallApi.get("/user/profile");
  if (data?.success && data.data) {
    profileForm.username = data.data.username || "";
    profileForm.nickname = data.data.nickname || "";
    profileForm.avatarUrl = data.data.avatarUrl || "";
    profileForm.email = data.data.email || "";
    profileForm.phone = data.data.phone || "";
  }
};

const loadAddresses = async () => {
  const { data } = await mallApi.get("/user/addresses");
  addresses.value = data?.success ? data.data || [] : [];
};

const resetAddressForm = () => {
  Object.assign(addressForm, createEmptyAddress());
  editingAddressId.value = null;
};

const saveProfile = async () => {
  message.value = "";
  const { data } = await mallApi.put("/user/profile", {
    nickname: profileForm.nickname,
    avatarUrl: profileForm.avatarUrl,
    email: profileForm.email,
    phone: profileForm.phone
  });
  if (data?.success) {
    message.value = "Profile updated.";
    await auth.fetchMallProfile();
    return;
  }
  message.value = data?.message || "Update profile failed.";
};

const submitAddress = async () => {
  message.value = "";
  const payload = { ...addressForm };
  const request = editingAddressId.value
    ? mallApi.put(`/user/addresses/${editingAddressId.value}`, payload)
    : mallApi.post("/user/addresses", payload);
  const { data } = await request;
  if (data?.success) {
    message.value = editingAddressId.value ? "Address updated." : "Address added.";
    resetAddressForm();
    await loadAddresses();
    return;
  }
  message.value = data?.message || "Save address failed.";
};

const editAddress = (address) => {
  editingAddressId.value = address.id;
  Object.assign(addressForm, {
    receiverName: address.receiverName || "",
    receiverPhone: address.receiverPhone || "",
    province: address.province || "",
    city: address.city || "",
    district: address.district || "",
    detailAddress: address.detailAddress || "",
    postalCode: address.postalCode || "",
    tag: address.tag || "",
    isDefault: Boolean(address.isDefault)
  });
};

const setDefault = async (id) => {
  message.value = "";
  const { data } = await mallApi.put(`/user/addresses/${id}/default`);
  if (data?.success) {
    message.value = "Default address updated.";
    await loadAddresses();
    return;
  }
  message.value = data?.message || "Set default failed.";
};

const removeAddress = async (id) => {
  message.value = "";
  const { data } = await mallApi.delete(`/user/addresses/${id}`);
  if (data?.success) {
    message.value = "Address deleted.";
    await loadAddresses();
    return;
  }
  message.value = data?.message || "Delete address failed.";
};

onMounted(async () => {
  if (!auth.mallCanShop) return;
  await Promise.all([loadProfile(), loadAddresses()]);
});
</script>

<style scoped>
.page-stack {
  display: grid;
  gap: 18px;
}

.page-head,
.profile-card,
.address-card,
.address-list,
.empty {
  padding: 22px;
}

.page-head h1,
.profile-card h3,
.address-card h3,
.address-list h3,
.empty h3 {
  margin: 10px 0 0;
  font-family: "Space Grotesk", sans-serif;
}

.account-layout {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
}

.profile-card,
.address-card,
.address-list {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  color: var(--muted);
  font-size: 13px;
}

input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
}

.address-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.address-grid .full {
  grid-column: 1 / -1;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.toggle input {
  width: auto;
}

.address-items {
  display: grid;
  gap: 12px;
}

.address-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
}

.address-item p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.line {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.address-actions {
  display: grid;
  gap: 8px;
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
  font-family: "Space Grotesk", sans-serif;
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

.danger {
  color: #b83a2e;
}

.tag.alt {
  background: rgba(42, 157, 143, 0.1);
  color: var(--accent-2);
}

.empty-note {
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 920px) {
  .account-layout,
  .address-grid,
  .address-item {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
