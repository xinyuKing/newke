export const formatCurrency = (value) => {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2
  }).format(Number.isNaN(amount) ? 0 : amount);
};

export const formatDateTime = (value) => {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
};

export const formatStars = (value) => {
  const rating = Math.max(0, Math.min(5, Number(value) || 0));
  return `${"*".repeat(rating)}${"-".repeat(5 - rating)}`;
};

export const truncate = (value, size = 100) => {
  if (!value) return "";
  if (value.length <= size) return value;
  return `${value.slice(0, size).trim()}...`;
};

export const createIdempotencyKey = (prefix = "biz") =>
  `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
