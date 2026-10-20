import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from '../components/Modal.jsx'
import { useConfirmacao } from '../components/ConfirmDialog.jsx'

const UNIDADES = [
  'UNIDADE', 'CAIXA', 'PACOTE', 'FRASCO', 'PAR', 'RESMA', 'ROLO', 'METRO',
  'LITRO', 'QUILO', 'GRAMA', 'GALAO', 'KIT', 'JOGO', 'SACO', 'LATA',
]

const VAZIO = {
  codigo: '', nome: '', descricao: '', unidade: 'UNIDADE', quantidadeAtual: 0,
  quantidadeMinima: 0, precoUnitario: 0, localizacao: '', categoriaId: '', fornecedorId: '',
}

export default function Materiais() {
  const [confirmar, dialogoConfirmacao] = useConfirmacao()
  const [materiais, setMateriais] = useState([])
  const [categorias, setCategorias] = useState([])
  const [fornecedores, setFornecedores] = useState([])
  const [busca, setBusca] = useState('')
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(VAZIO)
  const [rapido, setRapido] = useState(null) // 'categoria' | 'fornecedor' | null
  const [rapidoForm, setRapidoForm] = useState({ nome: '', descricao: '' })
  const [rapidoErro, setRapidoErro] = useState('')

  useEffect(() => {
    carregarTudo()
  }, [])

  function carregarTudo() {
    Promise.all([api.listarMateriais(), api.listarCategorias(), api.listarFornecedores()])
      .then(([m, c, f]) => { setMateriais(m); setCategorias(c); setFornecedores(f) })
      .catch((e) => setErro(mensagemDeErro(e)))
  }

  function buscar(termo) {
    setBusca(termo)
    api.listarMateriais(termo).then(setMateriais).catch((e) => setErro(mensagemDeErro(e)))
  }

  function abrirNovo() {
    setErro('')
    setEditando(null)
    setForm(VAZIO)
    setModalAberto(true)
  }

  function abrirEdicao(material) {
    setErro('')
    setEditando(material)
    setForm({
      codigo: material.codigo,
      nome: material.nome,
      descricao: material.descricao || '',
      unidade: material.unidade,
      quantidadeAtual: material.quantidadeAtual,
      quantidadeMinima: material.quantidadeMinima,
      precoUnitario: material.precoUnitario,
      localizacao: material.localizacao || '',
      categoriaId: material.categoria?.id || '',
      fornecedorId: material.fornecedor?.id || '',
    })
    setModalAberto(true)
  }

  function abrirRapido(tipo) {
    setRapido(tipo)
    setRapidoForm({ nome: '', descricao: '' })
    setRapidoErro('')
  }

  // Cadastro rápido: cria a categoria/fornecedor sem sair do formulário do material
  // e já deixa o novo registro selecionado.
  async function salvarRapido(e) {
    e.preventDefault()
    e.stopPropagation()
    setRapidoErro('')
    try {
      if (rapido === 'categoria') {
        const nova = await api.criarCategoria({ nome: rapidoForm.nome, descricao: rapidoForm.descricao })
        setCategorias((lista) => [...lista, nova].sort((a, b) => a.nome.localeCompare(b.nome)))
        setForm((f) => ({ ...f, categoriaId: nova.id }))
      } else {
        const novo = await api.criarFornecedor({ nome: rapidoForm.nome })
        setFornecedores((lista) => [...lista, novo].sort((a, b) => a.nome.localeCompare(b.nome)))
        setForm((f) => ({ ...f, fornecedorId: novo.id }))
      }
      setRapido(null)
    } catch (e2) {
      setRapidoErro(mensagemDeErro(e2))
    }
  }

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    const payload = {
      codigo: form.codigo,
      nome: form.nome,
      descricao: form.descricao,
      unidade: form.unidade,
      quantidadeAtual: Number(form.quantidadeAtual),
      quantidadeMinima: Number(form.quantidadeMinima),
      precoUnitario: Number(form.precoUnitario),
      localizacao: form.localizacao,
      categoria: form.categoriaId ? { id: Number(form.categoriaId) } : null,
      fornecedor: form.fornecedorId ? { id: Number(form.fornecedorId) } : null,
    }
    try {
      if (editando) {
        await api.atualizarMaterial(editando.id, payload)
      } else {
        await api.criarMaterial(payload)
      }
      setModalAberto(false)
      carregarTudo()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    }
  }

  async function excluir(material) {
    if (!(await confirmar({
      titulo: 'Excluir material',
      mensagem: <>Você está prestes a excluir o material <strong>{material.nome}</strong>.</>,
      detalhe: 'Esta ação não pode ser desfeita.',
      rotuloConfirmar: 'Excluir',
      perigo: true,
    }))) return
    setErro('')
    try {
      await api.excluirMaterial(material.id)
      carregarTudo()
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  function situacao(m) {
    if (m.quantidadeAtual === 0) return { classe: 'badge-out', texto: 'Esgotado' }
    if (m.quantidadeAtual <= m.quantidadeMinima) return { classe: 'badge-low', texto: 'Estoque baixo' }
    return { classe: 'badge-ok', texto: 'Normal' }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Materiais</h1>
          <div className="topbar-desc">Cadastro e controle de itens do estoque</div>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>+ Novo material</button>
      </header>

      <div className="content">
        {erro && !modalAberto && <div className="error-banner">{erro}</div>}

        <div className="search-bar">
          <input
            placeholder="Buscar por nome ou código..."
            value={busca}
            onChange={(e) => buscar(e.target.value)}
          />
        </div>

        <div className="panel">
          <div className="panel-body">
            {materiais.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhum material encontrado</div>
                Cadastre o primeiro item do seu estoque.
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Nome</th>
                    <th>Categoria</th>
                    <th>Qtd. atual</th>
                    <th>Qtd. mínima</th>
                    <th>Preço unit.</th>
                    <th>Situação</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {materiais.map((m) => {
                    const s = situacao(m)
                    return (
                      <tr key={m.id}>
                        <td className="mono">{m.codigo}</td>
                        <td>{m.nome}</td>
                        <td>{m.categoria?.nome || '—'}</td>
                        <td>{m.quantidadeAtual} {m.unidade}</td>
                        <td>{m.quantidadeMinima} {m.unidade}</td>
                        <td>{Number(m.precoUnitario).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</td>
                        <td><span className={`badge ${s.classe}`}>{s.texto}</span></td>
                        <td>
                          <div className="row-actions">
                            <button className="btn btn-secondary btn-sm" onClick={() => abrirEdicao(m)}>Editar</button>
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

      {modalAberto && (
        <Modal title={editando ? 'Editar material' : 'Novo material'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar}>
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
            <div className="form-grid">
              <div className="field">
                <label>Código</label>
                <input required value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} />
              </div>
              <div className="field">
                <label>Nome</label>
                <input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </div>
              <div className="field full">
                <label>Descrição</label>
                <textarea value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
              </div>
              <div className="field">
                <label>Unidade de medida</label>
                <select required value={form.unidade} onChange={(e) => setForm({ ...form, unidade: e.target.value })}>
                  {/* mantém unidades antigas (fora da lista) selecionáveis ao editar */}
                  {form.unidade && !UNIDADES.includes(form.unidade) && <option value={form.unidade}>{form.unidade}</option>}
                  {UNIDADES.map((u) => <option key={u} value={u}>{u.charAt(0) + u.slice(1).toLowerCase()}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Localização</label>
                <input value={form.localizacao} onChange={(e) => setForm({ ...form, localizacao: e.target.value })} />
              </div>
              <div className="field">
                <label>Quantidade atual</label>
                <input required type="number" min="0" value={form.quantidadeAtual} onChange={(e) => setForm({ ...form, quantidadeAtual: e.target.value })} />
              </div>
              <div className="field">
                <label>Quantidade mínima</label>
                <input required type="number" min="0" value={form.quantidadeMinima} onChange={(e) => setForm({ ...form, quantidadeMinima: e.target.value })} />
              </div>
              <div className="field">
                <label>Preço unitário (R$)</label>
                <input required type="number" min="0" step="0.01" value={form.precoUnitario} onChange={(e) => setForm({ ...form, precoUnitario: e.target.value })} />
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
              <div className="field">
                <div className="field-header">
                  <label>Fornecedor</label>
                  <button type="button" className="link-btn" onClick={() => abrirRapido('fornecedor')}>+ Novo fornecedor</button>
                </div>
                <select value={form.fornecedorId} onChange={(e) => setForm({ ...form, fornecedorId: e.target.value })}>
                  <option value="">Nenhum</option>
                  {fornecedores.map((f) => <option key={f.id} value={f.id}>{f.nome}</option>)}
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">{editando ? 'Salvar alterações' : 'Cadastrar material'}</button>
            </div>
          </form>
        </Modal>
      )}

      {modalAberto && rapido && (
        <Modal title={rapido === 'categoria' ? 'Nova categoria' : 'Novo fornecedor'} onClose={() => setRapido(null)}>
          <form onSubmit={salvarRapido}>
            {rapidoErro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{rapidoErro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nome</label>
                <input required autoFocus value={rapidoForm.nome} onChange={(e) => setRapidoForm({ ...rapidoForm, nome: e.target.value })} />
              </div>
              {rapido === 'categoria' && (
                <div className="field full">
                  <label>Descrição</label>
                  <textarea value={rapidoForm.descricao} onChange={(e) => setRapidoForm({ ...rapidoForm, descricao: e.target.value })} />
                </div>
              )}
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setRapido(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">{rapido === 'categoria' ? 'Cadastrar categoria' : 'Cadastrar fornecedor'}</button>
            </div>
          </form>
        </Modal>
      )}

      {dialogoConfirmacao}
    </>
  )
}
