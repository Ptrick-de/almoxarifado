import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import Filtros from './Filtros.jsx'
import { formatarDataHora, moeda, TIPOS_MOVIMENTACAO } from '../utils/formatos.js'

const VAZIOS = { busca: '', responsavel: '', tipo: '', de: '', ate: '' }

// Histórico de movimentações com filtros combináveis.
// tipos: lista de tipos que a tela mostra (ex.: ['PERDA','AVARIA']); omitido = todos.
export default function HistoricoMovimentacoes({ tipos = null, recarregar = 0, titulo = 'Histórico de movimentações' }) {
  const [filtros, setFiltros] = useState(VAZIOS)
  const [lista, setLista] = useState([])
  const [erro, setErro] = useState('')

  const tiposDisponiveis = (tipos || Object.keys(TIPOS_MOVIMENTACAO))

  useEffect(() => {
    const t = setTimeout(() => {
      const params = { ...filtros }
      if (!params.tipo && tipos && tipos.length === 1) params.tipo = tipos[0]
      api.listarMovimentacoes(params)
        .then((dados) => {
          setErro('')
          // Telas com mais de um tipo fixo (ex.: perda + avaria) filtram o restante aqui.
          setLista(tipos && tipos.length > 1 && !filtros.tipo ? dados.filter((m) => tipos.includes(m.tipo)) : dados)
        })
        .catch((e) => setErro(mensagemDeErro(e)))
    }, 250)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtros, recarregar])

  const campos = [
    { nome: 'busca', rotulo: 'Material', tipo: 'texto', placeholder: 'Código, nome, marca ou modelo' },
    { nome: 'responsavel', rotulo: 'Responsável / quem recebeu', tipo: 'texto' },
  ]
  if (tiposDisponiveis.length > 1) {
    campos.push({
      nome: 'tipo', rotulo: 'Tipo', tipo: 'select',
      opcoes: tiposDisponiveis.map((t) => ({ valor: t, rotulo: TIPOS_MOVIMENTACAO[t].label })),
    })
  }
  campos.push({ nome: 'de', rotulo: 'Período — de', tipo: 'data' }, { nome: 'ate', rotulo: 'Período — até', tipo: 'data' })

  return (
    <div className="panel">
      <div className="panel-header"><h2>{titulo}</h2></div>
      <div style={{ padding: '16px 20px 0' }}>
        <Filtros
          campos={campos}
          valores={filtros}
          onChange={(nome, valor) => setFiltros((f) => ({ ...f, [nome]: valor }))}
          onLimpar={() => setFiltros(VAZIOS)}
          total={lista.length}
        />
        {erro && <div className="error-banner">{erro}</div>}
      </div>
      <div className="panel-body" style={{ overflowX: 'auto' }}>
        {lista.length === 0 ? (
          <div className="empty-state">
            <div className="empty-title">Nenhuma movimentação encontrada</div>
            Registre uma operação ou ajuste os filtros.
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Data</th><th>Material</th><th>Tipo</th><th>Qtd.</th><th>Saldo após</th>
                <th>Responsável</th><th>Recebedor / fornecedor</th><th>Observação</th><th></th>
              </tr>
            </thead>
            <tbody>
              {lista.map((mv) => {
                const t = TIPOS_MOVIMENTACAO[mv.tipo] || { classe: '', label: mv.tipo }
                return (
                  <tr key={mv.id}>
                    <td>{formatarDataHora(mv.dataMovimentacao)}</td>
                    <td className="linha-material">
                      <span className="mono">{mv.material?.codigo}</span> {mv.material?.nome}
                      <small>{[mv.material?.marca, mv.material?.modelo].filter(Boolean).join(' / ')}</small>
                    </td>
                    <td><span className={`badge ${t.classe}`}>{t.label}</span></td>
                    <td>{mv.quantidade}</td>
                    <td>{mv.saldoApos ?? '—'}</td>
                    <td>{mv.responsavel || '—'}</td>
                    <td>
                      {mv.destinatario || mv.fornecedor?.nome || '—'}
                      {mv.setor && <small className="texto-pequeno" style={{ display: 'block' }}>{mv.setor}</small>}
                      {mv.tipo === 'ENTRADA' && mv.precoUnitario != null && (
                        <small className="texto-pequeno" style={{ display: 'block' }}>{moeda(mv.precoUnitario)} / un.</small>
                      )}
                    </td>
                    <td>{mv.observacao || '—'}</td>
                    <td>
                      <div className="row-actions">
                        {mv.tipo === 'SAIDA' && (
                          <Link className="btn btn-secondary btn-sm" to={`/documentos/ficha/${mv.id}`}>Ficha de Entrega de Material</Link>
                        )}
                        {mv.emprestimoId && (
                          <Link className="btn btn-secondary btn-sm" to={`/movimentacao/emprestimos?emprestimo=${mv.emprestimoId}`}>Ver empréstimo</Link>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
