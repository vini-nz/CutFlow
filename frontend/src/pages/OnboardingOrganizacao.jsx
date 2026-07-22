import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api.js'
import { useSession } from '../context/SessionContext.jsx'

/**
 * Criação da (primeira ou de mais uma) organização. Serve tanto para o
 * onboarding de quem acabou de se cadastrar quanto para "+ Nova organização"
 * na barra superior. Ao criar, a nova organização já vira o workspace ativo.
 */
export default function OnboardingOrganizacao() {
  const { sessao, recarregar, logout } = useSession()
  const navigate = useNavigate()

  const [form, setForm] = useState({ nome: '', documento: '' })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const primeiraVez = sessao && sessao.organizacoes.length === 0

  async function handleSubmit(e) {
    e.preventDefault()
    setEnviando(true)
    setErro('')
    try {
      await api.post('/organizacoes', {
        nome: form.nome,
        documento: form.documento || null
      })
      await recarregar() // atualiza a lista e a organização ativa
      navigate('/')
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível criar a organização.')
    } finally {
      setEnviando(false)
    }
  }

  async function handleSair() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-cutflow-900">
          {primeiraVez ? 'Crie sua marcenaria' : 'Nova organização'}
        </h1>
        <p className="mb-6 text-sm text-gray-500">
          {primeiraVez
            ? 'Seus projetos ficam dentro de uma organização. Crie a sua para começar.'
            : 'Crie outra organização — você poderá alternar entre elas pelo seletor no topo.'}
        </p>

        <form onSubmit={handleSubmit}>
          <label className="mb-1 block text-sm text-gray-700">Nome da marcenaria</label>
          <input
            required autoFocus value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            placeholder="Ex: Marcenaria do João"
            className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />
          <label className="mb-1 block text-sm text-gray-700">CNPJ (opcional)</label>
          <input
            value={form.documento}
            onChange={(e) => setForm({ ...form, documento: e.target.value })}
            className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
          />

          {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
          >
            {enviando ? 'Criando...' : 'Criar organização'}
          </button>
        </form>

        <div className="mt-4 flex justify-between text-xs">
          {!primeiraVez
            ? <button onClick={() => navigate('/')} className="text-gray-500 hover:text-cutflow-700">Voltar</button>
            : <span />}
          <button onClick={handleSair} className="text-gray-400 hover:text-gray-600">Sair</button>
        </div>
      </div>
    </div>
  )
}
