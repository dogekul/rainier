/// <reference types="vitest" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        headers: {
          origin: 'http://localhost:5173',
        },
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            // Browser-origin CORS is already terminated at Vite. Rewrite arbitrary dev origins
            // such as http://127.0.0.1:5174 to the backend's configured dev allowlist origin.
            proxyReq.setHeader('origin', 'http://localhost:5173');
            proxyReq.setHeader('Origin', 'http://localhost:5173');
          });
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
});
