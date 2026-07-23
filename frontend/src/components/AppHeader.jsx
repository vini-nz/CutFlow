import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSession } from '../context/SessionContext.jsx'
import ModalPerfil from './ModalPerfil.jsx'
import ModalOrganizacao from './ModalOrganizacao.jsx'

/**
 * Barra superior das telas autenticadas: marca, seletor de workspace
 * (organização ativa), gestão de equipe/organização (OWNER/ADMIN), perfil do
 * usuário e logout.
 */
export default function AppHeader() {
  const { sessao, logout, trocarOrganizacao } = useSession()
  const navigate = useNavigate()
  const [showPerfil, setShowPerfil] = useState(false)
  const [showOrg, setShowOrg] = useState(false)

  if (!sessao) return null

  const ativa = sessao.organizacoes.find((o) => o.uuid === sessao.organizacaoAtivaUuid)
  // Gestão de equipe/organização só faz sentido numa organização de verdade —
  // o espaço pessoal (ADR-0006) é de uma pessoa só.
  const podeGerenciar = ativa && !ativa.pessoal && (ativa.papel === 'OWNER' || ativa.papel === 'ADMIN')

  async function handleTroca(e) {
    const uuid = e.target.value
    if (uuid === sessao.organizacaoAtivaUuid) return
    await trocarOrganizacao(uuid)
    navigate('/')
  }

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 bg-white px-6 py-3">
      {showPerfil && <ModalPerfil onClose={() => setShowPerfil(false)} />}
      {showOrg && ativa && <ModalOrganizacao organizacao={ativa} onClose={() => setShowOrg(false)} />}

      <div className="flex items-center gap-3">
        <Link to="/" className="text-lg font-semibold text-cutflow-900">CutFlow</Link>
        {sessao.organizacoes.length > 0 && (
          <select
            value={sessao.organizacaoAtivaUuid || ''}
            onChange={handleTroca}
            className="rounded border border-gray-300 px-2 py-1.5 text-sm text-gray-700 focus:border-cutflow-600 focus:outline-none"
            title="Trocar de marcenaria/workspace"
          >
            {sessao.organizacoes.map((o) => (
              <option key={o.uuid} value={o.uuid}>{o.nome}</option>
            ))}
          </select>
        )}
        {podeGerenciar && (
          <button
            onClick={() => setShowOrg(true)}
            title="Editar organização"
            className="text-sm text-gray-400 hover:text-cutflow-700"
          >
            ⚙
          </button>
        )}
        <Link to="/onboarding" className="text-xs text-gray-500 hover:text-cutflow-700">
          + Nova organização
        </Link>
      </div>

      <div className="flex items-center gap-3 text-sm">
        {podeGerenciar && (
          <Link to="/equipe" className="text-gray-600 hover:text-cutflow-700">Equipe</Link>
        )}
        <button
          onClick={() => setShowPerfil(true)}
          className="hidden text-gray-500 hover:text-cutflow-700 hover:underline sm:inline"
          title="Meu perfil"
        >
          {sessao.usuario.email}
        </button>
        <button
          onClick={handleLogout}
          className="rounded border border-gray-300 px-3 py-1.5 text-gray-700 hover:bg-gray-100"
        >
          Sair
        </button>
      </div>
    </header>
  )
}
