import path from "path"
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      '/api/admin': 'http://localhost:8080',
      '/api/v1': 'http://localhost:8081',
      '/login': 'http://localhost:8080'
    }
  }
})
