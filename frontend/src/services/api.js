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

// Anexa o token CSRF manualmente a partir do cookie XSRF-TOKEN. O axios só faz
// isso automaticamente em requisições de MESMA ORIGEM; se VITE_API_URL apontar
// para o backend em outra origem (ex: http://localhost:8080), ele omitiria o
// header e todo POST/PUT/DELETE tomaria 403 de CSRF. Sempre mandamos o token
// só para a NOSSA API, então fazer isso manualmente é seguro e evita esse tropeço.
api.interceptors.request.use((config) => {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  if (m) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(m[1])
  }
  return config
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
