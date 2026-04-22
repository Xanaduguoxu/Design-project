import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  },
  server: {
    port: 8081,
    proxy: {
      '/v1': {
        target: 'http://localhost:9099',
        changeOrigin: true,
        ws: false,
        secure: false
      }
    }
  }
})