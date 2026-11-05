import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiTarget = process.env.CAMPUSSIGNAL_API_TARGET || 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
})
