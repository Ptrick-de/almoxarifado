import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import AbasSecao, { ABAS_MOVIMENTACAO } from '../components/AbasSecao.jsx'
import Filtros from '../components/Filtros.jsx'
import Modal from '../components/Modal.jsx'
import SeletorMaterial from '../components/SeletorMaterial.jsx'
import { useMateriais } from '../hooks/useMateriais.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatarData, formatarDataHora, hojeISO, novaChave, SITUACOES_EMPRESTIMO, TIPOS_MOVIMENTACAO } from '../utils/formatos.js'

const FILTROS_INICIAIS = { busca: '', pessoa: '', setor: '', situacao: 'ABERTOS', de: '', ate: '' }
const FILTROS_VAZIOS = { busca: '', pessoa: '', setor: '', situacao: '', de: '', ate: '' }

const CAMPOS_FILTRO = [
  { nome: 'busca', rotulo: 'Material', tipo: 'texto', placeholder: 'Código, nome, marca ou modelo' },
  { nome: 'pessoa', rotulo: 'Quem retirou / entregou', tipo: 'texto' },
  { nome: 'setor', rotulo: 'Setor', tipo: 'texto' },
  {
    nome: 'situacao', rotulo: 'Situação', tipo: 'select',
    opcoes: [
      { valor: 'ABERTOS', rotulo: 'Em aberto (ativos e atrasados)' },
      { valor: 'ATIVO', rotulo: 'Ativo (no prazo)' },
      { valor: 'ATRASADO', rotulo: 'Atrasado' },
      { valor: 'DEVOLVIDO', rotulo: 'Devolvido' },
    ],
  },
  { nome: 'de', rotulo: 'Emprestado de', tipo: 'data' },
  { nome: 'ate', rotulo: 'Emprestado até', tipo: 'data' },
]

