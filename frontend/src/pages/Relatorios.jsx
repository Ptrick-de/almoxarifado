import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import Filtros from '../components/Filtros.jsx'
import {
  baixarCsv, formatarData, formatarDataHora, moeda, SITUACOES_EMPRESTIMO, SITUACOES_MATERIAL, TIPOS_MOVIMENTACAO,
} from '../utils/formatos.js'

const RELATORIOS = [
  { id: 'estoque', label: 'Posição de estoque' },
  { id: 'movimentacoes', label: 'Movimentações' },
  { id: 'emprestimos', label: 'Empréstimos' },
]

export default function Relatorios() {
  const [params, setParams] = useSearchParams()
  const tipo = params.get('tipo')
  const aba = RELATORIOS.some((r) => r.id === tipo) ? tipo : 'estoque'
  const setAba = (id) => setParams({ tipo: id })
  return (
    <>
      <header className="topbar">
        <div>
          <h1>Relatórios</h1>
          <div className="topbar-desc">Consulte, filtre, imprima ou exporte (CSV) as informações do almoxarifado</div>
        </div>
      </header>
      <nav className="subtabs no-print" aria-label="Relatórios">
        {RELATORIOS.map((r) => (
          <a key={r.id} href={`#${r.id}`} className={aba === r.id ? 'active' : ''}
            onClick={(e) => { e.preventDefault(); setAba(r.id) }}>{r.label}</a>
        ))}
      </nav>
      <div className="content">
        {aba === 'estoque' && <RelEstoque />}
        {aba === 'movimentacoes' && <RelMovimentacoes />}
        {aba === 'emprestimos' && <RelEmprestimos />}
      </div>
    </>
  )
}

function useLista(carregar, filtros) {
  const [lista, setLista] = useState([])
  const [erro, setErro] = useState('')
  useEffect(() => {
    const t = setTimeout(() => {
      carregar(filtros).then((d) => { setLista(d); setErro('') }).catch((e) => setErro(mensagemDeErro(e)))
    }, 250)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtros])
  return [lista, erro]
}

function Painel({ titulo, filtrosEl, erro, vazio, children }) {
  return (
    <div className="panel">
      <div className="panel-header"><h2>{titulo}</h2></div>
      <div className="no-print" style={{ padding: '16px 20px 0' }}>
        {filtrosEl}
        {erro && <div className="error-banner">{erro}</div>}
      </div>
      <div className="panel-body" style={{ overflowX: 'auto' }}>
        {vazio ? <div className="empty-state"><div className="empty-title">Nenhum registro encontrado</div>Ajuste os filtros.</div> : children}
      </div>
    </div>
  )
}

const BOTOES = (onCsv) => (
  <>
    <button type="button" className="btn btn-secondary btn-sm" onClick={onCsv}>Exportar CSV</button>
    <button type="button" className="btn btn-secondary btn-sm" onClick={() => window.print()}>Imprimir</button>
  </>
)

