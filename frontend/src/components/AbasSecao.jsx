import { NavLink } from 'react-router-dom'

// Faixa de abas no topo de cada seção (Materiais, Movimentação...).
export const ABAS_MATERIAIS = [
  { to: '/materiais', label: 'Materiais' },
  { to: '/fornecedores', label: 'Fornecedores' },
  { to: '/categorias', label: 'Categorias' },
]

export const ABAS_MOVIMENTACAO = [
  { to: '/movimentacao', label: 'Visão geral', end: true },
  { to: '/movimentacao/entrada', label: 'Criar entrada' },
  { to: '/movimentacao/saida', label: 'Saída para consumo' },
  { to: '/movimentacao/emprestimos', label: 'Empréstimos' },
  { to: '/movimentacao/baixa', label: 'Perda ou avaria' },
]

export default function AbasSecao({ abas }) {
  return (
    <nav className="subtabs" aria-label="Seções">
      {abas.map((a) => (
        <NavLink key={a.to} to={a.to} end={a.end} className={({ isActive }) => (isActive ? 'active' : '')}>
          {a.label}
        </NavLink>
      ))}
    </nav>
  )
}
