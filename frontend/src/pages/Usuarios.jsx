import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import { useAuth } from '../auth/AuthContext.jsx'
import Modal from '../components/Modal.jsx'
import { useConfirmacao } from '../components/ConfirmDialog.jsx'

const VAZIO = { nome: '', usuario: '', senha: '', papel: 'OPERADOR' }

export default function Usuarios() {
  const [confirmar, dialogoConfirmacao] = useConfirmacao()
  const { usuario: usuarioLogado } = useAuth()
  const [usuarios, setUsuarios] = useState([])
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [form, setForm] = useState(VAZIO)
  const [alvoSenha, setAlvoSenha] = useState(null)
  const [novaSenha, setNovaSenha] = useState('')
  const [sucesso, setSucesso] = useState('')

  useEffect(() => { carregar() }, [])

  function carregar() {
    api.listarUsuarios().then(setUsuarios).catch((e) => setErro(mensagemDeErro(e)))
  }

  function abrirNovo() { setForm(VAZIO); setErro(''); setModalAberto(true) }

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      await api.criarUsuario(form)
      setModalAberto(false)
      carregar()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    }
  }

  function abrirRedefinir(u) { setAlvoSenha(u); setNovaSenha(''); setErro(''); setSucesso('') }

  async function redefinirSenha(e) {
    e.preventDefault()
    setErro('')
    try {
      await api.redefinirSenhaUsuario(alvoSenha.id, novaSenha)
      setSucesso(`Senha de "${alvoSenha.nome}" redefinida. Informe a nova senha ao usuário.`)
      setAlvoSenha(null)
    } catch (e2) { setErro(mensagemDeErro(e2)) }
  }

  async function alternarStatus(u) {
    if (u.ativo) {
      const confirmado = await confirmar({
        titulo: 'Desativar usuário',
        mensagem: <>Você está prestes a desativar o usuário <strong>{u.nome}</strong>.</>,
        detalhe: 'Ele não conseguirá mais entrar no sistema até ser reativado.',
        rotuloConfirmar: 'Desativar',
        perigo: true,
      })
      if (!confirmado) return
    }
    setErro('')
    try {
      await api.alterarStatusUsuario(u.id, !u.ativo)
      carregar()
    } catch (e) { setErro(mensagemDeErro(e)) }
  }

  async function excluir(u) {
    if (!(await confirmar({
      titulo: 'Excluir usuário',
      mensagem: <>Você está prestes a excluir o usuário <strong>{u.nome}</strong>.</>,
      detalhe: 'Esta ação não pode ser desfeita. Ele perderá o acesso ao sistema.',
      rotuloConfirmar: 'Excluir',
      perigo: true,
    }))) return
    setErro('')
    try {
      await api.excluirUsuario(u.id)
      carregar()
    } catch (e) { setErro(mensagemDeErro(e)) }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Usuários</h1>
          <div className="topbar-desc">Quem pode acessar o sistema da empresa</div>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>+ Novo usuário</button>
      </header>

      <div className="content">
        {erro && !modalAberto && !alvoSenha && <div className="error-banner">{erro}</div>}
        {sucesso && <div className="success-banner">{sucesso}</div>}

        <div className="panel">
          <div className="panel-body">
            <table>
              <thead>
                <tr><th>Nome</th><th>Usuário</th><th>Papel</th><th>Status</th><th></th></tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td>{u.nome}</td>
                    <td className="mono">{u.usuario}</td>
                    <td><span className="tag-pill">{u.papel === 'ADMIN' ? 'Administrador' : 'Operador'}</span></td>
                    <td>
                      <span className={`badge ${u.ativo ? 'badge-ok' : 'badge-out'}`}>{u.ativo ? 'Ativo' : 'Desativado'}</span>
                    </td>
                    <td>
                      <div className="row-actions">
                        <button className="btn btn-secondary btn-sm" onClick={() => abrirRedefinir(u)}>Redefinir senha</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => alternarStatus(u)}>
                          {u.ativo ? 'Desativar' : 'Reativar'}
                        </button>
                        {usuarioLogado.id !== u.id && (
                          <button className="btn btn-danger btn-sm" onClick={() => excluir(u)}>Excluir</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {modalAberto && (
        <Modal title="Novo usuário" onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar}>
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nome</label>
                <input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
              </div>
              <div className="field full">
                <label>Usuário</label>
                <input required minLength={3} autoCapitalize="off" autoCorrect="off" value={form.usuario} onChange={(e) => setForm({ ...form, usuario: e.target.value })} />
              </div>
              <div className="field">
                <label>Senha provisória</label>
                <input required type="password" minLength={6} value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
              </div>
              <div className="field">
                <label>Papel</label>
                <select value={form.papel} onChange={(e) => setForm({ ...form, papel: e.target.value })}>
                  <option value="OPERADOR">Operador</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">Cadastrar usuário</button>
            </div>
          </form>
        </Modal>
      )}

      {alvoSenha && (
        <Modal title={`Redefinir senha de ${alvoSenha.nome}`} onClose={() => setAlvoSenha(null)}>
          <form onSubmit={redefinirSenha}>
            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}
            <div className="form-grid">
              <div className="field full">
                <label>Nova senha</label>
                <input required type="password" minLength={6} autoFocus value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setAlvoSenha(null)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">Redefinir senha</button>
            </div>
          </form>
        </Modal>
      )}

      {dialogoConfirmacao}
    </>
  )
}
