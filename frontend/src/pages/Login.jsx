import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useSession } from '../context/SessionContext.jsx'

export default function Login() {
  const { sessao, carregando, login, googleHabilitado } = useSession()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(searchParams.get('erro') === 'google' ? 'Não foi possível entrar com o Google.' : '')
  const [enviando, setEnviando] = useState(false)

  // Volta para onde o usuário estava (ex: /convite/:token), padrão a home.
  const proximo = searchParams.get('next') || '/'

  if (!carregando && sessao) return <Navigate to={proximo} replace />

  async function handleSubmit(e) {
    e.preventDefault()
    setEnviando(true)
    setErro('')
    try {
      await login(email, senha)
      navigate(proximo)
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível entrar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-cutflow-900">CutFlow</h1>
        <p className="mb-6 text-sm text-gray-500">Entre para acessar seus planos de corte.</p>

        <form onSubmit={handleSubmit}>
          <label className="mb-1 block text-sm text-gray-700">E-mail</label>
          <input
            type="email" required autoFocus value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />
          <label className="mb-1 block text-sm text-gray-700">Senha</label>
          <input
            type="password" required value={senha}
            onChange={(e) => setSenha(e.target.value)}
            className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />

          {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
          >
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        {googleHabilitado && (
          <>
            <div className="my-4 flex items-center gap-3 text-xs text-gray-400">
              <span className="h-px flex-1 bg-gray-200" /> ou <span className="h-px flex-1 bg-gray-200" />
            </div>
            {/* Navegação de página inteira (não XHR): inicia o fluxo OAuth no backend. */}
            <a
              href="/oauth2/authorization/google"
              className="block w-full rounded border border-gray-300 px-4 py-2 text-center text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Entrar com Google
            </a>
          </>
        )}

        <p className="mt-6 text-center text-sm text-gray-500">
          Não tem conta?{' '}
          <Link
            to={proximo !== '/' ? `/registro?next=${encodeURIComponent(proximo)}` : '/registro'}
            className="font-medium text-cutflow-700 hover:underline"
          >
            Criar conta
          </Link>
        </p>
      </div>
    </div>
  )
}
