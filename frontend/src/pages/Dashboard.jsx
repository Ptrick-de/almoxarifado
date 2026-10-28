import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'

const ROTULO_TIPO = {
  ENTRADA: { badge: 'badge-entrada', label: 'Entrada' },
  SAIDA: { badge: 'badge-saida', label: 'Saída' },
  PERDA: { badge: 'badge-perda', label: 'Perda' },
  AVARIA: { badge: 'badge-avaria', label: 'Avaria' },
  EMPRESTIMO: { badge: 'badge-emprestimo', label: 'Empréstimo' },
  DEVOLUCAO: { badge: 'badge-devolucao', label: 'Devolução' },
}

export default function Dashboard() {
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    carregar()
  }, [])

  function carregar() {
    api.buscarDashboard()
      .then(setDados)
      .catch((e) => setErro(mensagemDeErro(e)))
  }

  function formatarData(dataStr) {
    return new Date(dataStr).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Painel geral</h1>
          <div className="topbar-desc">Visão consolidada do almoxarifado</div>
        </div>
      </header>

      <div className="content">
        {erro && <div className="error-banner">{erro}</div>}

        {dados && (
          <>
            <div className="shortcut-grid">
              <Link to="/materiais" className="shortcut-card">
                <div className="shortcut-title">Materiais</div>
                <div className="shortcut-desc">Consultar e cadastrar itens</div>
              </Link>
              <Link to="/movimentacao" className="shortcut-card">
                <div className="shortcut-title">Movimentações</div>
                <div className="shortcut-desc">Entradas, saídas, empréstimos e baixas</div>
              </Link>
              <Link to="/categorias" className="shortcut-card">
                <div className="shortcut-title">Categorias</div>
                <div className="shortcut-desc">Organizar os materiais</div>
              </Link>
              <Link to="/fornecedores" className="shortcut-card">
                <div className="shortcut-title">Fornecedores</div>
                <div className="shortcut-desc">Gerenciar fornecedores</div>
              </Link>
            </div>

            {dados.emprestimosAbertos > 0 && (
              <div className={dados.emprestimosAtrasados > 0 ? 'error-banner' : 'info-banner'} style={{ margin: '0 0 20px' }}>
                {dados.emprestimosAbertos} {dados.emprestimosAbertos === 1 ? 'empréstimo em aberto' : 'empréstimos em aberto'}
                {dados.emprestimosAtrasados > 0 && <> — <strong>{dados.emprestimosAtrasados} em atraso</strong></>}.{' '}
                <Link to="/movimentacao/emprestimos" style={{ textDecoration: 'underline' }}>Ver empréstimos</Link>
              </div>
            )}

            <div className="panel">
              <div className="panel-header">
                <h2>Itens abaixo do estoque mínimo</h2>
                <Link to="/materiais" className="tag-pill">Ver todos os materiais</Link>
              </div>
              <div className="panel-body">
                {dados.materiaisEstoqueBaixo.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-title">Tudo certo por aqui</div>
                    Nenhum item está abaixo do estoque mínimo no momento.
                  </div>
                ) : (
                  <table>
                    <thead>
                      <tr>
                        <th>Código</th>
                        <th>Material</th>
                        <th>Atual</th>
                        <th>Mínimo</th>
                        <th>Situação</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.materiaisEstoqueBaixo.map((m) => (
                        <tr key={m.id}>
                          <td className="mono">{m.codigo}</td>
                          <td>{m.nome}</td>
                          <td>{m.quantidadeAtual} {m.unidade}</td>
                          <td>{m.quantidadeMinima} {m.unidade}</td>
                          <td>
                            <span className={`badge ${m.quantidadeAtual === 0 ? 'badge-out' : 'badge-low'}`}>
                              {m.quantidadeAtual === 0 ? 'Esgotado' : 'Estoque baixo'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>

            <div className="panel">
              <div className="panel-header">
                <h2>Últimas movimentações</h2>
                <Link to="/movimentacao" className="tag-pill">Ver histórico completo</Link>
              </div>
              <div className="panel-body">
                {dados.ultimasMovimentacoes.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-title">Nenhuma movimentação ainda</div>
                    Registre entradas ou saídas para vê-las aqui.
                  </div>
                ) : (
                  <table>
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Material</th>
                        <th>Tipo</th>
                        <th>Quantidade</th>
                        <th>Responsável</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.ultimasMovimentacoes.map((mv) => (
                        <tr key={mv.id}>
                          <td>{formatarData(mv.dataMovimentacao)}</td>
                          <td>{mv.material?.nome}</td>
                          <td>
                            <span className={`badge ${ROTULO_TIPO[mv.tipo]?.badge || 'badge-saida'}`}>
                              {ROTULO_TIPO[mv.tipo]?.label || mv.tipo}
                            </span>
                          </td>
                          <td>{mv.quantidade}</td>
                          <td>{mv.responsavel || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </>
  )
}
