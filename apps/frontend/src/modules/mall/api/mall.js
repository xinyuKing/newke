import axios from "axios";

export const MALL_TOKEN_KEY = "newke.mall.token";
export const MALL_ROLE_KEY = "newke.mall.role";

const mallApi = axios.create({
  baseURL: import.meta.env.VITE_MALL_API_BASE || "/mall-api",
  withCredentials: false,
  timeout: 10000
});

mallApi.interceptors.request.use((config) => {
  const token = localStorage.getItem(MALL_TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

mallApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(MALL_TOKEN_KEY);
      localStorage.removeItem(MALL_ROLE_KEY);
    }
    return Promise.reject(error);
  }
);

export default mallApi;