function RelEstoque() {
  const vazios = { busca: '', marca: '', fornecedor: '', situacao: '' }
  const [filtros, setFiltros] = useState(vazios)
  const [lista, erro] = useLista(api.listarMateriais, filtros)
  const campos = [
    { nome: 'busca', rotulo: 'Código ou nome', tipo: 'texto' },
    { nome: 'marca', rotulo: 'Marca', tipo: 'texto' },
    { nome: 'fornecedor', rotulo: 'Fornecedor', tipo: 'texto' },
    { nome: 'situacao', rotulo: 'Situação', tipo: 'select', opcoes: Object.entries(SITUACOES_MATERIAL).map(([valor, v]) => ({ valor, rotulo: v.label })) },
  ]
  const valorTotal = lista.reduce((s, m) => s + Number(m.precoUnitario || 0) * m.quantidadeAtual, 0)

  const csv = () => baixarCsv('posicao-estoque.csv',
    ['Código', 'Material', 'Marca', 'Modelo', 'C.A.', 'Unidade', 'Disponível', 'Emprestado', 'Total', 'Mínimo', 'Preço unitário', 'Situação'],
    lista.map((m) => [m.codigo, m.nome, m.marca, m.modelo, m.ca, m.unidade, m.quantidadeDisponivel, m.quantidadeEmprestada, m.quantidadeTotal, m.quantidadeMinima, m.precoUnitario, SITUACOES_MATERIAL[m.situacao]?.label]))

  return (
    <Painel titulo="Posição de estoque" erro={erro} vazio={lista.length === 0}
      filtrosEl={<Filtros campos={campos} valores={filtros} total={lista.length}
        onChange={(n, v) => setFiltros((f) => ({ ...f, [n]: v }))} onLimpar={() => setFiltros(vazios)}>{BOTOES(csv)}</Filtros>}>
      <table>
        <thead><tr><th>Código</th><th>Material</th><th>Marca / modelo</th><th>Disponível</th><th>Emprestado</th><th>Total</th><th>Preço ref.</th><th>Situação</th></tr></thead>
        <tbody>
          {lista.map((m) => {
            const s = SITUACOES_MATERIAL[m.situacao] || { label: m.situacao, classe: '' }
            return (
              <tr key={m.id}>
                <td className="mono">{m.codigo}</td>
                <td>{m.nome}</td>
                <td>{[m.marca, m.modelo].filter(Boolean).join(' / ') || '—'}</td>
                <td>{m.quantidadeDisponivel} {m.unidade}</td>
                <td>{m.quantidadeEmprestada}</td>
                <td>{m.quantidadeTotal}</td>
                <td>{moeda(m.precoUnitario)}</td>
                <td><span className={`badge ${s.classe}`}>{s.label}</span></td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className="texto-pequeno" style={{ padding: '12px 20px' }}>Valor em estoque disponível (preço de referência): {moeda(valorTotal)}</div>
    </Painel>
  )
}

function RelMovimentacoes() {
  const vazios = { busca: '', responsavel: '', tipo: '', de: '', ate: '' }
  const [filtros, setFiltros] = useState(vazios)
  const [lista, erro] = useLista(api.listarMovimentacoes, filtros)
  const campos = [
    { nome: 'busca', rotulo: 'Material', tipo: 'texto' },
    { nome: 'responsavel', rotulo: 'Responsável / recebedor', tipo: 'texto' },
    { nome: 'tipo', rotulo: 'Tipo', tipo: 'select', opcoes: Object.entries(TIPOS_MOVIMENTACAO).map(([valor, v]) => ({ valor, rotulo: v.label })) },
    { nome: 'de', rotulo: 'De', tipo: 'data' },
    { nome: 'ate', rotulo: 'Até', tipo: 'data' },
  ]
  const csv = () => baixarCsv('movimentacoes.csv',
    ['Data', 'Código', 'Material', 'Tipo', 'Quantidade', 'Saldo após', 'Responsável', 'Recebedor', 'Setor', 'Fornecedor', 'Preço unitário', 'Observação'],
    lista.map((m) => [formatarDataHora(m.dataMovimentacao), m.material?.codigo, m.material?.nome, TIPOS_MOVIMENTACAO[m.tipo]?.label, m.quantidade, m.saldoApos, m.responsavel, m.destinatario, m.setor, m.fornecedor?.nome, m.precoUnitario, m.observacao]))

  return (
    <Painel titulo="Movimentações" erro={erro} vazio={lista.length === 0}
      filtrosEl={<Filtros campos={campos} valores={filtros} total={lista.length}
        onChange={(n, v) => setFiltros((f) => ({ ...f, [n]: v }))} onLimpar={() => setFiltros(vazios)}>{BOTOES(csv)}</Filtros>}>
      <table>
        <thead><tr><th>Data</th><th>Material</th><th>Tipo</th><th>Qtd.</th><th>Responsável</th><th>Recebedor / fornecedor</th><th>Observação</th></tr></thead>
        <tbody>
          {lista.map((m) => {
            const t = TIPOS_MOVIMENTACAO[m.tipo] || { label: m.tipo, classe: '' }
            return (
              <tr key={m.id}>
                <td>{formatarDataHora(m.dataMovimentacao)}</td>
                <td><span className="mono">{m.material?.codigo}</span> {m.material?.nome}</td>
                <td><span className={`badge ${t.classe}`}>{t.label}</span></td>
                <td>{m.quantidade}</td>
                <td>{m.responsavel || '—'}</td>
                <td>{m.destinatario || m.fornecedor?.nome || '—'}</td>
                <td>{m.observacao || '—'}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </Painel>
  )
}

function RelEmprestimos() {
  const vazios = { busca: '', pessoa: '', situacao: 'ABERTOS', de: '', ate: '' }
  const [filtros, setFiltros] = useState(vazios)
  const [lista, erro] = useLista(api.listarEmprestimos, filtros)
  const campos = [
    { nome: 'busca', rotulo: 'Material', tipo: 'texto' },
    { nome: 'pessoa', rotulo: 'Quem retirou / entregou', tipo: 'texto' },
    {
      nome: 'situacao', rotulo: 'Situação', tipo: 'select',
      opcoes: [
        { valor: 'ABERTOS', rotulo: 'Em aberto' }, { valor: 'ATIVO', rotulo: 'Ativo (no prazo)' },
        { valor: 'ATRASADO', rotulo: 'Atrasado' }, { valor: 'DEVOLVIDO', rotulo: 'Devolvido' },
      ],
    },
    { nome: 'de', rotulo: 'Emprestado de', tipo: 'data' },
    { nome: 'ate', rotulo: 'Emprestado até', tipo: 'data' },
  ]
  const csv = () => baixarCsv('emprestimos.csv',
    ['Nº', 'Código', 'Material', 'Retirado por', 'Setor', 'Responsável', 'Emprestado em', 'Previsão', 'Quantidade', 'Devolvido', 'Pendente', 'Situação'],
    lista.map((e) => [e.id, e.material?.codigo, e.material?.nome, e.tomador, e.setor, e.responsavelEntrega, formatarData(e.dataEmprestimo), formatarData(e.dataPrevistaDevolucao), e.quantidade, e.quantidadeDevolvida, e.quantidadePendente, SITUACOES_EMPRESTIMO[e.situacao]?.label]))

  return (
    <Painel titulo="Empréstimos" erro={erro} vazio={lista.length === 0}
      filtrosEl={<Filtros campos={campos} valores={filtros} total={lista.length}
        onChange={(n, v) => setFiltros((f) => ({ ...f, [n]: v }))} onLimpar={() => setFiltros({ ...vazios, situacao: '' })}>{BOTOES(csv)}</Filtros>}>
      <table>
        <thead><tr><th>Nº</th><th>Material</th><th>Retirado por</th><th>Emprestado</th><th>Previsão</th><th>Pendente</th><th>Situação</th></tr></thead>
        <tbody>
          {lista.map((e) => {
            const s = SITUACOES_EMPRESTIMO[e.situacao] || { label: e.situacao, classe: '' }
            return (
              <tr key={e.id}>
                <td className="mono">{e.id}</td>
                <td><span className="mono">{e.material?.codigo}</span> {e.material?.nome}</td>
                <td>{e.tomador}{e.setor ? ` (${e.setor})` : ''}</td>
                <td>{formatarData(e.dataEmprestimo)}</td>
                <td>{formatarData(e.dataPrevistaDevolucao)}</td>
                <td>{e.quantidadePendente} {e.material?.unidade}</td>
                <td><span className={`badge ${s.classe}`}>{s.label}</span></td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </Painel>
  )
}
