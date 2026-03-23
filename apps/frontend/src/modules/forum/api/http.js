import axios from "axios";

const CSRF_COOKIE_NAME = "forum_csrf";
const CSRF_HEADER_NAME = "X-CSRF-Token";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

const readCookie = (name) => {
  if (typeof document === "undefined") return "";
  const nameEq = `${name}=`;
  const parts = document.cookie ? document.cookie.split(";") : [];
  for (const rawPart of parts) {
    const part = rawPart.trim();
    if (part.startsWith(nameEq)) {
      return decodeURIComponent(part.slice(nameEq.length));
    }
  }
  return "";
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/community/api",
  withCredentials: true
});

api.interceptors.request.use((config) => {
  const method = (config.method || "get").toUpperCase();
  if (SAFE_METHODS.has(method)) {
    return config;
  }
  const token = readCookie(CSRF_COOKIE_NAME);
  if (!token) {
    return config;
  }
  config.headers = config.headers || {};
  config.headers[CSRF_HEADER_NAME] = token;
  return config;
});

export default api;
