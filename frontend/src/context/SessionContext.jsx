import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import api from '../services/api.js'

/**
 * Estado global de sessão (ADR-0005): quem está logado, de quais organizações
 * é membro e qual está ativa. Carrega /auth/me ao montar; expõe login,
 * cadastro, logout e troca de workspace.
 */
const SessionContext = createContext(null)

export function SessionProvider({ children }) {
  const [sessao, setSessao] = useState(null)
  const [googleHabilitado, setGoogleHabilitado] = useState(false)
  const [carregando, setCarregando] = useState(true)

  const carregarSessao = useCallback(async () => {
    try {
      // Garante o cookie XSRF-TOKEN antes de qualquer POST (login/cadastro).
      await api.get('/auth/csrf')
      try {
        const { data } = await api.get('/auth/config')
        setGoogleHabilitado(Boolean(data.googleHabilitado))
      } catch { /* config pública falhou: mantém Google oculto */ }
      const { data } = await api.get('/auth/me')
      setSessao(data)
    } catch {
      setSessao(null)
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    carregarSessao()
  }, [carregarSessao])

  const login = useCallback(async (email, senha) => {
    await api.get('/auth/csrf') // garante um XSRF-TOKEN fresco antes do POST
    const { data } = await api.post('/auth/login', { email, senha })
    setSessao(data)
    return data
  }, [])

  const registrar = useCallback(async (nome, email, senha) => {
    await api.get('/auth/csrf') // idem: token fresco antes de cadastrar
    await api.post('/auth/register', { nome, email, senha })
    // Cadastro não abre sessão; loga em seguida para o usuário já entrar.
    const { data } = await api.post('/auth/login', { email, senha })
    setSessao(data)
    return data
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      /* mesmo se falhar, limpa o estado local */
    }
    setSessao(null)
    // O logout do Spring Security também limpa o cookie CSRF. Sem renovar,
    // um login/cadastro seguinte (na mesma aba, sem recarregar) iria sem token
    // e falharia. Busca um XSRF-TOKEN novo para a próxima ação funcionar.
    try {
      await api.get('/auth/csrf')
    } catch { /* será renovado no próximo carregamento se falhar aqui */ }
  }, [])

  const trocarOrganizacao = useCallback(async (uuid) => {
    await api.post(`/organizacoes/${uuid}/ativar`)
    await carregarSessao()
  }, [carregarSessao])

  const value = {
    sessao, carregando, googleHabilitado,
    login, registrar, logout, trocarOrganizacao, recarregar: carregarSessao
  }
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession() {
  const ctx = useContext(SessionContext)
  if (!ctx) throw new Error('useSession precisa estar dentro de <SessionProvider>')
  return ctx
}
