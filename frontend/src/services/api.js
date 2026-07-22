import axios from 'axios'

// Mesma origem via proxy (ver vite.config.js): a base é relativa por padrão.
// withCredentials envia o cookie de sessão; os nomes de cookie/header XSRF
// batem com o CookieCsrfTokenRepository do backend (ADR-0005).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

// Se a sessão expirar durante o uso, um 401 numa chamada de dados manda de
// volta ao login. As sondagens de auth (me/login/config/csrf) são ignoradas
// para não criar loop quando o usuário simplesmente ainda não está logado.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    const ehSondagemAuth = /\/auth\/(me|login|config|csrf)/.test(url)
    const naTelaDeLogin = window.location.pathname.startsWith('/login')
      || window.location.pathname.startsWith('/registro')
    if (error.response?.status === 401 && !ehSondagemAuth && !naTelaDeLogin) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
