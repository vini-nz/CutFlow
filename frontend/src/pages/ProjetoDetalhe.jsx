import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../services/api.js'
import CanvasChapa from '../components/CanvasChapa.jsx'

const ESPESSURAS = [6, 15, 18, 25]

// Debounce da regeneracao automatica do plano: curto o bastante para parecer
// "tempo real", longo o bastante para agrupar uma sequencia rapida de
// inclusoes (ex: colar varias pecas seguidas) numa unica chamada.
const AUTO_REGEN_DEBOUNCE_MS = 600

// Nao existe formulario de criacao de chapa (ADR-0003): a Chapa e' auto-
// provisionada por combinacao espessura+acabamento (ADR-0004) assim que a
// primeira peca correspondente e' salva. Este form so EDITA uma chapa que ja
// existe (largura/altura/kerf/margem). Excluir so e' permitido quando nao
// restam pecas da combinacao (o backend explica isso num 409).
const emptyChapaEditForm = { larguraMm: '', alturaMm: '', espessuraMm: '', kerfMm: '', margemBordaMm: '' }
const emptyPecaForm = { nome: '', alturaMm: '', larguraMm: '', espessuraMm: 15, quantidade: 1, tipoAcabamento: 'LISO' }

function rotuloAcabamento(tipoAcabamento) {
  return tipoAcabamento === 'COM_VEIO' ? 'com veio' : 'liso'
}

