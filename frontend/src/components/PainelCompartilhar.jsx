import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'

const PAPEL_LABEL = { EDITOR: 'editor', VISUALIZADOR: 'visualizador' }

/**
 * Painel de compartilhamento de UM projeto (ADR-0006). Gera link reutilizável
 * (estilo Canva) ou convite por e-mail, lista convites ativos e as pessoas com
 * acesso. Só é aberto por quem tem acesso de edição ao projeto.
 */
export default function PainelCompartilhar({ projetoUuid, onClose }) {
  const [colaboradores, setColaboradores] = useState([])
  const [convites, setConvites] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [papel, setPapel] = useState('EDITOR')
  const [email, setEmail] = useState('')
  const [gerando, setGerando] = useState(false)
  const [copiado, setCopiado] = useState('')

  const carregar = useCallback(() => {
    setCarregando(true)
    Promise.all([
      api.get(`/projetos/${projetoUuid}/colaboradores`),
      api.get(`/projetos/${projetoUuid}/convites`)
    ])
      .then(([colabRes, convRes]) => {
        setColaboradores(colabRes.data)
        setConvites(convRes.data)
      })
      .catch(() => setErro('Não foi possível carregar o compartilhamento.'))
      .finally(() => setCarregando(false))
  }, [projetoUuid])

  useEffect(() => { carregar() }, [carregar])

  async function gerar(e) {
    e.preventDefault()
    setGerando(true)
    setErro('')
    try {
      const payload = { papel, email: email.trim() || null }
      await api.post(`/projetos/${projetoUuid}/convites`, payload)
      setEmail('')
      carregar()
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível gerar o convite.')
    } finally {
      setGerando(false)
    }
  }

  async function revogar(convite) {
    if (!confirm('Desativar este convite/link?')) return
    await api.delete(`/projetos/${projetoUuid}/convites/${convite.uuid}`)
    carregar()
  }

  async function remover(colaborador) {
    if (!confirm(`Remover ${colaborador.nome} do projeto?`)) return
    await api.delete(`/projetos/${projetoUuid}/colaboradores/${colaborador.uuid}`)
    carregar()
  }

  async function copiar(url) {
    try {
      await navigator.clipboard.writeText(url)
      setCopiado(url)
      setTimeout(() => setCopiado(''), 1500)
    } catch { /* clipboard indisponível: usuário copia manualmente */ }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-black/40 p-4" onClick={onClose}>
      <div className="mt-10 w-full max-w-lg rounded-xl bg-white p-6 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-cutflow-900">Compartilhar projeto</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700">✕</button>
        </div>

        {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

        <form onSubmit={gerar} className="mb-5 rounded-lg border border-gray-200 p-4">
          <p className="mb-2 text-sm font-medium text-gray-800">Convidar alguém</p>
          <div className="mb-2 flex flex-wrap items-end gap-2">
            <div className="flex-1">
              <label className="mb-1 block text-xs text-gray-600">E-mail (opcional)</label>
              <input
                type="email" value={email} placeholder="deixe vazio para gerar um link"
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-600">Acesso</label>
              <select
                value={papel} onChange={(e) => setPapel(e.target.value)}
                className="rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none"
              >
                <option value="EDITOR">Editor</option>
                <option value="VISUALIZADOR">Visualizador</option>
              </select>
            </div>
            <button
              type="submit" disabled={gerando}
              className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
            >
              {gerando ? '...' : (email.trim() ? 'Convidar' : 'Gerar link')}
            </button>
          </div>
          <p className="text-[11px] text-gray-400">
            Com e-mail: convite pessoal (uso único). Sem e-mail: link reutilizável — qualquer pessoa
            com o link entra (estilo Canva).
          </p>
        </form>

        {carregando ? (
          <p className="text-sm text-gray-400">Carregando...</p>
        ) : (
          <>
            <div className="mb-5">
              <p className="mb-2 text-sm font-medium text-gray-800">Convites e links ativos</p>
              {convites.length === 0 && <p className="text-sm text-gray-400">Nenhum convite ativo.</p>}
              <div className="space-y-2">
                {convites.map((c) => (
                  <div key={c.uuid} className="flex items-center justify-between gap-2 rounded border border-gray-200 p-2 text-sm">
                    <div className="min-w-0">
                      <p className="truncate text-gray-800">
                        {c.reutilizavel ? '🔗 Link' : `✉️ ${c.emailAlvo}`}{' '}
                        <span className="text-xs text-gray-400">({PAPEL_LABEL[c.papel]})</span>
                      </p>
                      <p className="truncate text-[11px] text-gray-400">{c.urlConvite}</p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                      <button onClick={() => copiar(c.urlConvite)} className="text-xs text-cutflow-700 hover:underline">
                        {copiado === c.urlConvite ? 'Copiado!' : 'Copiar'}
                      </button>
                      <button onClick={() => revogar(c)} className="text-xs text-red-600 hover:underline">Desativar</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-medium text-gray-800">Pessoas com acesso</p>
              {colaboradores.length === 0 && <p className="text-sm text-gray-400">Ninguém além da sua organização.</p>}
              <div className="space-y-2">
                {colaboradores.map((c) => (
                  <div key={c.uuid} className="flex items-center justify-between gap-2 rounded border border-gray-200 p-2 text-sm">
                    <div className="min-w-0">
                      <p className="truncate text-gray-800">{c.nome}</p>
                      <p className="truncate text-[11px] text-gray-400">{c.email} · {PAPEL_LABEL[c.papel]}</p>
                    </div>
                    <button onClick={() => remover(c)} className="text-xs text-red-600 hover:underline">Remover</button>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
