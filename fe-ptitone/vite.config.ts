import { fileURLToPath, URL } from 'node:url'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Alias kiểu `@/...` — khai báo song song trong tsconfig.app.json (`paths`).
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
