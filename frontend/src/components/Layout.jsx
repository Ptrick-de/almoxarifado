import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from './Modal.jsx'
import { useAuth } from '../auth/AuthContext.jsx'

// Menu em árvore (como um explorador de arquivos). Cada grupo abre/fecha pela setinha;
// "ativo" lista os prefixos de rota que mantêm o grupo marcado e aberto.
const navItems = [
  { id: 'painel', to: '/', label: 'Painel', end: true, icon: IconGrid },
  {
    id: 'materiais', label: 'Materiais', icon: IconBox, to: '/materiais',
    ativo: ['/materiais', '/fornecedores', '/categorias'],
    filhos: [
      { to: '/materiais', label: 'Materiais' },
      { to: '/fornecedores', label: 'Fornecedores' },
      { to: '/categorias', label: 'Categorias' },
    ],
  },
  {
    id: 'movimentacao', label: 'Movimentação', icon: IconArrows, to: '/movimentacao',
    ativo: ['/movimentacao', '/documentos'],
    filhos: [
      { to: '/movimentacao', label: 'Visão geral', end: true },
      { to: '/movimentacao/entrada', label: 'Criar entrada' },
      { to: '/movimentacao/saida', label: 'Saída para consumo' },
      { to: '/movimentacao/emprestimos', label: 'Empréstimos' },
      { to: '/movimentacao/baixa', label: 'Perda ou avaria' },
    ],
  },
  {
    id: 'relatorios', label: 'Relatórios', icon: IconReport, to: '/relatorios',
    ativo: ['/relatorios'],
    filhos: [
      { to: '/relatorios?tipo=estoque', label: 'Posição de estoque', tipo: 'estoque' },
      { to: '/relatorios?tipo=movimentacoes', label: 'Movimentações', tipo: 'movimentacoes' },
      { to: '/relatorios?tipo=emprestimos', label: 'Empréstimos', tipo: 'emprestimos' },
    ],
  },
]

const CHAVE_ABERTOS = 'almoxarifado_menu_abertos'

function lerAbertos() {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_ABERTOS) || 'null')
    if (Array.isArray(salvo)) return salvo
  } catch (_) { /* sem armazenamento: usa o padrão */ }
  return ['materiais', 'movimentacao']
}

function noGrupo(item, pathname) {
  return item.ativo.some((p) => pathname === p || pathname.startsWith(p + '/'))
}

function MenuArvore({ pathname, search, admin }) {
  const [abertos, setAbertos] = useState(lerAbertos)

  // Ao navegar para dentro de um grupo (por link ou atalho), ele se abre sozinho.
  useEffect(() => {
    const atual = navItems.find((i) => i.filhos && noGrupo(i, pathname))
    if (atual) setAbertos((a) => (a.includes(atual.id) ? a : [...a, atual.id]))
  }, [pathname])

  useEffect(() => {
    try { localStorage.setItem(CHAVE_ABERTOS, JSON.stringify(abertos)) } catch (_) { /* ignora */ }
  }, [abertos])

  const alternar = (id) => setAbertos((a) => (a.includes(id) ? a.filter((x) => x !== id) : [...a, id]))

  return (
    <nav className="sidebar-nav" aria-label="Menu principal">
      {navItems.map((item) => {
        if (!item.filhos) {
          return (
            <NavLink key={item.id} to={item.to} end={item.end}
              className={() => (pathname === item.to ? 'active' : '')}>
              <item.icon className="icon" />
              {item.label}
            </NavLink>
          )
        }
        const aberto = abertos.includes(item.id)
        const ativo = noGrupo(item, pathname)
        return (
          <div key={item.id} className="tree-group">
            <div className={`tree-head${ativo ? ' active' : ''}`}>
              <NavLink to={item.to} className="tree-label" onClick={() => !aberto && alternar(item.id)}>
                <item.icon className="icon" />
                {item.label}
              </NavLink>
              <button type="button" className={`tree-toggle${aberto ? ' open' : ''}`}
                aria-expanded={aberto} aria-label={`${aberto ? 'Recolher' : 'Expandir'} ${item.label}`}
                onClick={() => alternar(item.id)}>
                <IconChevron />
              </button>
            </div>
            {aberto && (
              <div className="tree-children">
                {item.filhos.map((f) => {
                  const marcado = f.tipo
                    ? pathname === '/relatorios' && (new URLSearchParams(search).get('tipo') || 'estoque') === f.tipo
                    : f.end ? pathname === f.to : pathname === f.to || pathname.startsWith(f.to + '/')
                  return (
                    <NavLink key={f.to} to={f.to} className={() => `tree-child${marcado ? ' active' : ''}`}>
                      {f.label}
                    </NavLink>
                  )
                })}
              </div>
            )}
          </div>
        )
      })}
      {admin && (
        <NavLink to="/usuarios" className={() => (pathname === '/usuarios' ? 'active' : '')}>
          <IconUser className="icon" />
          Usuários
        </NavLink>
      )}
    </nav>
  )
}

export default function Layout() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname, search } = useLocation()
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
        <MenuArvore pathname={pathname} search={search} admin={usuario?.papel === 'ADMIN'} />
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

function IconChevron(props) {
  return (
    <svg viewBox="0 0 24 24" fill="none" width="14" height="14" {...props}>
      <path d="M9 6L15 12L9 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
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
