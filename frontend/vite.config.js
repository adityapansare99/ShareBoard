import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Single source of truth for the backend origin.
// - LOCAL DEV: leave VITE_BACKEND_URL unset (see frontend/.env) and the app
//   calls same-origin /api & /ws, which the proxy below forwards to the
//   default local backend (http://localhost:8080).
// - PROD / non-local: set VITE_BACKEND_URL to point the proxy elsewhere.
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080';
  const wsBackend = backend.replace(/^http/, 'ws');

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true,
        },
        '/ws': {
          target: wsBackend,
          ws: true,
        },
      },
    },
  };
});
