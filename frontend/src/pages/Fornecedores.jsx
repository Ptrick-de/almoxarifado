import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from '../components/Modal.jsx'
import { useConfirmacao } from '../components/ConfirmDialog.jsx'

const VAZIO = { nome: '', cnpj: '', telefone: '', email: '', endereco: '' }

export default function Fornecedores() {
  const [confirmar, dialogoConfirmacao] = useConfirmacao()
  const [fornecedores, setFornecedores] = useState([])
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(VAZIO)

  useEffect(() => { carregar() }, [])

  function carregar() {
    api.listarFornecedores().then(setFornecedores).catch((e) => setErro(mensagemDeErro(e)))
  }

  function abrirNovo() { setErro(''); setEditando(null); setForm(VAZIO); setModalAberto(true) }
  function abrirEdicao(f) {
    setErro('')
    setEditando(f)
    setForm({ nome: f.nome, cnpj: f.cnpj || '', telefone: f.telefone || '', email: f.email || '', endereco: f.endereco || '' })
    setModalAberto(true)
  }

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      if (editando) await api.atualizarFornecedor(editando.id, form)
      else await api.criarFornecedor(form)
      setModalAberto(false)
      carregar()
    } catch (e2) { setErro(mensagemDeErro(e2)) }
  }

  async function excluir(f) {
    if (!(await confirmar({
      titulo: 'Excluir fornecedor',
      mensagem: <>Você está prestes a excluir o fornecedor <strong>{f.nome}</strong>.</>,
      detalhe: 'Esta ação não pode ser desfeita.',
      rotuloConfirmar: 'Excluir',
      perigo: true,
    }))) return
    setErro('')
    try { await api.excluirFornecedor(f.id); carregar() }
    catch (e) { setErro(mensagemDeErro(e)) }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Fornecedores</h1>
          <div className="topbar-desc">Empresas que abastecem o almoxarifado</div>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>+ Novo fornecedor</button>
      </header>

      <div className="content">
        {erro && !modalAberto && <div className="error-banner">{erro}</div>}
        <div className="panel">
          <div className="panel-body">
            {fornecedores.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhum fornecedor cadastrado</div>
                Cadastre as empresas que fornecem materiais.
              </div>
            ) : (
              <table>
                <thead>
                  <tr><th>Nome</th><th>CNPJ</th><th>Telefone</th><th>E-mail</th><th></th></tr>
                </thead>
                <tbody>
                  {fornecedores.map((f) => (
                    <tr key={f.id}>
                      <td>{f.nome}</td>
                      <td className="mono">{f.cnpj || '—'}</td>
                      <td>{f.telefone || '—'}</td>
                      <td>{f.email || '—'}</td>
                      <td>
                        <div className="row-actions">
                          <button className="btn btn-secondary btn-sm" onClick={() => abrirEdicao(f)}>Editar</button>
                          <button className="btn btn-danger btn-sm" onClick={() => excluir(f)}>Excluir</button>
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
        <Modal title={editando ? 'Editar fornecedor' : 'Novo fornecedor'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar}>
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nome</label>
                <input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </div>
              <div className="field">
                <label>CNPJ</label>
                <input value={form.cnpj} onChange={(e) => setForm({ ...form, cnpj: e.target.value })} />
              </div>
              <div className="field">
                <label>Telefone</label>
                <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
              </div>
              <div className="field full">
                <label>E-mail</label>
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="field full">
                <label>Endereço</label>
                <input value={form.endereco} onChange={(e) => setForm({ ...form, endereco: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">{editando ? 'Salvar alterações' : 'Cadastrar fornecedor'}</button>
            </div>
          </form>
        </Modal>
      )}

      {dialogoConfirmacao}
    </>
  )
}
