import { useState } from 'react'
import { Link } from 'react-router-dom'
import AbasSecao, { ABAS_MOVIMENTACAO } from '../components/AbasSecao.jsx'
import HistoricoMovimentacoes from '../components/HistoricoMovimentacoes.jsx'
import ConsultaMaterialModal from '../components/ConsultaMaterialModal.jsx'

// Tela principal da aba Movimentação: acesso às operações específicas + histórico geral.
export default function Movimentacao() {
  const [consulta, setConsulta] = useState(false)

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Movimentação</h1>
          <div className="topbar-desc">Entradas, saídas, empréstimos, devoluções, perdas e avarias de estoque</div>
        </div>
        <button className="btn btn-secondary" onClick={() => setConsulta(true)}>Consultar Material</button>
      </header>
      <AbasSecao abas={ABAS_MOVIMENTACAO} />

      <div className="content">
        <div className="shortcut-grid">
          <Link to="/movimentacao/entrada" className="shortcut-card">
            <div className="shortcut-title">Criar entrada</div>
            <div className="shortcut-desc">Receber materiais no almoxarifado</div>
          </Link>
          <Link to="/movimentacao/saida" className="shortcut-card">
            <div className="shortcut-title">Saída para consumo</div>
            <div className="shortcut-desc">Entrega de material para uso, com ficha</div>
          </Link>
          <Link to="/movimentacao/emprestimos" className="shortcut-card">
            <div className="shortcut-title">Empréstimos</div>
            <div className="shortcut-desc">Emprestar, devolver e gerar o termo</div>
          </Link>
          <Link to="/movimentacao/baixa" className="shortcut-card">
            <div className="shortcut-title">Perda ou avaria</div>
            <div className="shortcut-desc">Dar baixa em itens perdidos ou danificados</div>
          </Link>
        </div>

        <HistoricoMovimentacoes />
      </div>

      {consulta && <ConsultaMaterialModal onClose={() => setConsulta(false)} />}
    </>
  )
}
