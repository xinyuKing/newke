# Newke Unified Frontend

## Location

The frontend now lives at:

- `apps/frontend`

## Structure

```text
src
|-- modules
|   |-- forum
|   |-- mall
|   `-- shared
|-- router
|-- stores
|-- assets
|-- App.vue
`-- main.js
```

## Environment

Copy from `apps/frontend/.env.example` when you need to override local proxy targets.

- `VITE_GATEWAY_PROXY_TARGET`: default proxy target for the unified gateway.
- `VITE_FORUM_PROXY_TARGET`: optional forum-only override.
- `VITE_MALL_PROXY_TARGET`: optional mall-only override.
- `VITE_MALL_API_BASE`: mall API base path, defaults to `/api`.

## Setup

```bash
cd apps/frontend
npm install
npm run dev
```

Default dev server runs at `http://localhost:5173`.

## Proxy Strategy

- `/community/**` -> `http://localhost:8080`
- `/api/**` -> `http://localhost:8080/api/**`

That keeps the mall frontend on the same `/api/**` path in both development and production, instead of relying on a dev-only rewrite path.

## Build

```bash
cd apps/frontend
npm run build
npm run preview
```
