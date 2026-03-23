import { defineStore } from "pinia";
import api from "../modules/forum/api/http";
import mallApi, { MALL_ROLE_KEY, MALL_TOKEN_KEY } from "../modules/mall/api/mall";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null,
    loading: false,
    mallToken: localStorage.getItem(MALL_TOKEN_KEY),
    mallRole: localStorage.getItem(MALL_ROLE_KEY),
    mallProfile: null,
    mallLoading: false
  }),
  getters: {
    mallLoggedIn: (state) => Boolean(state.mallToken),
    mallCanShop: (state) => state.mallRole === "USER",
    displayName: (state) =>
      state.mallProfile?.nickname ||
      state.mallProfile?.username ||
      state.user?.username ||
      "Guest"
  },
  actions: {
    async fetchMe() {
      this.loading = true;
      try {
        const { data } = await api.get("/session/me");
        if (data && data.code === 0) {
          this.user = data.data;
        } else {
          this.user = null;
        }
      } catch (error) {
        this.user = null;
      } finally {
        this.loading = false;
      }
      if (this.mallToken) {
        await this.fetchMallProfile();
      }
    },
    async fetchMallProfile() {
      if (!this.mallToken) {
        this.mallProfile = null;
        this.mallRole = null;
        return null;
      }
      this.mallLoading = true;
      try {
        const { data } = await mallApi.get("/user/profile");
        if (data?.success) {
          this.mallProfile = data.data;
          return data.data;
        }
        if (data?.message === "Unauthorized") {
          this.clearMallSession();
        }
        return null;
      } catch (error) {
        if (error.response?.status === 401) {
          this.clearMallSession();
        }
        return null;
      } finally {
        this.mallLoading = false;
      }
    },
    async initMallSession(credentials) {
      try {
        const { data } = await mallApi.post("/auth/login", credentials);
        if (!data?.success || !data.data?.token) {
          return {
            ok: false,
            message: data?.message || "Mall login failed."
          };
        }
        this.mallToken = data.data.token;
        this.mallRole = data.data.role || null;
        localStorage.setItem(MALL_TOKEN_KEY, this.mallToken);
        if (this.mallRole) {
          localStorage.setItem(MALL_ROLE_KEY, this.mallRole);
        }
        await this.fetchMallProfile();
        return { ok: true };
      } catch (error) {
        return {
          ok: false,
          message: error.response?.data?.message || error.message || "Mall login failed."
        };
      }
    },
    clearMallSession() {
      this.mallToken = null;
      this.mallRole = null;
      this.mallProfile = null;
      localStorage.removeItem(MALL_TOKEN_KEY);
      localStorage.removeItem(MALL_ROLE_KEY);
    },
    async logout() {
      try {
        await api.post("/session/logout");
      } finally {
        this.user = null;
        this.clearMallSession();
      }
    }
  }
});
