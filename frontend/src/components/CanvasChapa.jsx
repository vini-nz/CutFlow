import { useEffect, useRef } from 'react'

// Paleta fixa: cor deterministica por nome de peca, para o mesmo tipo de
// peca (ex: "Prateleira") ficar sempre com a mesma cor em todas as chapas.
const PALETA = [
  '#d97706', '#2563eb', '#16a34a', '#dc2626', '#7c3aed',
  '#0891b2', '#db2777', '#65a30d', '#ea580c', '#4f46e5'
]

function corParaPeca(nome) {
  let hash = 0
  for (let i = 0; i < nome.length; i++) {
    hash = (hash * 31 + nome.charCodeAt(i)) >>> 0
  }
  return PALETA[hash % PALETA.length]
}

export default function CanvasChapa({ chapa, largura = 700 }) {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !chapa) return

    const escala = largura / chapa.larguraMm
    const altura = Math.round(chapa.alturaMm * escala)
    canvas.width = largura
    canvas.height = altura

    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, largura, altura)

    // Fundo da chapa
    ctx.fillStyle = '#f5f0e8'
    ctx.fillRect(0, 0, largura, altura)
    ctx.strokeStyle = '#78350f'
    ctx.lineWidth = 2
    ctx.strokeRect(0, 0, largura, altura)

    // Sobras (retalhos aproveitaveis)
    ctx.fillStyle = 'rgba(120, 53, 15, 0.08)'
    ctx.strokeStyle = 'rgba(120, 53, 15, 0.35)'
    ctx.setLineDash([4, 3])
    for (const sobra of chapa.sobras) {
      const x = sobra.xMm * escala
      const y = sobra.yMm * escala
      const w = sobra.larguraMm * escala
      const h = sobra.alturaMm * escala
      ctx.fillRect(x, y, w, h)
      ctx.strokeRect(x, y, w, h)
    }
    ctx.setLineDash([])

    // Pecas posicionadas
    for (const pos of chapa.posicionamentos) {
      const x = pos.xMm * escala
      const y = pos.yMm * escala
      const w = pos.larguraMm * escala
      const h = pos.alturaMm * escala

      ctx.fillStyle = corParaPeca(pos.nomePeca)
      ctx.globalAlpha = 0.85
      ctx.fillRect(x, y, w, h)
      ctx.globalAlpha = 1
      ctx.strokeStyle = '#ffffff'
      ctx.lineWidth = 1.5
      ctx.strokeRect(x, y, w, h)

      if (w > 40 && h > 20) {
        ctx.fillStyle = '#ffffff'
        ctx.font = 'bold 12px sans-serif'
        ctx.textBaseline = 'top'
        ctx.fillText(`#${pos.numeroEtiqueta} ${pos.nomePeca}`, x + 4, y + 4)
        ctx.font = '10px sans-serif'
        ctx.fillText(`${pos.larguraMm}x${pos.alturaMm}mm${pos.rotacionada ? ' (girada)' : ''}`, x + 4, y + 19)
      }
    }
  }, [chapa, largura])

  if (!chapa) return null

  return <canvas ref={canvasRef} className="max-w-full rounded border border-gray-200" />
}
