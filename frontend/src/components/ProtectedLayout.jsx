import { Navigate, Outlet } from 'react-router-dom'
import { useSession } from '../context/SessionContext.jsx'
import AppHeader from './AppHeader.jsx'

/**
 * Guarda das telas do app (ADR-0005): exige usuário logado E organização
 * ativa. Sem login vai para /login; logado mas sem organização vai para o
 * onboarding. Renderiza a barra superior comum + a rota filha.
 */
export default function ProtectedLayout() {
  const { sessao, carregando } = useSession()

  if (carregando) {
    return <div className="p-6 text-sm text-gray-400">Carregando...</div>
  }
  if (!sessao) {
    return <Navigate to="/login" replace />
  }
  if (!sessao.organizacaoAtivaUuid) {
    return <Navigate to="/onboarding" replace />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <AppHeader />
      <Outlet />
    </div>
  )
}
