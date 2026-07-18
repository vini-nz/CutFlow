import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../services/api.js'
import CanvasChapa from '../components/CanvasChapa.jsx'

const ESPESSURAS = [6, 15, 18, 25]

const emptyChapaForm = { larguraMm: 1840, alturaMm: 2740, espessuraMm: 15, quantidadeDisponivel: 5, kerfMm: 4, margemBordaMm: 6 }
const emptyPecaForm = { nome: '', alturaMm: '', larguraMm: '', espessuraMm: 15, quantidade: 1, tipoAcabamento: 'LISO' }

export default function ProjetoDetalhe() {
  const { uuid } = useParams()

  const [projeto, setProjeto] = useState(null)
  const [chapas, setChapas] = useState([])
  const [pecas, setPecas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [chapaForm, setChapaForm] = useState(emptyChapaForm)
  const [showChapaForm, setShowChapaForm] = useState(false)
  const [chapaFormError, setChapaFormError] = useState('')
  const [savingChapa, setSavingChapa] = useState(false)

  const [pecaForm, setPecaForm] = useState(emptyPecaForm)
  const [showPecaForm, setShowPecaForm] = useState(false)
  const [pecaFormError, setPecaFormError] = useState('')
  const [savingPeca, setSavingPeca] = useState(false)

  const [plano, setPlano] = useState(null)
  const [planoError, setPlanoError] = useState('')
  const [gerando, setGerando] = useState(false)
  const [exportando, setExportando] = useState(false)
  const [chapaSelecionada, setChapaSelecionada] = useState(0)

  function loadTudo() {
    setLoading(true)
    Promise.all([
      api.get(`/projetos/${uuid}`),
      api.get(`/projetos/${uuid}/chapas`),
      api.get(`/projetos/${uuid}/pecas`)
    ])
      .then(([projetoRes, chapasRes, pecasRes]) => {
        setProjeto(projetoRes.data)
        setChapas(chapasRes.data)
        setPecas(pecasRes.data)
      })
      .catch(() => setError('Não foi possível carregar o projeto.'))
      .finally(() => setLoading(false))

    api.get(`/projetos/${uuid}/plano-de-corte`)
      .then((res) => setPlano(res.data))
      .catch(() => setPlano(null))
  }

  useEffect(() => {
    loadTudo()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uuid])

  async function handleChapaSubmit(e) {
    e.preventDefault()
    setSavingChapa(true)
    setChapaFormError('')
    try {
      await api.post(`/projetos/${uuid}/chapas`, chapaForm)
      setShowChapaForm(false)
      setChapaForm(emptyChapaForm)
      loadTudo()
    } catch (err) {
      setChapaFormError(err.response?.data?.message || 'Não foi possível salvar a chapa.')
    } finally {
      setSavingChapa(false)
    }
  }

  async function handleDeleteChapa(chapa) {
    if (!confirm(`Remover a chapa de ${chapa.espessuraMm}mm?`)) return
    await api.delete(`/projetos/${uuid}/chapas/${chapa.uuid}`)
    loadTudo()
  }

  async function handlePecaSubmit(e) {
    e.preventDefault()
    setSavingPeca(true)
    setPecaFormError('')
    try {
      await api.post(`/projetos/${uuid}/pecas`, pecaForm)
      setShowPecaForm(false)
      setPecaForm(emptyPecaForm)
      loadTudo()
    } catch (err) {
      setPecaFormError(err.response?.data?.message || 'Não foi possível salvar a peça.')
    } finally {
      setSavingPeca(false)
    }
  }

  async function handleDeletePeca(peca) {
    if (!confirm(`Remover a peça "${peca.nome}"?`)) return
    await api.delete(`/projetos/${uuid}/pecas/${peca.uuid}`)
    loadTudo()
  }

  async function handleGerarPlano() {
    setGerando(true)
    setPlanoError('')
    try {
      const res = await api.post(`/projetos/${uuid}/plano-de-corte`)
      setPlano(res.data)
      setChapaSelecionada(0)
    } catch (err) {
      setPlanoError(err.response?.data?.message || 'Não foi possível gerar o plano de corte.')
    } finally {
      setGerando(false)
    }
  }

  async function handleExportarPdf() {
    setExportando(true)
    try {
      const res = await api.get(`/projetos/${uuid}/plano-de-corte/pdf`, { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const link = document.createElement('a')
      link.href = url
      link.download = `plano-de-corte-${projeto?.nome || 'projeto'}.pdf`
      link.click()
      window.URL.revokeObjectURL(url)
    } catch {
      setPlanoError('Não foi possível exportar o PDF.')
    } finally {
      setExportando(false)
    }
  }

  if (loading) {
    return <div className="p-6 text-gray-400">Carregando...</div>
  }

  if (!projeto) {
    return <div className="p-6 text-red-600">{error || 'Projeto não encontrado.'}</div>
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white px-6 py-4">
        <Link to="/" className="text-sm text-gray-500 hover:text-cutflow-700">← Projetos</Link>
        <h1 className="mt-1 text-lg font-medium text-cutflow-900">{projeto.nome}</h1>
        {projeto.cliente && <p className="text-sm text-gray-500">Cliente: {projeto.cliente}</p>}
      </header>

      <main className="grid grid-cols-1 gap-6 p-6 lg:grid-cols-3">
        {/* Coluna esquerda: chapas */}
        <section className="lg:col-span-1">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-medium text-gray-900">Chapas</h2>
            <button
              onClick={() => setShowChapaForm((s) => !s)}
              className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700"
            >
              + Chapa
            </button>
          </div>

          {showChapaForm && (
            <form onSubmit={handleChapaSubmit} className="mb-4 rounded-lg border border-gray-200 bg-white p-4">
              <div className="mb-3 grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Largura (mm)</label>
                  <input type="number" required value={chapaForm.larguraMm}
                    onChange={(e) => setChapaForm({ ...chapaForm, larguraMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Altura (mm)</label>
                  <input type="number" required value={chapaForm.alturaMm}
                    onChange={(e) => setChapaForm({ ...chapaForm, alturaMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
              </div>

              <label className="mb-1 block text-xs text-gray-600">Espessura</label>
              <select value={chapaForm.espessuraMm}
                onChange={(e) => setChapaForm({ ...chapaForm, espessuraMm: Number(e.target.value) })}
                className="mb-3 w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none">
                {ESPESSURAS.map((esp) => <option key={esp} value={esp}>{esp}mm</option>)}
              </select>

              <label className="mb-1 block text-xs text-gray-600">Chapas disponíveis</label>
              <input type="number" required min={0} value={chapaForm.quantidadeDisponivel}
                onChange={(e) => setChapaForm({ ...chapaForm, quantidadeDisponivel: Number(e.target.value) })}
                className="mb-3 w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />

              <div className="mb-3 grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Kerf (mm)</label>
                  <input type="number" value={chapaForm.kerfMm}
                    onChange={(e) => setChapaForm({ ...chapaForm, kerfMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Margem borda (mm)</label>
                  <input type="number" value={chapaForm.margemBordaMm}
                    onChange={(e) => setChapaForm({ ...chapaForm, margemBordaMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
              </div>

              {chapaFormError && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{chapaFormError}</p>}

              <div className="flex gap-2">
                <button type="submit" disabled={savingChapa}
                  className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60">
                  {savingChapa ? 'Salvando...' : 'Salvar'}
                </button>
                <button type="button" onClick={() => setShowChapaForm(false)}
                  className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100">
                  Cancelar
                </button>
              </div>
            </form>
          )}

          <div className="space-y-2">
            {chapas.length === 0 && <p className="text-sm text-gray-400">Nenhuma chapa cadastrada.</p>}
            {chapas.map((chapa) => (
              <div key={chapa.uuid} className="rounded-lg border border-gray-200 bg-white p-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-900">{chapa.larguraMm}x{chapa.alturaMm}mm — {chapa.espessuraMm}mm</span>
                  <button onClick={() => handleDeleteChapa(chapa)} className="text-xs text-red-600 hover:underline">Remover</button>
                </div>
                <p className="mt-1 text-xs text-gray-500">
                  {chapa.material} · {chapa.quantidadeDisponivel} disponíveis · kerf {chapa.kerfMm}mm · margem {chapa.margemBordaMm}mm
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* Coluna central: pecas */}
        <section className="lg:col-span-1">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-medium text-gray-900">Peças</h2>
            <button
              onClick={() => setShowPecaForm((s) => !s)}
              className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700"
            >
              + Peça
            </button>
          </div>

          {showPecaForm && (
            <form onSubmit={handlePecaSubmit} className="mb-4 rounded-lg border border-gray-200 bg-white p-4">
              <label className="mb-1 block text-xs text-gray-600">Nome</label>
              <input required autoFocus value={pecaForm.nome} placeholder="Ex: Lateral, Prateleira..."
                onChange={(e) => setPecaForm({ ...pecaForm, nome: e.target.value })}
                className="mb-3 w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />

              <div className="mb-3 grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Largura (mm)</label>
                  <input type="number" required value={pecaForm.larguraMm}
                    onChange={(e) => setPecaForm({ ...pecaForm, larguraMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Altura (mm)</label>
                  <input type="number" required value={pecaForm.alturaMm}
                    onChange={(e) => setPecaForm({ ...pecaForm, alturaMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
              </div>

              <div className="mb-3 grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Espessura</label>
                  <select value={pecaForm.espessuraMm}
                    onChange={(e) => setPecaForm({ ...pecaForm, espessuraMm: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none">
                    {ESPESSURAS.map((esp) => <option key={esp} value={esp}>{esp}mm</option>)}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-xs text-gray-600">Quantidade</label>
                  <input type="number" required min={1} value={pecaForm.quantidade}
                    onChange={(e) => setPecaForm({ ...pecaForm, quantidade: Number(e.target.value) })}
                    className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none" />
                </div>
              </div>

              <label className="mb-1 block text-xs text-gray-600">Acabamento</label>
              <select value={pecaForm.tipoAcabamento}
                onChange={(e) => setPecaForm({ ...pecaForm, tipoAcabamento: e.target.value })}
                className="mb-3 w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none">
                <option value="LISO">Liso (pode girar)</option>
                <option value="COM_VEIO">Com veio (lado certo)</option>
              </select>

              {pecaFormError && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{pecaFormError}</p>}

              <div className="flex gap-2">
                <button type="submit" disabled={savingPeca}
                  className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60">
                  {savingPeca ? 'Salvando...' : 'Salvar'}
                </button>
                <button type="button" onClick={() => setShowPecaForm(false)}
                  className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100">
                  Cancelar
                </button>
              </div>
            </form>
          )}

          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr>
                  <th className="px-3 py-2 font-medium">Peça</th>
                  <th className="px-3 py-2 font-medium">Medidas</th>
                  <th className="px-3 py-2 font-medium">Qtd</th>
                  <th className="px-3 py-2 font-medium"></th>
                </tr>
              </thead>
              <tbody>
                {pecas.length === 0 && (
                  <tr><td colSpan={4} className="px-3 py-4 text-center text-gray-400">Nenhuma peça cadastrada.</td></tr>
                )}
                {pecas.map((peca) => (
                  <tr key={peca.uuid} className="border-t border-gray-100">
                    <td className="px-3 py-2 text-gray-900">
                      {peca.nome}
                      <span className="ml-1 text-xs text-gray-400">
                        {peca.tipoAcabamento === 'COM_VEIO' ? '(veio)' : '(liso)'}
                      </span>
                    </td>
                    <td className="px-3 py-2 text-gray-600">{peca.larguraMm}x{peca.alturaMm}mm, {peca.espessuraMm}mm</td>
                    <td className="px-3 py-2 text-gray-600">{peca.quantidade}</td>
                    <td className="px-3 py-2 text-right">
                      <button onClick={() => handleDeletePeca(peca)} className="text-xs text-red-600 hover:underline">Remover</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <button
            onClick={handleGerarPlano}
            disabled={gerando || pecas.length === 0}
            className="mt-4 w-full rounded bg-cutflow-700 px-4 py-3 text-sm font-semibold text-white hover:bg-cutflow-900 disabled:opacity-50"
          >
            {gerando ? 'Gerando plano...' : 'Gerar plano de corte'}
          </button>
          {planoError && <p className="mt-2 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{planoError}</p>}
        </section>

        {/* Coluna direita: visualizacao do plano */}
        <section className="lg:col-span-1">
          <h2 className="mb-3 text-base font-medium text-gray-900">Visualização</h2>

          {!plano && (
            <div className="rounded-lg border border-dashed border-gray-300 bg-white p-6 text-center text-sm text-gray-400">
              Gere o plano de corte para visualizar as chapas.
            </div>
          )}

          {plano && (
            <div className="rounded-lg border border-gray-200 bg-white p-4">
              <div className="mb-3 flex flex-wrap gap-1">
                {plano.chapas.map((chapa, index) => (
                  <button
                    key={chapa.uuid}
                    onClick={() => setChapaSelecionada(index)}
                    className={`rounded px-3 py-1 text-xs font-medium ${
                      index === chapaSelecionada
                        ? 'bg-cutflow-600 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    Chapa {chapa.numeroChapa}
                  </button>
                ))}
              </div>

              <CanvasChapa chapa={plano.chapas[chapaSelecionada]} />

              <p className="mt-2 text-xs text-gray-500">
                Aproveitamento desta chapa: {plano.chapas[chapaSelecionada]?.percentualAproveitamento}%
              </p>

              <div className="mt-4 grid grid-cols-3 gap-2 border-t border-gray-100 pt-4 text-center">
                <div>
                  <p className="text-xl font-semibold text-cutflow-900">{plano.totalChapasUtilizadas}</p>
                  <p className="text-xs text-gray-500">chapas necessárias</p>
                </div>
                <div>
                  <p className="text-xl font-semibold text-green-700">{plano.percentualAproveitamento}%</p>
                  <p className="text-xs text-gray-500">aproveitamento</p>
                </div>
                <div>
                  <p className="text-xl font-semibold text-red-600">{plano.percentualDesperdicio}%</p>
                  <p className="text-xs text-gray-500">desperdício</p>
                </div>
              </div>

              <button
                onClick={handleExportarPdf}
                disabled={exportando}
                className="mt-4 w-full rounded border border-cutflow-600 px-4 py-2 text-sm font-medium text-cutflow-700 hover:bg-cutflow-50 disabled:opacity-50"
              >
                {exportando ? 'Exportando...' : 'Exportar PDF'}
              </button>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}