export default function ProjetoDetalhe() {
  const { uuid } = useParams()

  const [projeto, setProjeto] = useState(null)
  const [chapas, setChapas] = useState([])
  const [pecas, setPecas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Edicao de uma chapa ja existente (nao ha criacao manual)
  const [chapaEditandoUuid, setChapaEditandoUuid] = useState(null)
  const [chapaForm, setChapaForm] = useState(emptyChapaEditForm)
  const [chapaFormError, setChapaFormError] = useState('')
  const [chapasError, setChapasError] = useState('')
  const [savingChapa, setSavingChapa] = useState(false)

  const [pecaForm, setPecaForm] = useState(emptyPecaForm)
  const [pecaEditandoUuid, setPecaEditandoUuid] = useState(null)
  const [showPecaForm, setShowPecaForm] = useState(false)
  const [pecaFormError, setPecaFormError] = useState('')
  const [pecasError, setPecasError] = useState('')
  const [savingPeca, setSavingPeca] = useState(false)

  const [plano, setPlano] = useState(null)
  const [planoError, setPlanoError] = useState('')
  const [gerando, setGerando] = useState(false)
  const [gerandoAuto, setGerandoAuto] = useState(false)
  const [exportando, setExportando] = useState(false)
  const [chapaSelecionada, setChapaSelecionada] = useState(0)

  // Regeneracao automatica ("tempo real"): cada mutacao bem-sucedida de
  // peca/chapa incrementa o tick; o efeito com debounce dispara a geracao.
  // pecasRef evita closure sobre um estado velho dentro do timeout.
  const [mutacaoTick, setMutacaoTick] = useState(0)
  const pecasRef = useRef(pecas)
  pecasRef.current = pecas

  function recarregarDados() {
    return Promise.all([
      api.get(`/projetos/${uuid}/chapas`),
      api.get(`/projetos/${uuid}/pecas`)
    ]).then(([chapasRes, pecasRes]) => {
      setChapas(chapasRes.data)
      setPecas(pecasRes.data)
    })
  }

  useEffect(() => {
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

    // Ultimo plano persistido (se as pecas nao mudaram desde entao)
    api.get(`/projetos/${uuid}/plano-de-corte`)
      .then((res) => setPlano(res.data))
      .catch(() => setPlano(null))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uuid])

  async function gerarPlano({ automatico } = { automatico: false }) {
    if (automatico) setGerandoAuto(true)
    else setGerando(true)
    setPlanoError('')
    try {
      const res = await api.post(`/projetos/${uuid}/plano-de-corte`)
      setPlano(res.data)
      // Mantem a aba selecionada estavel durante atualizacoes ao vivo; so
      // volta para a primeira se a chapa selecionada deixou de existir.
      setChapaSelecionada((atual) => Math.min(atual, res.data.chapas.length - 1))
    } catch (err) {
      setPlano(null)
      setPlanoError(err.response?.data?.message || 'Não foi possível gerar o plano de corte.')
    } finally {
      if (automatico) setGerandoAuto(false)
      else setGerando(false)
    }
  }

  useEffect(() => {
    if (mutacaoTick === 0) return // abrir a pagina nao regenera nada
    const timer = setTimeout(() => {
      if (pecasRef.current.length === 0) {
        // Sem pecas nao ha plano possivel (e o backend recusaria): limpa a
        // visualizacao em vez de continuar mostrando um plano obsoleto.
        setPlano(null)
        setPlanoError('')
        return
      }
      gerarPlano({ automatico: true })
    }, AUTO_REGEN_DEBOUNCE_MS)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutacaoTick])

  function marcarMutacao() {
    setMutacaoTick((t) => t + 1)
  }

  // Resumo ao vivo (client-side): total de pecas, por acabamento e por
  // espessura - atualiza a cada peca adicionada/removida.
  const resumoPecas = useMemo(() => {
    const porEspessura = {}
    let totalUnidades = 0
    let liso = 0
    let comVeio = 0

    for (const peca of pecas) {
      totalUnidades += peca.quantidade
      if (peca.tipoAcabamento === 'COM_VEIO') comVeio += peca.quantidade
      else liso += peca.quantidade

      porEspessura[peca.espessuraMm] = (porEspessura[peca.espessuraMm] || 0) + peca.quantidade
    }

    return { totalUnidades, liso, comVeio, porEspessura }
  }, [pecas])

  function abrirEdicaoChapa(chapa) {
    setChapaEditandoUuid(chapa.uuid)
    setChapaForm({
      larguraMm: chapa.larguraMm,
      alturaMm: chapa.alturaMm,
      espessuraMm: chapa.espessuraMm,
      kerfMm: chapa.kerfMm,
      margemBordaMm: chapa.margemBordaMm
    })
    setChapaFormError('')
    setChapasError('')
  }

  async function handleChapaSubmit(e) {
    e.preventDefault()
    setSavingChapa(true)
    setChapaFormError('')
    try {
      await api.put(`/projetos/${uuid}/chapas/${chapaEditandoUuid}`, chapaForm)
      setChapaEditandoUuid(null)
      await recarregarDados()
      marcarMutacao()
    } catch (err) {
      setChapaFormError(err.response?.data?.message || 'Não foi possível salvar a chapa.')
    } finally {
      setSavingChapa(false)
    }
  }

  async function handleDeleteChapa(chapa) {
    const rotulo = `${chapa.espessuraMm}mm (${rotuloAcabamento(chapa.tipoAcabamento)})`
    if (!confirm(`Excluir a chapa de ${rotulo}?`)) return
    setChapasError('')
    try {
      await api.delete(`/projetos/${uuid}/chapas/${chapa.uuid}`)
      await recarregarDados()
      marcarMutacao()
    } catch (err) {
      setChapasError(err.response?.data?.message || 'Não foi possível excluir a chapa.')
    }
  }

  function abrirNovaPeca() {
    setPecaEditandoUuid(null)
    setPecaForm(emptyPecaForm)
    setPecaFormError('')
    setShowPecaForm(true)
  }

  function abrirEdicaoPeca(peca) {
    setPecaEditandoUuid(peca.uuid)
    setPecaForm({
      nome: peca.nome,
      alturaMm: peca.alturaMm,
      larguraMm: peca.larguraMm,
      espessuraMm: peca.espessuraMm,
      quantidade: peca.quantidade,
      tipoAcabamento: peca.tipoAcabamento
    })
    setPecaFormError('')
    setShowPecaForm(true)
  }

  function duplicarPeca(peca) {
    // Duplicar abre o formulario de NOVA peca ja preenchido - util para
    // pecas quase iguais (ex: prateleiras de outra largura).
    setPecaEditandoUuid(null)
    setPecaForm({
      nome: `${peca.nome} (cópia)`,
      alturaMm: peca.alturaMm,
      larguraMm: peca.larguraMm,
      espessuraMm: peca.espessuraMm,
      quantidade: peca.quantidade,
      tipoAcabamento: peca.tipoAcabamento
    })
    setPecaFormError('')
    setShowPecaForm(true)
  }

  async function handlePecaSubmit(e) {
    e.preventDefault()
    setSavingPeca(true)
    setPecaFormError('')
    try {
      if (pecaEditandoUuid) {
        await api.put(`/projetos/${uuid}/pecas/${pecaEditandoUuid}`, pecaForm)
      } else {
        await api.post(`/projetos/${uuid}/pecas`, pecaForm)
      }
      setShowPecaForm(false)
      setPecaEditandoUuid(null)
      setPecaForm(emptyPecaForm)
      await recarregarDados() // pecas novas + chapa auto-provisionada
      marcarMutacao()
    } catch (err) {
      setPecaFormError(err.response?.data?.message || 'Não foi possível salvar a peça.')
    } finally {
      setSavingPeca(false)
    }
  }

  async function handleDeletePeca(peca) {
    if (!confirm(`Remover a peça "${peca.nome}"?`)) return
    setPecasError('')
    try {
      await api.delete(`/projetos/${uuid}/pecas/${peca.uuid}`)
      await recarregarDados()
      marcarMutacao()
    } catch (err) {
      setPecasError(err.response?.data?.message || 'Não foi possível remover a peça.')
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

  const chapaDoPlano = plano?.chapas[chapaSelecionada]

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white px-6 py-4">
        <Link to="/" className="text-sm text-gray-500 hover:text-cutflow-700">← Projetos</Link>
        <h1 className="mt-1 text-lg font-medium text-cutflow-900">{projeto.nome}</h1>
        {projeto.cliente && <p className="text-sm text-gray-500">Cliente: {projeto.cliente}</p>}
      </header>

      {/* Resumo ao vivo - atualiza sozinho conforme peças são adicionadas/removidas */}
      {pecas.length > 0 && (
        <div className="border-b border-gray-200 bg-cutflow-50 px-6 py-3">
          <div className="flex flex-wrap gap-6 text-sm">
            <span className="text-gray-700">
              <strong className="text-cutflow-900">{resumoPecas.totalUnidades}</strong> peças no total
            </span>
            <span className="text-gray-700">
              <strong className="text-cutflow-900">{resumoPecas.liso}</strong> lisas ·{' '}
              <strong className="text-cutflow-900">{resumoPecas.comVeio}</strong> com veio
            </span>
            <span className="text-gray-700">
              por espessura:{' '}
              {Object.entries(resumoPecas.porEspessura)
                .sort(([a], [b]) => a - b)
                .map(([esp, qtd]) => `${qtd}× ${esp}mm`)
                .join(' · ')}
            </span>
            {plano && (
              <span className="text-gray-700">
                <strong className="text-cutflow-900">{plano.totalChapasUtilizadas}</strong>{' '}
                {plano.totalChapasUtilizadas === 1 ? 'chapa necessária' : 'chapas necessárias'}
              </span>
            )}
          </div>
        </div>
      )}

      <main className="grid grid-cols-1 gap-6 p-6 lg:grid-cols-3">
        {/* Coluna esquerda: chapas (uma por espessura+acabamento, sem cadastro manual) */}
        <section className="lg:col-span-1">
          <div className="mb-3">
            <h2 className="text-base font-medium text-gray-900">Chapas</h2>
            <p className="text-xs text-gray-500">
              Criadas automaticamente por espessura e acabamento conforme você adiciona peças. Peça com veio
              nunca é cortada em chapa lisa (e vice-versa).
            </p>
          </div>

          {chapasError && <p className="mb-2 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{chapasError}</p>}

          <div className="space-y-2">
            {chapas.length === 0 && (
              <p className="text-sm text-gray-400">Nenhuma chapa ainda — adicione uma peça para gerar a primeira.</p>
            )}
            {chapas.map((chapa) => (
              <div key={chapa.uuid} className="rounded-lg border border-gray-200 bg-white p-3">
                {chapaEditandoUuid === chapa.uuid ? (
                  <form onSubmit={handleChapaSubmit}>
                    <p className="mb-2 text-xs font-medium text-gray-700">
                      Chapa {chapa.espessuraMm}mm · {rotuloAcabamento(chapa.tipoAcabamento)}
                    </p>
                    <div className="mb-2 grid grid-cols-2 gap-2">
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
                    <div className="mb-2 grid grid-cols-2 gap-2">
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

                    {chapaFormError && <p className="mb-2 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{chapaFormError}</p>}

                    <div className="flex gap-2">
                      <button type="submit" disabled={savingChapa}
                        className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60">
                        {savingChapa ? 'Salvando...' : 'Salvar'}
                      </button>
                      <button type="button" onClick={() => setChapaEditandoUuid(null)}
                        className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100">
                        Cancelar
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-medium text-gray-900">
                        {chapa.larguraMm}x{chapa.alturaMm}mm — {chapa.espessuraMm}mm
                      </span>
                      <span className={`ml-2 rounded px-1.5 py-0.5 text-[11px] font-medium ${
                        chapa.tipoAcabamento === 'COM_VEIO'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-gray-100 text-gray-600'
                      }`}>
                        {rotuloAcabamento(chapa.tipoAcabamento)}
                      </span>
                      <p className="mt-1 text-xs text-gray-500">
                        {chapa.material} · kerf {chapa.kerfMm}mm · margem {chapa.margemBordaMm}mm
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-3">
                      <button onClick={() => abrirEdicaoChapa(chapa)} className="text-xs text-cutflow-700 hover:underline">
                        Editar
                      </button>
                      <button onClick={() => handleDeleteChapa(chapa)} className="text-xs text-red-600 hover:underline">
                        Excluir
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* Coluna central: pecas */}
        <section className="lg:col-span-1">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-medium text-gray-900">Peças</h2>
            <button
              onClick={() => (showPecaForm ? setShowPecaForm(false) : abrirNovaPeca())}
              className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700"
            >
              + Peça
            </button>
          </div>

          {showPecaForm && (
            <form onSubmit={handlePecaSubmit} className="mb-4 rounded-lg border border-gray-200 bg-white p-4">
              <p className="mb-2 text-xs font-medium text-gray-700">
                {pecaEditandoUuid ? 'Editar peça' : 'Nova peça'}
              </p>
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
                className="mb-1 w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-cutflow-600 focus:outline-none">
                <option value="LISO">Liso (pode girar)</option>
                <option value="COM_VEIO">Com veio (lado certo)</option>
              </select>
              <p className="mb-3 text-[11px] text-gray-400">
                O acabamento vem de fábrica na chapa: peças com veio saem de chapa com veio, lisas de chapa lisa.
              </p>

              {pecaFormError && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{pecaFormError}</p>}

              <div className="flex gap-2">
                <button type="submit" disabled={savingPeca}
                  className="rounded bg-cutflow-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60">
                  {savingPeca ? 'Salvando...' : 'Salvar'}
                </button>
                <button type="button" onClick={() => { setShowPecaForm(false); setPecaEditandoUuid(null) }}
                  className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100">
                  Cancelar
                </button>
              </div>
            </form>
          )}

          {pecasError && <p className="mb-2 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{pecasError}</p>}

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
                      <div className="flex justify-end gap-2 whitespace-nowrap">
                        <button onClick={() => abrirEdicaoPeca(peca)} className="text-xs text-cutflow-700 hover:underline">Editar</button>
                        <button onClick={() => duplicarPeca(peca)} className="text-xs text-gray-500 hover:underline">Duplicar</button>
                        <button onClick={() => handleDeletePeca(peca)} className="text-xs text-red-600 hover:underline">Remover</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <button
            onClick={() => gerarPlano()}
            disabled={gerando || pecas.length === 0}
            className="mt-4 w-full rounded bg-cutflow-700 px-4 py-3 text-sm font-semibold text-white hover:bg-cutflow-900 disabled:opacity-50"
          >
            {gerando ? 'Gerando plano...' : 'Gerar plano de corte'}
          </button>
          <p className="mt-1 text-center text-[11px] text-gray-400">
            O plano é recalculado sozinho a cada alteração — o botão força uma nova geração.
          </p>
          {planoError && <p className="mt-2 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{planoError}</p>}
        </section>

        {/* Coluna direita: visualizacao do plano */}
        <section className="lg:col-span-1">
          <div className="mb-3 flex items-center gap-2">
            <h2 className="text-base font-medium text-gray-900">Visualização</h2>
            {gerandoAuto && (
              <span className="rounded bg-cutflow-50 px-2 py-0.5 text-[11px] font-medium text-cutflow-700">
                Atualizando plano...
              </span>
            )}
          </div>

          {!plano && (
            <div className="rounded-lg border border-dashed border-gray-300 bg-white p-6 text-center text-sm text-gray-400">
              {pecas.length === 0
                ? 'Adicione peças — o plano de corte aparece aqui sozinho.'
                : 'Gere o plano de corte para visualizar as chapas.'}
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

              <CanvasChapa chapa={chapaDoPlano} />

              {chapaDoPlano && (
                <p className="mt-2 text-xs text-gray-500">
                  {chapaDoPlano.larguraMm}x{chapaDoPlano.alturaMm}mm · {chapaDoPlano.espessuraMm}mm ·{' '}
                  {rotuloAcabamento(chapaDoPlano.tipoAcabamento)} · aproveitamento{' '}
                  {chapaDoPlano.percentualAproveitamento}%
                </p>
              )}

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
