import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../services/api.js'
import { useSession } from '../context/SessionContext.jsx'

const PAPEL_LABEL = { EDITOR: 'editar', VISUALIZADOR: 'visualizar' }

/**
 * Tela pública de aceite de convite (ADR-0006) - /convite/:token. Mostra
 * "Fulano te convidou para o projeto X" mesmo sem login; se não estiver
 * logado, leva ao login/cadastro voltando para cá; se estiver, aceita e abre
 * o projeto.
 */
export default function Convite() {
  const { token } = useParams()
  const { sessao, carregando: carregandoSessao, recarregar } = useSession()
  const navigate = useNavigate()

  const [detalhes, setDetalhes] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [aceitando, setAceitando] = useState(false)

  useEffect(() => {
    api.get(`/convites/${token}`)
      .then((res) => setDetalhes(res.data))
      .catch(() => setErro('Convite não encontrado.'))
      .finally(() => setCarregando(false))
  }, [token])

  async function aceitar() {
    setAceitando(true)
    setErro('')
    try {
      const { data } = await api.post(`/convites/${token}/aceitar`)
      await recarregar() // o projeto passa a aparecer em "Compartilhados comigo"
      navigate(`/projetos/${data.projetoUuid}`)
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível aceitar o convite.')
    } finally {
      setAceitando(false)
    }
  }

  const proximo = encodeURIComponent(`/convite/${token}`)

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-cutflow-900">Convite</h1>

        {carregando || carregandoSessao ? (
          <p className="mt-4 text-sm text-gray-400">Carregando...</p>
        ) : erro && !detalhes ? (
          <p className="mt-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>
        ) : !detalhes.valido ? (
          <p className="mt-4 text-sm text-gray-600">Este convite não é mais válido (usado ou desativado).</p>
        ) : (
          <>
            <p className="mt-4 text-sm text-gray-700">
              <strong>{detalhes.convidadoPor}</strong> convidou você para{' '}
              <strong>{PAPEL_LABEL[detalhes.papel]}</strong> o projeto{' '}
              <strong>{detalhes.nomeProjeto}</strong>.
            </p>

            {erro && <p className="mt-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

            {sessao ? (
              <button
                onClick={aceitar} disabled={aceitando}
                className="mt-5 w-full rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
              >
                {aceitando ? 'Entrando no projeto...' : 'Aceitar convite'}
              </button>
            ) : (
              <div className="mt-5 space-y-2">
                <p className="text-xs text-gray-500">Entre ou crie uma conta para acessar o projeto.</p>
                <Link
                  to={`/login?next=${proximo}`}
                  className="block w-full rounded bg-cutflow-600 px-4 py-2 text-center text-sm font-medium text-white hover:bg-cutflow-700"
                >
                  Entrar
                </Link>
                <Link
                  to={`/registro?next=${proximo}`}
                  className="block w-full rounded border border-gray-300 px-4 py-2 text-center text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  Criar conta
                </Link>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
