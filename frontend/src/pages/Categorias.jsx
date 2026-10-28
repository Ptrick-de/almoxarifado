import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from '../components/Modal.jsx'
import AbasSecao, { ABAS_MATERIAIS } from '../components/AbasSecao.jsx'
import { useConfirmacao } from '../components/ConfirmDialog.jsx'

const VAZIO = { nome: '', descricao: '' }

export default function Categorias() {
  const [confirmar, dialogoConfirmacao] = useConfirmacao()
  const [categorias, setCategorias] = useState([])
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(VAZIO)

  useEffect(() => { carregar() }, [])

  function carregar() {
    api.listarCategorias().then(setCategorias).catch((e) => setErro(mensagemDeErro(e)))
  }

  function abrirNovo() { setErro(''); setEditando(null); setForm(VAZIO); setModalAberto(true) }
  function abrirEdicao(c) { setErro(''); setEditando(c); setForm({ nome: c.nome, descricao: c.descricao || '' }); setModalAberto(true) }

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      if (editando) await api.atualizarCategoria(editando.id, form)
      else await api.criarCategoria(form)
      setModalAberto(false)
      carregar()
    } catch (e2) { setErro(mensagemDeErro(e2)) }
  }

  async function excluir(c) {
    if (!(await confirmar({
      titulo: 'Excluir categoria',
      mensagem: <>Você está prestes a excluir a categoria <strong>{c.nome}</strong>.</>,
      detalhe: 'Esta ação não pode ser desfeita.',
      rotuloConfirmar: 'Excluir',
      perigo: true,
    }))) return
    setErro('')
    try { await api.excluirCategoria(c.id); carregar() }
    catch (e) { setErro(mensagemDeErro(e)) }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Categorias</h1>
          <div className="topbar-desc">Organize os materiais por tipo</div>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>+ Nova categoria</button>
      </header>
      <AbasSecao abas={ABAS_MATERIAIS} />

      <div className="content">
        {erro && !modalAberto && <div className="error-banner">{erro}</div>}
        <div className="panel">
          <div className="panel-body">
            {categorias.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhuma categoria cadastrada</div>
                Crie categorias para organizar seus materiais.
              </div>
            ) : (
              <table>
                <thead>
                  <tr><th>Nome</th><th>Descrição</th><th></th></tr>
                </thead>
                <tbody>
                  {categorias.map((c) => (
                    <tr key={c.id}>
                      <td>{c.nome}</td>
                      <td>{c.descricao || '—'}</td>
                      <td>
                        <div className="row-actions">
                          <button className="btn btn-secondary btn-sm" onClick={() => abrirEdicao(c)}>Editar</button>
                          <button className="btn btn-danger btn-sm" onClick={() => excluir(c)}>Excluir</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {modalAberto && (
        <Modal title={editando ? 'Editar categoria' : 'Nova categoria'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar}>
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nome</label>
                <input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </div>
              <div className="field full">
                <label>Descrição</label>
                <textarea value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">{editando ? 'Salvar alterações' : 'Cadastrar categoria'}</button>
            </div>
          </form>
        </Modal>
      )}

      {dialogoConfirmacao}
    </>
  )
}
