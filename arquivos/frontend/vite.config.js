import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// O front-end chama "/api/..." e o Vite repassa para o back-end Spring Boot (porta 8080).
// Assim funciona igual no computador e no GitHub Codespaces.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': { target: process.env.VITE_API_URL || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
