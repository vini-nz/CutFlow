import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api.js'
import { useSession } from '../context/SessionContext.jsx'
import Modal from '../components/Modal.jsx'

const emptyForm = { nome: '', cliente: '' }

export default function Projetos() {
  const { sessao } = useSession()
  const orgAtiva = sessao?.organizacaoAtivaUuid

  const [projetos, setProjetos] = useState([])
  const [compartilhados, setCompartilhados] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [form, setForm] = useState(emptyForm)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  function loadProjetos() {
    setLoading(true)
    api.get('/projetos', { params: { size: 50 } })
      .then((response) => setProjetos(response.data.content))
      .catch(() => setError('Não foi possível carregar os projetos.'))
      .finally(() => setLoading(false))
  }

  function loadCompartilhados() {
    // Independe do workspace ativo (ADR-0006): projetos que outros me passaram.
    api.get('/projetos/compartilhados')
      .then((response) => setCompartilhados(response.data))
      .catch(() => setCompartilhados([]))
  }

  // Recarrega ao trocar de workspace (organização ativa).
  useEffect(() => {
    loadProjetos()
    loadCompartilhados()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgAtiva])

  function openCreateForm() {
    setForm(emptyForm)
    setFormError('')
    setShowForm(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      await api.post('/projetos', form)
      setShowForm(false)
      loadProjetos()
    } catch (err) {
      setFormError(err.response?.data?.message || 'Não foi possível salvar o projeto.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(projeto) {
    if (!confirm(`Remover o projeto "${projeto.nome}"? Isso apaga chapas, peças e planos gerados.`)) return
    try {
      await api.delete(`/projetos/${projeto.uuid}`)
      loadProjetos()
    } catch {
      setError('Não foi possível remover o projeto.')
    }
  }

  return (
    <main className="mx-auto max-w-5xl p-6">
      {showForm && (
        <Modal title="Novo projeto" onClose={() => setShowForm(false)}>
          <form onSubmit={handleSubmit}>
            <label className="mb-1 block text-sm text-gray-700">Nome *</label>
            <input
              required autoFocus value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              placeholder="Ex: Armário Cozinha João"
              className="mb-3 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
            />
            <label className="mb-1 block text-sm text-gray-700">Cliente</label>
            <input
              value={form.cliente}
              onChange={(e) => setForm({ ...form, cliente: e.target.value })}
              className="mb-4 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
            />
            {formError && <p className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{formError}</p>}
            <div className="flex gap-2">
              <button
                type="submit" disabled={saving}
                className="rounded-lg bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
              >
                {saving ? 'Salvando...' : 'Criar projeto'}
              </button>
              <button
                type="button" onClick={() => setShowForm(false)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                Cancelar
              </button>
            </div>
          </form>
        </Modal>
      )}

      <div className="mb-5 flex items-end justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Projetos</h2>
          <p className="text-sm text-gray-500">Plano de corte para marcenaria</p>
        </div>
        <button
          onClick={openCreateForm}
          className="rounded-lg bg-cutflow-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-cutflow-700"
        >
          + Novo projeto
        </button>
      </div>

      {error && <p className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-gray-100 bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
            <tr>
              <th className="px-4 py-3 font-medium">Projeto</th>
              <th className="px-4 py-3 font-medium">Cliente</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={3} className="px-4 py-10 text-center text-gray-400">Carregando...</td></tr>
            )}

            {!loading && projetos.length === 0 && (
              <tr><td colSpan={3} className="px-4 py-10 text-center text-sm text-gray-400">
                Nenhum projeto ainda. Clique em <span className="font-medium text-gray-500">+ Novo projeto</span> para começar.
              </td></tr>
            )}

            {projetos.map((projeto) => (
              <tr key={projeto.uuid} className="border-t border-gray-100 hover:bg-gray-50/60">
                <td className="px-4 py-3">
                  <Link to={`/projetos/${projeto.uuid}`} className="font-medium text-cutflow-700 hover:underline">
                    {projeto.nome}
                  </Link>
                </td>
                <td className="px-4 py-3 text-gray-600">{projeto.cliente || '—'}</td>
                <td className="px-4 py-3 text-right">
                  <button onClick={() => handleDelete(projeto)} className="text-sm text-red-600 hover:underline">
                    Remover
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Compartilhados comigo (ADR-0006): projetos de outras pessoas/organizações */}
      {compartilhados.length > 0 && (
        <div className="mt-10">
          <h2 className="text-lg font-semibold text-gray-900">Compartilhados comigo</h2>
          <p className="mb-3 text-sm text-gray-500">Projetos que alguém compartilhou diretamente com você.</p>
          <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-sm">
              <thead className="border-b border-gray-100 bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Projeto</th>
                  <th className="px-4 py-3 font-medium">Cliente</th>
                  <th className="px-4 py-3 font-medium">Acesso</th>
                </tr>
              </thead>
              <tbody>
                {compartilhados.map((projeto) => (
                  <tr key={projeto.uuid} className="border-t border-gray-100 hover:bg-gray-50/60">
                    <td className="px-4 py-3">
                      <Link to={`/projetos/${projeto.uuid}`} className="font-medium text-cutflow-700 hover:underline">
                        {projeto.nome}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{projeto.cliente || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${
                        projeto.podeEditar ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'
                      }`}>
                        {projeto.podeEditar ? 'editor' : 'somente leitura'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </main>
  )
}
