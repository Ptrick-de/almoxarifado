import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'

export function RotaProtegida({ children, apenasAdmin = false }) {
  const { autenticado, carregando, usuario } = useAuth()

  if (carregando) return null
  if (!autenticado) return <Navigate to="/login" replace />
  if (apenasAdmin && usuario.papel !== 'ADMIN') return <Navigate to="/" replace />

  return children
}
