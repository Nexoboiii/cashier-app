import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  // build into Spring's static folder
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },

  // dev only — proxy 5173 → 8080
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})