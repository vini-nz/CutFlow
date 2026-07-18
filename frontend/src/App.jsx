import { Routes, Route, Navigate } from 'react-router-dom'
import Projetos from './pages/Projetos.jsx'
import ProjetoDetalhe from './pages/ProjetoDetalhe.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Projetos />} />
      <Route path="/projetos/:uuid" element={<ProjetoDetalhe />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
