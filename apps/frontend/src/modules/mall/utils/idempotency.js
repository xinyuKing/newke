const STORAGE_PREFIX = "mall:idempotency";
const PAYLOAD_SUFFIX = "payload";

const isSessionStorageAvailable = () => typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";

const normalizePart = (value) => {
  if (value === null || value === undefined || value === "") {
    return "default";
  }
  return String(value);
};

const buildStorageKey = (scope, owner, subject) =>
  [STORAGE_PREFIX, normalizePart(scope), normalizePart(owner), normalizePart(subject)].join(":");

const buildPayloadStorageKey = (scope, owner, subject) =>
  [buildStorageKey(scope, owner, subject), PAYLOAD_SUFFIX].join(":");

export const getPendingIdempotencyKey = ({ scope, owner, subject }) => {
  if (!isSessionStorageAvailable()) return "";
  return window.sessionStorage.getItem(buildStorageKey(scope, owner, subject)) || "";
};

export const setPendingIdempotencyKey = ({ scope, owner, subject }, value) => {
  if (!isSessionStorageAvailable()) return;
  const storageKey = buildStorageKey(scope, owner, subject);
  if (!value) {
    window.sessionStorage.removeItem(storageKey);
    return;
  }
  window.sessionStorage.setItem(storageKey, value);
};

export const clearPendingIdempotencyKey = ({ scope, owner, subject }) => {
  setPendingIdempotencyKey({ scope, owner, subject }, "");
};

export const getPendingIdempotencyPayload = ({ scope, owner, subject }) => {
  if (!isSessionStorageAvailable()) return null;
  const rawValue = window.sessionStorage.getItem(buildPayloadStorageKey(scope, owner, subject));
  if (!rawValue) return null;
  try {
    return JSON.parse(rawValue);
  } catch {
    return null;
  }
};

export const setPendingIdempotencyPayload = ({ scope, owner, subject }, value) => {
  if (!isSessionStorageAvailable()) return;
  const storageKey = buildPayloadStorageKey(scope, owner, subject);
  if (!value) {
    window.sessionStorage.removeItem(storageKey);
    return;
  }
  window.sessionStorage.setItem(storageKey, JSON.stringify(value));
};

export const clearPendingIdempotencyPayload = ({ scope, owner, subject }) => {
  setPendingIdempotencyPayload({ scope, owner, subject }, null);
};

export const isAmbiguousMallFailure = (response) => {
  const status = Number(response?.status || 0);
  return status === 0 || status >= 500;
};

export const resolveIdempotentRequestFailure = (response, fallbackMessage, ambiguousMessage) => {
  const ambiguous = isAmbiguousMallFailure(response);
  return {
    ambiguous,
    message: ambiguous ? ambiguousMessage : response?.data?.message || fallbackMessage
  };
};
