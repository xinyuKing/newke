import axios from "axios";

export const MALL_TOKEN_KEY = "newke.mall.token";
export const MALL_ROLE_KEY = "newke.mall.role";

const mallApi = axios.create({
  baseURL: import.meta.env.VITE_MALL_API_BASE || "/api",
  withCredentials: false,
  timeout: 10000
});

const toFailureResponse = (error) => {
  const response = error?.response;
  const responseData = response?.data;
  const fallbackMessage =
    response?.statusText ||
    error?.message ||
    "Network unavailable. Please retry.";
  const normalizedData =
    responseData && typeof responseData === "object"
      ? {
          ...responseData,
          success: false,
          message: responseData.message || fallbackMessage
        }
      : {
          success: false,
          message: fallbackMessage
        };

  return {
    data: normalizedData,
    status: response?.status || 0,
    statusText: response?.statusText || "ERROR",
    headers: response?.headers || {},
    config: error?.config,
    request: response?.request || error?.request
  };
};

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
    return Promise.resolve(toFailureResponse(error));
  }
);

export default mallApi;
