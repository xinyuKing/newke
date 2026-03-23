<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div>
        <div class="footer-title">Community</div>
        <div class="footer-sub">Built for thoughtful dialogue, powered by Vue 3.</div>
      </div>
      <div class="footer-links">
        <span>About</span>
        <span>Join</span>
        <span>Feedback</span>
        <span>Terms</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 60px;
  padding: 32px 0;
  border-top: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.6);
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: var(--muted);
}

.footer-title {
  font-family: "Space Grotesk", sans-serif;
  font-weight: 600;
  color: var(--ink);
}

.footer-links {
  display: flex;
  gap: 16px;
}

@media (max-width: 768px) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
