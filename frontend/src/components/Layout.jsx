import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from './Modal.jsx'
import { useAuth } from '../auth/AuthContext.jsx'

// Abas principais. "ativo" lista os prefixos de rota que mantêm a aba marcada
// (Fornecedores e Categorias ficam dentro de Materiais).
const navItems = [
  { to: '/', label: 'Painel', end: true, icon: IconGrid, ativo: ['/'] },
  { to: '/materiais', label: 'Materiais', icon: IconBox, ativo: ['/materiais', '/fornecedores', '/categorias'] },
  { to: '/movimentacao', label: 'Movimentação', icon: IconArrows, ativo: ['/movimentacao', '/documentos'] },
  { to: '/relatorios', label: 'Relatórios', icon: IconReport, ativo: ['/relatorios'] },
]

export default function Layout() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [senhaAberta, setSenhaAberta] = useState(false)
  const [senhaForm, setSenhaForm] = useState({ senhaAtual: '', novaSenha: '', confirmar: '' })
  const [senhaErro, setSenhaErro] = useState('')
  const [senhaOk, setSenhaOk] = useState(false)

  useEffect(() => {
    document.title = usuario?.empresaNome ? `${usuario.empresaNome} — Almoxarifado` : 'Almoxarifado'
  }, [usuario])

  function sair() {
    logout()
    navigate('/login')
  }

  function abrirSenha() {
    setSenhaForm({ senhaAtual: '', novaSenha: '', confirmar: '' })
    setSenhaErro('')
    setSenhaOk(false)
    setSenhaAberta(true)
  }

  async function trocarSenha(e) {
    e.preventDefault()
    setSenhaErro('')
    if (senhaForm.novaSenha !== senhaForm.confirmar) {
      setSenhaErro('A confirmação não confere com a nova senha')
      return
    }
    try {
      await api.alterarMinhaSenha(senhaForm.senhaAtual, senhaForm.novaSenha)
      setSenhaOk(true)
    } catch (e2) {
      setSenhaErro(mensagemDeErro(e2))
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="mark">{usuario?.empresaNome || 'Almoxarifado'}</div>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={() => ((item.end ? pathname === '/' : item.ativo.some((p) => pathname === p || pathname.startsWith(p + '/'))) ? 'active' : '')}
            >
              <item.icon className="icon" />
              {item.label}
            </NavLink>
          ))}
          {usuario?.papel === 'ADMIN' && (
            <NavLink to="/usuarios" className={({ isActive }) => (isActive ? 'active' : '')}>
              <IconUser className="icon" />
              Usuários
            </NavLink>
          )}
        </nav>
        <div className="sidebar-footer">
          <div style={{ color: '#C7C4B8', fontWeight: 600, marginBottom: 2 }}>{usuario?.nome}</div>
          <div style={{ marginBottom: 12 }}>{usuario?.papel === 'ADMIN' ? 'Administrador' : 'Operador'}</div>
          <div style={{ display: 'flex', gap: 14 }}>
            <button className="link-btn" onClick={abrirSenha} style={{ color: '#9A9F93' }}>Alterar senha</button>
            <button className="link-btn" onClick={sair} style={{ color: '#9A9F93' }}>Sair</button>
          </div>
        </div>
      </aside>
      <div className="main-area">
        <Outlet />
      </div>

      {senhaAberta && (
        <Modal title="Alterar minha senha" onClose={() => setSenhaAberta(false)}>
          {senhaOk ? (
            <>
              <div style={{ padding: 20 }}>
                <div className="success-banner" style={{ marginBottom: 0 }}>Senha alterada com sucesso.</div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-primary" onClick={() => setSenhaAberta(false)}>Fechar</button>
              </div>
            </>
          ) : (
            <form onSubmit={trocarSenha}>
              {senhaErro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{senhaErro}</div>}
              <div className="form-grid">
                <div className="field full">
                  <label>Senha atual</label>
                  <input required type="password" autoFocus value={senhaForm.senhaAtual} onChange={(e) => setSenhaForm({ ...senhaForm, senhaAtual: e.target.value })} />
                </div>
                <div className="field">
                  <label>Nova senha</label>
                  <input required type="password" minLength={6} value={senhaForm.novaSenha} onChange={(e) => setSenhaForm({ ...senhaForm, novaSenha: e.target.value })} />
                </div>
                <div className="field">
                  <label>Confirmar nova senha</label>
                  <input required type="password" minLength={6} value={senhaForm.confirmar} onChange={(e) => setSenhaForm({ ...senhaForm, confirmar: e.target.value })} />
                </div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setSenhaAberta(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary">Salvar nova senha</button>
              </div>
            </form>
          )}
        </Modal>
      )}
    </div>
  )
}

function IconUser(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <circle cx="12" cy="8" r="3.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="M4.5 20C5.5 16.5 8.4 14.5 12 14.5C15.6 14.5 18.5 16.5 19.5 20" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

function IconGrid(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <rect x="3" y="3" width="8" height="8" rx="1" stroke="currentColor" strokeWidth="1.6" />
      <rect x="13" y="3" width="8" height="8" rx="1" stroke="currentColor" strokeWidth="1.6" />
      <rect x="3" y="13" width="8" height="8" rx="1" stroke="currentColor" strokeWidth="1.6" />
      <rect x="13" y="13" width="8" height="8" rx="1" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  )
}

function IconBox(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <path d="M3 7L12 3L21 7V17L12 21L3 17V7Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M3 7L12 11L21 7" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M12 11V21" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  )
}

function IconArrows(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <path d="M4 8H17L14 5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M20 16H7L10 19" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconReport(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <path d="M6 3H14L19 8V21H6V3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M14 3V8H19" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M9 13H16M9 17H14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

function IconTag(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <path d="M11.5 3H4V10.5L14 20.5L21.5 13L11.5 3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <circle cx="8" cy="7.5" r="1.4" fill="currentColor" />
    </svg>
  )
}

function IconTruck(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <path d="M2 6H14V16H2V6Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M14 10H18L21 13V16H14V10Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <circle cx="6" cy="18" r="1.8" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="17.5" cy="18" r="1.8" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  )
}
