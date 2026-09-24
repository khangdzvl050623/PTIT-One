import { fileURLToPath, URL } from 'node:url'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
      },
    },
  },
  resolve: {
    alias: {
      // Alias kiểu `@/...` — khai báo song song trong tsconfig.app.json (`paths`).
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
