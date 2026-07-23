import { Routes, Route, Navigate } from 'react-router-dom'
import Projetos from './pages/Projetos.jsx'
import ProjetoDetalhe from './pages/ProjetoDetalhe.jsx'
import Login from './pages/Login.jsx'
import Registro from './pages/Registro.jsx'
import OnboardingOrganizacao from './pages/OnboardingOrganizacao.jsx'
import Equipe from './pages/Equipe.jsx'
import Convite from './pages/Convite.jsx'
import ProtectedLayout from './components/ProtectedLayout.jsx'
import RequireAuth from './components/RequireAuth.jsx'

export default function App() {
  return (
    <Routes>
      {/* Públicas */}
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      {/* Aceite de convite (ADR-0006): acessível sem login para mostrar o convite */}
      <Route path="/convite/:token" element={<Convite />} />

      {/* Logado, mas ainda sem organização */}
      <Route path="/onboarding" element={<RequireAuth><OnboardingOrganizacao /></RequireAuth>} />

      {/* App: exige login + organização ativa (compartilham a barra superior) */}
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<Projetos />} />
        <Route path="/projetos/:uuid" element={<ProjetoDetalhe />} />
        <Route path="/equipe" element={<Equipe />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
