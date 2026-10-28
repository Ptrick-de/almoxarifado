import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from '../components/Modal.jsx'
import AbasSecao, { ABAS_MATERIAIS } from '../components/AbasSecao.jsx'
import Filtros from '../components/Filtros.jsx'
import ConsultaMaterialModal from '../components/ConsultaMaterialModal.jsx'
import { useConfirmacao } from '../components/ConfirmDialog.jsx'
import { moeda, SITUACOES_MATERIAL } from '../utils/formatos.js'

const UNIDADES = [
  'UNIDADE', 'CAIXA', 'PACOTE', 'FRASCO', 'PAR', 'RESMA', 'ROLO', 'METRO',
  'LITRO', 'QUILO', 'GRAMA', 'GALAO', 'KIT', 'JOGO', 'SACO', 'LATA',
]

const VAZIO = {
  nome: '', descricao: '', marca: '', modelo: '', ca: '', unidade: 'UNIDADE', quantidadeAtual: 0,
  quantidadeMinima: 0, precoUnitario: '', localizacao: '', categoriaId: '', ativo: true,
  codigoBase: '', fornecedores: [], // [{ fornecedorId, precoUnitario }]
}

const FILTROS_VAZIOS = {
  busca: '', codigo: '', nome: '', marca: '', modelo: '', ca: '', fornecedorId: '', categoriaId: '', situacao: '',
}

