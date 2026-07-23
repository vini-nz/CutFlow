/**
 * Janela modal reutilizável (overlay + card + fechar). As telas de edição
 * novas usam modal em vez de formulário inline, começando a corrigir a UX
 * apontada no feedback.
 */
export default function Modal({ title, onClose, children }) {
  return (
    <div
      className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-black/40 p-4"
      onClick={onClose}
    >
      <div className="mt-16 w-full max-w-md rounded-xl bg-white p-6 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-cutflow-900">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-700" aria-label="Fechar">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}
