import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the page calls /api, /actuator and /ws on its own address and Vite forwards them to the
// Spring Boot backend on port 8080, so no CORS setup is needed locally or in Codespaces.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Codespaces opens the page on https://<name>-5173.app.github.dev
    allowedHosts: ['.app.github.dev'],
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
});
