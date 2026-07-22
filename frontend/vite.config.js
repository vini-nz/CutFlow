import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Login por sessão exige que a SPA e a API sejam vistas como MESMA ORIGEM pelo
// navegador (cookies SameSite). Por isso o dev server faz proxy de /api, /oauth2
// e do callback OAuth para o backend, em vez de a SPA chamar o backend
// cross-origin. Em produção, sirva frontend e backend atrás do mesmo domínio
// (reverse proxy) para o mesmo efeito.
const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

const proxyOpts = { target: proxyTarget, changeOrigin: true, xfwd: true }

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': proxyOpts,
      '/oauth2': proxyOpts,          // início do login Google
      '/login/oauth2': proxyOpts     // callback do Google (não colide com a rota /login da SPA)
    }
  }
})
