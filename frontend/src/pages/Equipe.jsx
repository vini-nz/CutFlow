import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { useSession } from '../context/SessionContext.jsx'

const PAPEIS = { OWNER: 'Dono', ADMIN: 'Administrador', MEMBRO: 'Membro' }

/**
 * Gestão da equipe da organização ativa (ADR-0005). Qualquer membro vê a
 * lista; adicionar/remover exige OWNER/ADMIN (a UI de gestão só aparece para
 * eles, e o backend também bloqueia). Só é possível adicionar quem já tem
 * conta no CutFlow — convite por e-mail fica como melhoria futura.
 */
export default function Equipe() {
  const { sessao } = useSession()
  const orgUuid = sessao?.organizacaoAtivaUuid
  const ativa = sessao?.organizacoes.find((o) => o.uuid === orgUuid)
  const podeGerenciar = ativa && (ativa.papel === 'OWNER' || ativa.papel === 'ADMIN')

  const [membros, setMembros] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [form, setForm] = useState({ email: '', papel: 'MEMBRO' })
  const [formErro, setFormErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const carregar = useCallback(() => {
    if (!orgUuid) return
    setCarregando(true)
    api.get(`/organizacoes/${orgUuid}/membros`)
      .then((res) => setMembros(res.data))
      .catch(() => setErro('Não foi possível carregar a equipe.'))
      .finally(() => setCarregando(false))
  }, [orgUuid])

  useEffect(() => { carregar() }, [carregar])

  async function adicionar(e) {
    e.preventDefault()
    setSalvando(true)
    setFormErro('')
    try {
      await api.post(`/organizacoes/${orgUuid}/membros`, form)
      setForm({ email: '', papel: 'MEMBRO' })
      carregar()
    } catch (err) {
      setFormErro(err.response?.data?.message || 'Não foi possível adicionar.')
    } finally {
      setSalvando(false)
    }
  }

  async function remover(membro) {
    if (!confirm(`Remover ${membro.nome} da equipe?`)) return
    try {
      await api.delete(`/organizacoes/${orgUuid}/membros/${membro.uuid}`)
      carregar()
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível remover.')
    }
  }

  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-lg font-medium text-gray-900">Equipe — {ativa?.nome}</h1>
      <p className="mb-4 text-sm text-gray-500">
        Quem pode acessar os projetos desta organização.
      </p>

      {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

      {podeGerenciar && (
        <form onSubmit={adicionar} className="mb-5 flex flex-wrap items-end gap-2 rounded-lg border border-gray-200 bg-white p-4">
          <div className="flex-1">
            <label className="mb-1 block text-xs text-gray-600">E-mail de quem já tem conta</label>
            <input
              type="email" required value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="pessoa@email.com"
              className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-600">Papel</label>
            <select
              value={form.papel}
              onChange={(e) => setForm({ ...form, papel: e.target.value })}
              className="rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none"
            >
              <option value="MEMBRO">Membro</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </div>
          <button
            type="submit" disabled={salvando}
            className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
          >
            {salvando ? 'Adicionando...' : 'Adicionar'}
          </button>
          {formErro && <p className="w-full rounded bg-red-50 px-3 py-2 text-sm text-red-700">{formErro}</p>}
        </form>
      )}

      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr>
              <th className="px-4 py-2 font-medium">Nome</th>
              <th className="px-4 py-2 font-medium">E-mail</th>
              <th className="px-4 py-2 font-medium">Papel</th>
              <th className="px-4 py-2 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {carregando && <tr><td colSpan={4} className="px-4 py-4 text-center text-gray-400">Carregando...</td></tr>}
            {!carregando && membros.map((m) => (
              <tr key={m.uuid} className="border-t border-gray-100">
                <td className="px-4 py-2 text-gray-900">{m.nome}</td>
                <td className="px-4 py-2 text-gray-600">{m.email}</td>
                <td className="px-4 py-2 text-gray-600">{PAPEIS[m.papel] || m.papel}</td>
                <td className="px-4 py-2 text-right">
                  {podeGerenciar && m.papel !== 'OWNER' && (
                    <button onClick={() => remover(m)} className="text-xs text-red-600 hover:underline">Remover</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  )
}
