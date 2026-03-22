import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080/community",
        changeOrigin: true
      },
      "/kaptcha": {
        target: "http://localhost:8080/community",
        changeOrigin: true
      },
      "/share": {
        target: "http://localhost:8080/community",
        changeOrigin: true
      }
    }
  }
});
