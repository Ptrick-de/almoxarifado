import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import { RotaProtegida } from './auth/RotaProtegida.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Materiais from './pages/Materiais.jsx'
import Categorias from './pages/Categorias.jsx'
import Fornecedores from './pages/Fornecedores.jsx'
import Movimentacoes from './pages/Movimentacoes.jsx'
import Usuarios from './pages/Usuarios.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<RotaProtegida><Layout /></RotaProtegida>}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/materiais" element={<Materiais />} />
        <Route path="/categorias" element={<Categorias />} />
        <Route path="/fornecedores" element={<Fornecedores />} />
        <Route path="/movimentacoes" element={<Movimentacoes />} />
        <Route
          path="/usuarios"
          element={
            <RotaProtegida apenasAdmin>
              <Usuarios />
            </RotaProtegida>
          }
        />
      </Route>
    </Routes>
  )
}
