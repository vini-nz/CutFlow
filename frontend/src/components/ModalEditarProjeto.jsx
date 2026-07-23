import { useState } from 'react'
import api from '../services/api.js'
import Modal from './Modal.jsx'

/** Editar nome/cliente do projeto. onSaved recebe o projeto atualizado. */
export default function ModalEditarProjeto({ projeto, onClose, onSaved }) {
  const [nome, setNome] = useState(projeto.nome)
  const [cliente, setCliente] = useState(projeto.cliente || '')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  async function salvar(e) {
    e.preventDefault()
    setSalvando(true)
    setErro('')
    try {
      const { data } = await api.put(`/projetos/${projeto.uuid}`, { nome, cliente: cliente || null })
      onSaved(data)
      onClose()
    } catch (err) {
      setErro(err.response?.data?.message || 'Não foi possível salvar.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal title="Editar projeto" onClose={onClose}>
      <form onSubmit={salvar}>
        <label className="mb-1 block text-sm text-gray-700">Nome *</label>
        <input
          required autoFocus value={nome} onChange={(e) => setNome(e.target.value)}
          className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        <label className="mb-1 block text-sm text-gray-700">Cliente</label>
        <input
          value={cliente} onChange={(e) => setCliente(e.target.value)}
          className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        {erro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}
        <div className="flex gap-2">
          <button
            type="submit" disabled={salvando}
            className="rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
          >
            {salvando ? 'Salvando...' : 'Salvar'}
          </button>
          <button type="button" onClick={onClose} className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}
