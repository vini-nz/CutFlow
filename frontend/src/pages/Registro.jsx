import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useSession } from '../context/SessionContext.jsx'

export default function Registro() {
  const { sessao, carregando, registrar, googleHabilitado } = useSession()
  const navigate = useNavigate()

  const [form, setForm] = useState({ nome: '', email: '', senha: '' })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (!carregando && sessao) return <Navigate to="/" replace />

  async function handleSubmit(e) {
    e.preventDefault()
    setEnviando(true)
    setErro('')
    try {
      await registrar(form.nome, form.email, form.senha)
      navigate('/') // ProtectedLayout leva ao onboarding se ainda não há organização
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível criar a conta.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-cutflow-900">Criar conta</h1>
        <p className="mb-6 text-sm text-gray-500">Comece a organizar seus planos de corte.</p>

        <form onSubmit={handleSubmit}>
          <label className="mb-1 block text-sm text-gray-700">Nome</label>
          <input
            required autoFocus value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />
          <label className="mb-1 block text-sm text-gray-700">E-mail</label>
          <input
            type="email" required value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />
          <label className="mb-1 block text-sm text-gray-700">Senha</label>
          <input
            type="password" required minLength={8} value={form.senha}
            onChange={(e) => setForm({ ...form, senha: e.target.value })}
            className="mb-1 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />
          <p className="mb-4 text-xs text-gray-400">Ao menos 8 caracteres.</p>

          {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
          >
            {enviando ? 'Criando...' : 'Criar conta'}
          </button>
        </form>

        {googleHabilitado && (
          <>
            <div className="my-4 flex items-center gap-3 text-xs text-gray-400">
              <span className="h-px flex-1 bg-gray-200" /> ou <span className="h-px flex-1 bg-gray-200" />
            </div>
            <a
              href="/oauth2/authorization/google"
              className="block w-full rounded border border-gray-300 px-4 py-2 text-center text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Continuar com Google
            </a>
          </>
        )}

        <p className="mt-6 text-center text-sm text-gray-500">
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-cutflow-700 hover:underline">Entrar</Link>
        </p>
      </div>
    </div>
  )
}
