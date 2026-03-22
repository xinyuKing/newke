import { defineStore } from "pinia";
import api from "../api/http";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null,
    loading: false
  }),
  actions: {
    async fetchMe() {
      this.loading = true;
      try {
        const { data } = await api.get("/auth/me");
        if (data && data.code === 0) {
          this.user = data.data;
        }
      } finally {
        this.loading = false;
      }
    },
    async logout() {
      await api.post("/auth/logout");
      this.user = null;
    }
  }
});
