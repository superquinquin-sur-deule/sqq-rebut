import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Quarkus runs on :8080 — proxy the API and OpenAPI endpoints to it in dev.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/q': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