export default function Materiais() {
  const [confirmar, dialogoConfirmacao] = useConfirmacao()
  const [materiais, setMateriais] = useState([])
  const [categorias, setCategorias] = useState([])
  const [fornecedores, setFornecedores] = useState([])
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS)
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [variacaoDe, setVariacaoDe] = useState(null)
  const [form, setForm] = useState(VAZIO)
  const [salvando, setSalvando] = useState(false)
  const [consulta, setConsulta] = useState(null) // false/null fechado; { material } ou {} aberto
  const [rapido, setRapido] = useState(null) // 'categoria' | 'fornecedor' | null (+ indice do fornecedor)
  const [rapidoForm, setRapidoForm] = useState({ nome: '', descricao: '' })
  const [rapidoErro, setRapidoErro] = useState('')
  const [recarregar, setRecarregar] = useState(0)

  useEffect(() => {
    Promise.all([api.listarCategorias(), api.listarFornecedores()])
      .then(([c, f]) => { setCategorias(c); setFornecedores(f) })
      .catch((e) => setErro(mensagemDeErro(e)))
  }, [])

  // Pesquisa com filtros combináveis (aguarda o usuário parar de digitar).
  useEffect(() => {
    const t = setTimeout(() => {
      api.listarMateriais(filtros)
        .then((lista) => { setMateriais(lista); setErro('') })
        .catch((e) => setErro(mensagemDeErro(e)))
    }, 250)
    return () => clearTimeout(t)
  }, [filtros, recarregar])

  function abrirNovo() {
    setErro('')
    setEditando(null)
    setVariacaoDe(null)
    setForm(VAZIO)
    setModalAberto(true)
  }

  // Nova marca/modelo/C.A. de um produto que já existe: mantém o mesmo código do produto.
  function abrirVariacao(m) {
    setErro('')
    setEditando(null)
    setVariacaoDe(m)
    setForm({
      ...VAZIO,
      nome: m.nome,
      descricao: m.descricao || '',
      unidade: m.unidade,
      quantidadeMinima: m.quantidadeMinima,
      localizacao: m.localizacao || '',
      categoriaId: m.categoria?.id || '',
      codigoBase: m.codigo,
    })
    setModalAberto(true)
  }

  function abrirEdicao(m) {
    setErro('')
    setEditando(m)
    setVariacaoDe(null)
    setForm({
      nome: m.nome,
      descricao: m.descricao || '',
      marca: m.marca || '',
      modelo: m.modelo || '',
      ca: m.ca || '',
      unidade: m.unidade,
      quantidadeAtual: m.quantidadeAtual,
      quantidadeMinima: m.quantidadeMinima,
      precoUnitario: m.precoUnitario ?? '',
      localizacao: m.localizacao || '',
      categoriaId: m.categoria?.id || '',
      ativo: m.ativo,
      codigoBase: '',
      fornecedores: m.fornecedores.map((f) => ({ fornecedorId: f.fornecedorId, precoUnitario: f.precoUnitario })),
    })
    setModalAberto(true)
  }

  function abrirRapido(tipo, indice = null) {
    setRapido({ tipo, indice })
    setRapidoForm({ nome: '', descricao: '' })
    setRapidoErro('')
  }

  // Cadastro rápido: cria a categoria/fornecedor sem sair do formulário do material.
  async function salvarRapido(e) {
    e.preventDefault()
    e.stopPropagation()
    setRapidoErro('')
    try {
      if (rapido.tipo === 'categoria') {
        const nova = await api.criarCategoria({ nome: rapidoForm.nome, descricao: rapidoForm.descricao })
        setCategorias((lista) => [...lista, nova].sort((a, b) => a.nome.localeCompare(b.nome)))
        setForm((f) => ({ ...f, categoriaId: nova.id }))
      } else {
        const novo = await api.criarFornecedor({ nome: rapidoForm.nome })
        setFornecedores((lista) => [...lista, novo].sort((a, b) => a.nome.localeCompare(b.nome)))
        setForm((f) => {
          const lista = [...f.fornecedores]
          if (rapido.indice != null && lista[rapido.indice]) lista[rapido.indice] = { ...lista[rapido.indice], fornecedorId: novo.id }
          return { ...f, fornecedores: lista }
        })
      }
      setRapido(null)
    } catch (e2) {
      setRapidoErro(mensagemDeErro(e2))
    }
  }

  function alterarFornecedor(indice, campo, valor) {
    setForm((f) => ({
      ...f,
      fornecedores: f.fornecedores.map((x, i) => (i === indice ? { ...x, [campo]: valor } : x)),
    }))
  }

  function adicionarFornecedor() {
    setForm((f) => ({ ...f, fornecedores: [...f.fornecedores, { fornecedorId: '', precoUnitario: '' }] }))
  }

  function removerFornecedor(indice) {
    setForm((f) => ({ ...f, fornecedores: f.fornecedores.filter((_, i) => i !== indice) }))
  }

  async function salvar(e) {
    e.preventDefault()
    setErro('')

    const ids = form.fornecedores.map((f) => String(f.fornecedorId))
    if (ids.some((id) => !id)) { setErro('Selecione o fornecedor em todas as linhas ou remova as linhas vazias.'); return }
    if (new Set(ids).size !== ids.length) { setErro('Um mesmo fornecedor foi informado mais de uma vez.'); return }
    if (form.fornecedores.some((f) => f.precoUnitario === '' || Number(f.precoUnitario) < 0)) {
      setErro('Informe o preço de cada fornecedor (zero ou maior).')
      return
    }

    const payload = {
      nome: form.nome,
      descricao: form.descricao,
      marca: form.marca,
      modelo: form.modelo,
      ca: form.ca,
      unidade: form.unidade,
      quantidadeAtual: Number(form.quantidadeAtual),
      quantidadeMinima: Number(form.quantidadeMinima),
      precoUnitario: form.precoUnitario === '' ? null : Number(form.precoUnitario),
      localizacao: form.localizacao,
      categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
      ativo: form.ativo,
      codigoBase: form.codigoBase || null,
      fornecedores: form.fornecedores.map((f) => ({ fornecedorId: Number(f.fornecedorId), precoUnitario: Number(f.precoUnitario) })),
    }
    setSalvando(true)
    try {
      if (editando) await api.atualizarMaterial(editando.id, payload)
      else await api.criarMaterial(payload)
      setModalAberto(false)
      setRecarregar((n) => n + 1)
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    } finally {
      setSalvando(false)
    }
  }

  async function excluir(m) {
    if (!(await confirmar({
      titulo: 'Excluir material',
      mensagem: <>Você está prestes a excluir o material <strong>{m.nome}</strong>{m.marca ? <> ({m.marca})</> : null}.</>,
      detalhe: 'Esta ação não pode ser desfeita. Materiais com histórico de movimentações não podem ser excluídos — nesse caso, inative o cadastro.',
      rotuloConfirmar: 'Excluir',
      perigo: true,
    }))) return
    setErro('')
    try {
      await api.excluirMaterial(m.id)
      setRecarregar((n) => n + 1)
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  const campos = [
    { nome: 'busca', rotulo: 'Pesquisar', tipo: 'texto', placeholder: 'Código, nome, marca, modelo, C.A. ou fornecedor' },
    { nome: 'codigo', rotulo: 'Código', tipo: 'texto' },
    { nome: 'nome', rotulo: 'Nome ou descrição', tipo: 'texto' },
    { nome: 'marca', rotulo: 'Marca', tipo: 'texto' },
    { nome: 'modelo', rotulo: 'Modelo', tipo: 'texto' },
    { nome: 'ca', rotulo: 'C.A.', tipo: 'texto' },
    { nome: 'fornecedorId', rotulo: 'Fornecedor', tipo: 'select', opcoes: fornecedores.map((f) => ({ valor: f.id, rotulo: f.nome })) },
    { nome: 'categoriaId', rotulo: 'Categoria', tipo: 'select', opcoes: categorias.map((c) => ({ valor: c.id, rotulo: c.nome })) },
    {
      nome: 'situacao', rotulo: 'Situação do estoque', tipo: 'select',
      opcoes: Object.entries(SITUACOES_MATERIAL).map(([valor, s]) => ({ valor, rotulo: s.label })),
    },
  ]

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Materiais</h1>
          <div className="topbar-desc">Cadastro, variações, fornecedores e disponibilidade dos itens do estoque</div>
        </div>
        <div className="acoes-pagina">
          <button className="btn btn-secondary" onClick={() => setConsulta({})}>Consultar Material</button>
          <button className="btn btn-primary" onClick={abrirNovo}>+ Novo material</button>
        </div>
      </header>
      <AbasSecao abas={ABAS_MATERIAIS} />

      <div className="content">
        {erro && !modalAberto && <div className="error-banner">{erro}</div>}

        <Filtros
          campos={campos}
          valores={filtros}
          onChange={(nome, valor) => setFiltros((f) => ({ ...f, [nome]: valor }))}
          onLimpar={() => setFiltros(FILTROS_VAZIOS)}
          total={materiais.length}
        />

        <div className="panel">
          <div className="panel-body" style={{ overflowX: 'auto' }}>
            {materiais.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhum material encontrado</div>
                Cadastre o primeiro item do seu estoque ou ajuste os filtros.
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Material</th>
                    <th>Marca / modelo</th>
                    <th>C.A.</th>
                    <th>Fornecedores</th>
                    <th>Disponível</th>
                    <th>Emprestada</th>
                    <th>Situação</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {materiais.map((m) => {
                    const s = SITUACOES_MATERIAL[m.situacao]
                    return (
                      <tr key={m.id}>
                        <td className="mono">{m.codigo}</td>
                        <td className="linha-material">
                          {m.nome}
                          <small>{m.categoria?.nome || 'Sem categoria'}{m.localizacao ? ` · ${m.localizacao}` : ''}</small>
                        </td>
                        <td>{[m.marca, m.modelo].filter(Boolean).join(' / ') || '—'}</td>
                        <td>{m.ca || '—'}</td>
                        <td className="linha-material">
                          {m.fornecedores.length === 0 ? '—' : m.fornecedores.map((f) => (
                            <span key={f.fornecedorId} style={{ display: 'block' }}>
                              {f.fornecedorNome} <small style={{ display: 'inline' }}>{moeda(f.precoUnitario)}</small>
                            </span>
                          ))}
                        </td>
                        <td>{m.quantidadeDisponivel} {m.unidade}</td>
                        <td>{m.quantidadeEmprestada}</td>
                        <td><span className={`badge ${s.classe}`}>{s.label}</span></td>
                        <td>
                          <div className="row-actions">
                            <button className="btn btn-secondary btn-sm" onClick={() => setConsulta({ material: m })}>Consultar</button>
                            <button className="btn btn-secondary btn-sm" onClick={() => abrirEdicao(m)}>Editar</button>
                            <button className="btn btn-secondary btn-sm" onClick={() => abrirVariacao(m)} title="Mesmo produto, outra marca, modelo ou C.A.">Nova variação</button>
                            <button className="btn btn-danger btn-sm" onClick={() => excluir(m)}>Excluir</button>
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

      {consulta && <ConsultaMaterialModal materialInicial={consulta.material || null} onClose={() => setConsulta(null)} />}

      {modalAberto && (
        <Modal title={editando ? 'Editar material' : variacaoDe ? 'Nova variação do produto' : 'Novo material'} onClose={() => setModalAberto(false)} larg>
          <form onSubmit={salvar}>
            {variacaoDe && (
              <div className="info-banner">
                Nova marca, modelo ou C.A. do produto <strong>{variacaoDe.codigo} — {variacaoDe.nome}</strong>. O código do produto é mantido.
              </div>
            )}
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}

            <div className="form-section">Identificação</div>
            <div className="form-grid">
              <div className="field">
                <label>Código</label>
                <input readOnly value={editando ? editando.codigo : variacaoDe ? variacaoDe.codigo : 'Gerado automaticamente'} />
                <span className="field-hint">
                  {editando ? 'O código identifica o produto e não pode ser alterado.' : 'Produtos com o mesmo nome reutilizam o mesmo código.'}
                </span>
              </div>
              <div className="field">
                <label>Nome do material *</label>
                <input required maxLength={255} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </div>
              <div className="field full">
                <label>Descrição</label>
                <textarea value={form.descricao} maxLength={255} onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
              </div>
            </div>

            <div className="form-section">Marca, modelo e C.A.</div>
            <div className="form-grid">
              <div className="field">
                <label>Marca</label>
                <input maxLength={80} value={form.marca} onChange={(e) => setForm({ ...form, marca: e.target.value })} />
              </div>
              <div className="field">
                <label>Modelo</label>
                <input maxLength={80} value={form.modelo} onChange={(e) => setForm({ ...form, modelo: e.target.value })} />
              </div>
              <div className="field">
                <label>C.A. (Certificado de Aprovação)</label>
                <input maxLength={30} placeholder="Quando aplicável (EPIs)" value={form.ca} onChange={(e) => setForm({ ...form, ca: e.target.value })} />
              </div>
            </div>

            <div className="form-section">Estoque</div>
            <div className="form-grid">
              <div className="field">
                <label>Unidade de medida *</label>
                <select required value={form.unidade} onChange={(e) => setForm({ ...form, unidade: e.target.value })}>
                  {form.unidade && !UNIDADES.includes(form.unidade) && <option value={form.unidade}>{form.unidade}</option>}
                  {UNIDADES.map((u) => <option key={u} value={u}>{u.charAt(0) + u.slice(1).toLowerCase()}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Localização</label>
                <input maxLength={255} value={form.localizacao} onChange={(e) => setForm({ ...form, localizacao: e.target.value })} />
              </div>
              <div className="field">
                <label>{editando ? 'Quantidade em estoque' : 'Quantidade inicial em estoque'}</label>
                <input
                  type="number" min="0" required readOnly={!!editando}
                  value={form.quantidadeAtual}
                  onChange={(e) => setForm({ ...form, quantidadeAtual: e.target.value })}
                />
                <span className="field-hint">
                  {editando ? 'O saldo só muda por entradas, saídas, empréstimos e baixas.' : 'Se maior que zero, é lançada como entrada no histórico.'}
                </span>
              </div>
              <div className="field">
                <label>Quantidade mínima *</label>
                <input required type="number" min="0" value={form.quantidadeMinima} onChange={(e) => setForm({ ...form, quantidadeMinima: e.target.value })} />
              </div>
              <div className="field">
                <div className="field-header">
                  <label>Categoria</label>
                  <button type="button" className="link-btn" onClick={() => abrirRapido('categoria')}>+ Nova categoria</button>
                </div>
                <select value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })}>
                  <option value="">Nenhuma</option>
                  {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </div>
              {editando && (
                <div className="field">
                  <label>Situação do cadastro</label>
                  <label className="check">
                    <input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                    Material ativo
                  </label>
                  <span className="field-hint">Inativo = indisponível para novas movimentações; o histórico é preservado.</span>
                </div>
              )}
            </div>

            <div className="form-section">Fornecedores e preços</div>
            <div className="form-grid">
              <div className="field">
                <label>Preço unitário de referência (R$)</label>
                <input type="number" min="0" step="0.01" placeholder="Opcional" value={form.precoUnitario} onChange={(e) => setForm({ ...form, precoUnitario: e.target.value })} />
                <span className="field-hint">Se vazio, usa o menor preço entre os fornecedores.</span>
              </div>
              <div className="field full">
                <div className="field-header">
                  <label>Fornecedores desta variação (preço por fornecedor)</label>
                  <button type="button" className="link-btn" onClick={adicionarFornecedor}>+ Adicionar fornecedor</button>
                </div>
              </div>
              {form.fornecedores.length === 0 && (
                <div className="field full texto-pequeno">Nenhum fornecedor associado.</div>
              )}
              {form.fornecedores.map((f, i) => (
                <div className="fornecedor-linha" key={i}>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <select style={{ flex: 1 }} aria-label="Fornecedor" value={f.fornecedorId} onChange={(e) => alterarFornecedor(i, 'fornecedorId', e.target.value)}>
                      <option value="">Selecione o fornecedor</option>
                      {fornecedores.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}
                    </select>
                    <button type="button" className="link-btn" onClick={() => abrirRapido('fornecedor', i)}>+ Novo</button>
                  </div>
                  <input type="number" min="0" step="0.01" placeholder="Preço (R$)" aria-label="Preço do fornecedor" value={f.precoUnitario} onChange={(e) => alterarFornecedor(i, 'precoUnitario', e.target.value)} />
                  <button type="button" className="btn btn-danger btn-sm" onClick={() => removerFornecedor(i)}>Remover</button>
                </div>
              ))}
            </div>

            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary" disabled={salvando}>
                {salvando ? 'Salvando...' : editando ? 'Salvar alterações' : 'Cadastrar material'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {modalAberto && rapido && (
        <Modal title={rapido.tipo === 'categoria' ? 'Nova categoria' : 'Novo fornecedor'} onClose={() => setRapido(null)}>
          <form onSubmit={salvarRapido}>
            {rapidoErro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{rapidoErro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nome</label>
                <input required autoFocus value={rapidoForm.nome} onChange={(e) => setRapidoForm({ ...rapidoForm, nome: e.target.value })} />
              </div>
              {rapido.tipo === 'categoria' && (
                <div className="field full">
                  <label>Descrição</label>
                  <textarea value={rapidoForm.descricao} onChange={(e) => setRapidoForm({ ...rapidoForm, descricao: e.target.value })} />
                </div>
              )}
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setRapido(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">{rapido.tipo === 'categoria' ? 'Cadastrar categoria' : 'Cadastrar fornecedor'}</button>
            </div>
          </form>
        </Modal>
      )}

      {dialogoConfirmacao}
    </>
  )
}
