import { Navigate } from 'react-router-dom'
import { useSession } from '../context/SessionContext.jsx'

/**
 * Exige apenas login (sem exigir organização ativa) - usado no onboarding,
 * onde o usuário logado ainda vai criar/entrar na primeira organização.
 */
export default function RequireAuth({ children }) {
  const { sessao, carregando } = useSession()

  if (carregando) {
    return <div className="p-6 text-sm text-gray-400">Carregando...</div>
  }
  if (!sessao) {
    return <Navigate to="/login" replace />
  }
  return children
}
