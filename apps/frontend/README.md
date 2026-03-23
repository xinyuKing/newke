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

## Setup

```bash
cd apps/frontend
npm install
npm run dev
```

Default dev server runs at `http://localhost:5173`.

## Proxy Strategy

- `/community/**` -> `http://localhost:8080`
- `/mall-api/**` -> `http://localhost:8080/api/**`

That means frontend development now targets the unified gateway instead of treating forum and mall as two separate browser-side backends.

## Build

```bash
cd apps/frontend
npm run build
npm run preview
```
