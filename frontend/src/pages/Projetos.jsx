import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api.js'

const emptyForm = { nome: '', cliente: '' }

export default function Projetos() {
  const [projetos, setProjetos] = useState([])
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

  useEffect(() => {
    loadProjetos()
  }, [])

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
    <div className="min-h-screen bg-gray-50">
      <header className="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div>
          <h1 className="text-lg font-medium text-cutflow-900">CutFlow</h1>
          <p className="mt-1 text-sm text-gray-500">Plano de corte para marcenaria</p>
        </div>
        <button
          onClick={openCreateForm}
          className="rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700"
        >
          Novo projeto
        </button>
      </header>

      <main className="p-6">
        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        {showForm && (
          <form
            onSubmit={handleSubmit}
            className="mb-6 max-w-lg rounded-lg border border-gray-200 bg-white p-5"
          >
            <h2 className="mb-4 text-base font-medium text-gray-900">Novo projeto</h2>

            <label className="mb-1 block text-sm text-gray-700">Nome *</label>
            <input
              required
              autoFocus
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              placeholder="Ex: Armário Cozinha João"
              className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
            />

            <label className="mb-1 block text-sm text-gray-700">Cliente</label>
            <input
              value={form.cliente}
              onChange={(e) => setForm({ ...form, cliente: e.target.value })}
              className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
            />

            {formError && (
              <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{formError}</p>
            )}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={saving}
                className="rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
              >
                {saving ? 'Salvando...' : 'Criar projeto'}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
              >
                Cancelar
              </button>
            </div>
          </form>
        )}

        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-gray-500">
              <tr>
                <th className="px-4 py-3 font-medium">Projeto</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={3} className="px-4 py-6 text-center text-gray-400">Carregando...</td></tr>
              )}

              {!loading && projetos.length === 0 && (
                <tr><td colSpan={3} className="px-4 py-6 text-center text-gray-400">Nenhum projeto cadastrado.</td></tr>
              )}

              {projetos.map((projeto) => (
                <tr key={projeto.uuid} className="border-t border-gray-100">
                  <td className="px-4 py-3">
                    <Link to={`/projetos/${projeto.uuid}`} className="font-medium text-cutflow-700 hover:underline">
                      {projeto.nome}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{projeto.cliente || '—'}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleDelete(projeto)}
                      className="text-red-600 hover:underline"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  )
}
