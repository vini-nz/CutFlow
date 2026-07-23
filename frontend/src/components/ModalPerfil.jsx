import { useState } from 'react'
import api from '../services/api.js'
import { useSession } from '../context/SessionContext.jsx'
import Modal from './Modal.jsx'

/** Editar meu perfil: nome, e-mail e senha. */
export default function ModalPerfil({ onClose }) {
  const { sessao, recarregar } = useSession()

  const [nome, setNome] = useState(sessao.usuario.nome)
  const [email, setEmail] = useState(sessao.usuario.email)
  const [salvandoPerfil, setSalvandoPerfil] = useState(false)
  const [perfilErro, setPerfilErro] = useState('')
  const [perfilOk, setPerfilOk] = useState('')

  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [salvandoSenha, setSalvandoSenha] = useState(false)
  const [senhaErro, setSenhaErro] = useState('')
  const [senhaOk, setSenhaOk] = useState('')

  async function salvarPerfil(e) {
    e.preventDefault()
    setSalvandoPerfil(true)
    setPerfilErro('')
    setPerfilOk('')
    try {
      await api.put('/auth/perfil', { nome, email })
      await recarregar()
      setPerfilOk('Perfil atualizado.')
    } catch (err) {
      setPerfilErro(err.response?.data?.message || 'Não foi possível salvar.')
    } finally {
      setSalvandoPerfil(false)
    }
  }

  async function salvarSenha(e) {
    e.preventDefault()
    setSalvandoSenha(true)
    setSenhaErro('')
    setSenhaOk('')
    try {
      await api.put('/auth/senha', { senhaAtual: senhaAtual || null, novaSenha })
      setSenhaAtual('')
      setNovaSenha('')
      setSenhaOk('Senha alterada.')
    } catch (err) {
      setSenhaErro(err.response?.data?.message || 'Não foi possível alterar a senha.')
    } finally {
      setSalvandoSenha(false)
    }
  }

  return (
    <Modal title="Meu perfil" onClose={onClose}>
      <form onSubmit={salvarPerfil} className="mb-5">
        <label className="mb-1 block text-sm text-gray-700">Nome</label>
        <input
          required value={nome} onChange={(e) => setNome(e.target.value)}
          className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        <label className="mb-1 block text-sm text-gray-700">E-mail</label>
        <input
          type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
          className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        {perfilErro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{perfilErro}</p>}
        {perfilOk && <p className="mb-3 rounded bg-green-50 px-3 py-2 text-sm text-green-700">{perfilOk}</p>}
        <button
          type="submit" disabled={salvandoPerfil}
          className="rounded bg-cutflow-600 px-4 py-2 text-sm font-medium text-white hover:bg-cutflow-700 disabled:opacity-60"
        >
          {salvandoPerfil ? 'Salvando...' : 'Salvar perfil'}
        </button>
      </form>

      <form onSubmit={salvarSenha} className="border-t border-gray-100 pt-4">
        <p className="mb-2 text-sm font-medium text-gray-800">Alterar senha</p>
        <label className="mb-1 block text-xs text-gray-600">Senha atual</label>
        <input
          type="password" value={senhaAtual} onChange={(e) => setSenhaAtual(e.target.value)}
          placeholder="deixe vazio se você entra só pelo Google"
          className="mb-2 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        <label className="mb-1 block text-xs text-gray-600">Nova senha</label>
        <input
          type="password" minLength={8} value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)}
          className="mb-1 w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-cutflow-600 focus:outline-none"
        />
        <p className="mb-3 text-[11px] text-gray-400">Ao menos 8 caracteres.</p>
        {senhaErro && <p className="mb-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{senhaErro}</p>}
        {senhaOk && <p className="mb-3 rounded bg-green-50 px-3 py-2 text-sm text-green-700">{senhaOk}</p>}
        <button
          type="submit" disabled={salvandoSenha || novaSenha.length < 8}
          className="rounded border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-60"
        >
          {salvandoSenha ? 'Alterando...' : 'Alterar senha'}
        </button>
      </form>
    </Modal>
  )
}
