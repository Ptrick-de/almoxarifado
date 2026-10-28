import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import { RotaProtegida } from './auth/RotaProtegida.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Materiais from './pages/Materiais.jsx'
import Categorias from './pages/Categorias.jsx'
import Fornecedores from './pages/Fornecedores.jsx'
import Movimentacao from './pages/Movimentacao.jsx'
import MovEntrada from './pages/MovEntrada.jsx'
import MovSaida from './pages/MovSaida.jsx'
import MovEmprestimos from './pages/MovEmprestimos.jsx'
import MovBaixa from './pages/MovBaixa.jsx'
import Relatorios from './pages/Relatorios.jsx'
import DocFicha from './pages/DocFicha.jsx'
import DocTermo from './pages/DocTermo.jsx'
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
        <Route path="/movimentacao" element={<Movimentacao />} />
        <Route path="/movimentacao/entrada" element={<MovEntrada />} />
        <Route path="/movimentacao/saida" element={<MovSaida />} />
        <Route path="/movimentacao/emprestimos" element={<MovEmprestimos />} />
        <Route path="/movimentacao/baixa" element={<MovBaixa />} />
        <Route path="/movimentacoes" element={<Navigate to="/movimentacao" replace />} />
        <Route path="/relatorios" element={<Relatorios />} />
        <Route path="/documentos/ficha/:id" element={<DocFicha />} />
        <Route path="/documentos/termo/:id" element={<DocTermo />} />
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
