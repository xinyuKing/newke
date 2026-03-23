import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const unifiedGatewayTarget = env.VITE_GATEWAY_PROXY_TARGET || "http://localhost:8080";
  const forumTarget = env.VITE_FORUM_PROXY_TARGET || unifiedGatewayTarget;
  const mallTarget = env.VITE_MALL_PROXY_TARGET || unifiedGatewayTarget;
  const rewriteMallApi = (path) => path.replace(/^\/mall-api/, "/api");

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        "/community": {
          target: forumTarget,
          changeOrigin: true
        },
        "/mall-api": {
          target: mallTarget,
          changeOrigin: true,
          rewrite: rewriteMallApi
        }
      }
    }
  };
});