export default function MovEmprestimos() {
  const { usuario } = useAuth()
  const { materiais, recarregar: recarregarMateriais, erroCarga } = useMateriais()
  const [params, setParams] = useSearchParams()

  const vazio = () => ({
    materialId: '', quantidade: '', tomador: '', setor: '', responsavelEntrega: usuario?.nome || '',
    dataEmprestimo: hojeISO(), dataPrevistaDevolucao: '', observacao: '',
  })
  const [form, setForm] = useState(vazio)
  const [chave, setChave] = useState(novaChave)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [criado, setCriado] = useState(null)

  const [filtros, setFiltros] = useState(FILTROS_INICIAIS)
  const [lista, setLista] = useState([])
  const [erroLista, setErroLista] = useState('')
  const [versao, setVersao] = useState(0)

  const [devolvendo, setDevolvendo] = useState(null)
  const [detalhe, setDetalhe] = useState(null) // { emprestimo, historico }

  const material = materiais.find((m) => String(m.id) === String(form.materialId))

  useEffect(() => {
    const t = setTimeout(() => {
      api.listarEmprestimos(filtros)
        .then((d) => { setLista(d); setErroLista('') })
        .catch((e) => setErroLista(mensagemDeErro(e)))
    }, 250)
    return () => clearTimeout(t)
  }, [filtros, versao])

  const abrirDetalhe = useCallback(async (id) => {
    try {
      const [emprestimo, historico] = await Promise.all([api.buscarEmprestimo(id), api.historicoEmprestimo(id)])
      setDetalhe({ emprestimo, historico })
    } catch (e) {
      setErroLista(mensagemDeErro(e))
    }
  }, [])

  // Link vindo do histórico de movimentações: ?emprestimo=ID abre os detalhes.
  useEffect(() => {
    const id = params.get('emprestimo')
    if (id) {
      abrirDetalhe(id)
      setParams({}, { replace: true })
    }
  }, [params, setParams, abrirDetalhe])

  async function emprestar(e) {
    e.preventDefault()
    if (enviando) return
    setErro(''); setCriado(null)
    if (form.dataPrevistaDevolucao && form.dataPrevistaDevolucao < form.dataEmprestimo) {
      setErro('A data prevista de devolução não pode ser anterior à data do empréstimo.')
      return
    }
    setEnviando(true)
    try {
      const emp = await api.registrarEmprestimo({
        materialId: Number(form.materialId),
        quantidade: Number(form.quantidade),
        tomador: form.tomador,
        setor: form.setor,
        responsavelEntrega: form.responsavelEntrega,
        dataEmprestimo: form.dataEmprestimo || null,
        dataPrevistaDevolucao: form.dataPrevistaDevolucao || null,
        observacao: form.observacao,
        chaveIdempotencia: chave,
      })
      setCriado(emp)
      setForm(vazio())
      setChave(novaChave())
      setVersao((v) => v + 1)
      recarregarMateriais()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    } finally {
      setEnviando(false)
    }
  }

  function aoDevolver() {
    setDevolvendo(null)
    setVersao((v) => v + 1)
    recarregarMateriais()
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Empréstimos</h1>
          <div className="topbar-desc">Material emprestado sai do saldo disponível e volta quando for devolvido</div>
        </div>
      </header>
      <AbasSecao abas={ABAS_MOVIMENTACAO} />

      <div className="content">
        <div className="panel" style={{ marginBottom: 24 }}>
          <div className="panel-header"><h2>Novo empréstimo</h2></div>
          <form onSubmit={emprestar}>
            {(erro || erroCarga) && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro || erroCarga}</div>}
            {criado && (
              <div style={{ margin: '16px 20px 0', display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }} className="success-banner">
                <span>Empréstimo nº {criado.id} registrado: {criado.quantidade} {criado.material?.unidade} de {criado.material?.nome} para {criado.tomador}.</span>
                <Link className="btn btn-primary btn-sm" to={`/documentos/termo/${criado.id}`}>Gerar Termo de Empréstimo</Link>
              </div>
            )}

            <div className="form-section">Material emprestado</div>
            <div className="form-grid">
              <SeletorMaterial materiais={materiais} value={form.materialId} onChange={(v) => setForm({ ...form, materialId: v })} exigirSaldo />
              <div className="field">
                <label>Quantidade {material ? `(${material.unidade})` : ''} *</label>
                <input
                  required type="number" min="1" step="1" max={material ? material.quantidadeAtual : undefined}
                  value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })}
                />
                {material && <span className="field-hint">Disponível para empréstimo: {material.quantidadeAtual} {material.unidade}</span>}
              </div>
            </div>

            <div className="form-section">Quem está retirando</div>
            <div className="form-grid">
              <div className="field">
                <label>Nome de quem retira *</label>
                <input required maxLength={150} value={form.tomador} onChange={(e) => setForm({ ...form, tomador: e.target.value })} />
              </div>
              <div className="field">
                <label>Setor / departamento</label>
                <input maxLength={100} value={form.setor} onChange={(e) => setForm({ ...form, setor: e.target.value })} />
              </div>
              <div className="field">
                <label>Responsável pela entrega *</label>
                <input required maxLength={150} value={form.responsavelEntrega} onChange={(e) => setForm({ ...form, responsavelEntrega: e.target.value })} />
              </div>
            </div>

            <div className="form-section">Prazo</div>
            <div className="form-grid">
              <div className="field">
                <label>Data do empréstimo *</label>
                <input required type="date" max={hojeISO()} value={form.dataEmprestimo} onChange={(e) => setForm({ ...form, dataEmprestimo: e.target.value })} />
              </div>
              <div className="field">
                <label>Data prevista de devolução *</label>
                <input required type="date" min={form.dataEmprestimo || undefined} value={form.dataPrevistaDevolucao} onChange={(e) => setForm({ ...form, dataPrevistaDevolucao: e.target.value })} />
              </div>
              <div className="field full">
                <label>Observações</label>
                <textarea maxLength={1000} value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => { setForm(vazio()); setErro(''); setCriado(null) }}>Limpar</button>
              <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Registrando...' : 'Registrar empréstimo'}</button>
            </div>
          </form>
        </div>

        <div className="panel">
          <div className="panel-header"><h2>Empréstimos</h2></div>
          <div style={{ padding: '16px 20px 0' }}>
            <Filtros
              campos={CAMPOS_FILTRO}
              valores={filtros}
              onChange={(nome, valor) => setFiltros((f) => ({ ...f, [nome]: valor }))}
              onLimpar={() => setFiltros(FILTROS_VAZIOS)}
              total={lista.length}
            />
            {erroLista && <div className="error-banner">{erroLista}</div>}
          </div>
          <div className="panel-body" style={{ overflowX: 'auto' }}>
            {lista.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhum empréstimo encontrado</div>
                Registre um empréstimo ou ajuste os filtros.
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Nº</th><th>Material</th><th>Retirado por</th><th>Emprestado</th><th>Devolvido</th>
                    <th>Pendente</th><th>Previsão</th><th>Situação</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((emp) => {
                    const s = SITUACOES_EMPRESTIMO[emp.situacao] || { label: emp.situacao, classe: '' }
                    return (
                      <tr key={emp.id}>
                        <td className="mono">{emp.id}</td>
                        <td className="linha-material">
                          <span className="mono">{emp.material?.codigo}</span> {emp.material?.nome}
                          <small>{[emp.material?.marca, emp.material?.modelo].filter(Boolean).join(' / ')}</small>
                        </td>
                        <td>
                          {emp.tomador}
                          {emp.setor && <small className="texto-pequeno" style={{ display: 'block' }}>{emp.setor}</small>}
                        </td>
                        <td>{formatarData(emp.dataEmprestimo)}</td>
                        <td>{emp.quantidadeDevolvida} / {emp.quantidade}</td>
                        <td>{emp.quantidadePendente} {emp.material?.unidade}</td>
                        <td>
                          {formatarData(emp.dataPrevistaDevolucao)}
                          {emp.dataDevolucaoEfetiva && <small className="texto-pequeno" style={{ display: 'block' }}>Devolvido em {formatarData(emp.dataDevolucaoEfetiva)}</small>}
                        </td>
                        <td><span className={`badge ${s.classe}`}>{s.label}</span></td>
                        <td>
                          <div className="row-actions">
                            {emp.status === 'ATIVO' && (
                              <button className="btn btn-primary btn-sm" onClick={() => setDevolvendo(emp)}>Devolver</button>
                            )}
                            <button className="btn btn-secondary btn-sm" onClick={() => abrirDetalhe(emp.id)}>Detalhes</button>
                            <Link className="btn btn-secondary btn-sm" to={`/documentos/termo/${emp.id}`}>Gerar Termo de Empréstimo</Link>
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
      </div>

      {devolvendo && <DevolucaoModal emprestimo={devolvendo} usuario={usuario} onClose={() => setDevolvendo(null)} onSalvo={aoDevolver} />}

      {detalhe && (
        <Modal title={`Empréstimo nº ${detalhe.emprestimo.id}`} onClose={() => setDetalhe(null)} larg>
          <div className="detail-grid">
            <Item rotulo="Material" valor={`${detalhe.emprestimo.material?.codigo} — ${detalhe.emprestimo.material?.nome}`} />
            <Item rotulo="Retirado por" valor={detalhe.emprestimo.tomador} />
            <Item rotulo="Setor" valor={detalhe.emprestimo.setor || '—'} />
            <Item rotulo="Responsável pela entrega" valor={detalhe.emprestimo.responsavelEntrega} />
            <Item rotulo="Data do empréstimo" valor={formatarData(detalhe.emprestimo.dataEmprestimo)} />
            <Item rotulo="Devolução prevista" valor={formatarData(detalhe.emprestimo.dataPrevistaDevolucao)} />
            <Item rotulo="Quantidade" valor={`${detalhe.emprestimo.quantidade} (devolvido ${detalhe.emprestimo.quantidadeDevolvida}, pendente ${detalhe.emprestimo.quantidadePendente})`} />
            <Item rotulo="Situação" valor={(SITUACOES_EMPRESTIMO[detalhe.emprestimo.situacao] || {}).label || detalhe.emprestimo.situacao} />
          </div>
          <div style={{ padding: '0 20px 20px', overflowX: 'auto' }}>
            <table>
              <thead><tr><th>Data</th><th>Operação</th><th>Qtd.</th><th>Responsável</th><th>Observação</th></tr></thead>
              <tbody>
                {detalhe.historico.map((h) => (
                  <tr key={h.id}>
                    <td>{formatarDataHora(h.dataMovimentacao)}</td>
                    <td>{(TIPOS_MOVIMENTACAO[h.tipo] || {}).label || h.tipo}</td>
                    <td>{h.quantidade}</td>
                    <td>{h.responsavel || '—'}</td>
                    <td>{h.observacao || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </>
  )
}

function Item({ rotulo, valor }) {
  return (
    <div className="detail-item">
      <div className="rotulo">{rotulo}</div>
      <div className="valor">{valor}</div>
    </div>
  )
}

function DevolucaoModal({ emprestimo, usuario, onClose, onSalvo }) {
  const [form, setForm] = useState({
    quantidade: String(emprestimo.quantidadePendente), dataDevolucao: hojeISO(),
    responsavel: usuario?.nome || '', observacao: '',
  })
  const [chave] = useState(novaChave)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')

  async function devolver(e) {
    e.preventDefault()
    if (enviando) return
    setErro('')
    setEnviando(true)
    try {
      await api.registrarDevolucao(emprestimo.id, {
        quantidade: Number(form.quantidade),
        dataDevolucao: form.dataDevolucao || null,
        responsavel: form.responsavel,
        observacao: form.observacao,
        chaveIdempotencia: chave,
      })
      onSalvo()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
      setEnviando(false)
    }
  }

  return (
    <Modal title={`Devolução — empréstimo nº ${emprestimo.id}`} onClose={onClose}>
      <form onSubmit={devolver}>
        {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
        <div className="info-banner">
          {emprestimo.material?.nome}: {emprestimo.quantidadePendente} {emprestimo.material?.unidade} ainda com {emprestimo.tomador}.
          Devoluções parciais são permitidas.
        </div>
        <div className="form-grid">
          <div className="field">
            <label>Quantidade devolvida *</label>
            <input required type="number" min="1" max={emprestimo.quantidadePendente} step="1" autoFocus
              value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })} />
          </div>
          <div className="field">
            <label>Data da devolução *</label>
            <input required type="date" max={hojeISO()} min={emprestimo.dataEmprestimo}
              value={form.dataDevolucao} onChange={(e) => setForm({ ...form, dataDevolucao: e.target.value })} />
          </div>
          <div className="field full">
            <label>Responsável pelo recebimento</label>
            <input maxLength={150} value={form.responsavel} onChange={(e) => setForm({ ...form, responsavel: e.target.value })} />
          </div>
          <div className="field full">
            <label>Observações (estado do material)</label>
            <textarea maxLength={1000} value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
          </div>
        </div>
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
          <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Registrando...' : 'Confirmar devolução'}</button>
        </div>
      </form>
    </Modal>
  )
}
