import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { mensagemDeErro } from '../api/api.js'
import { useAuth } from '../auth/AuthContext.jsx'

export default function Login() {
  const { login, autenticado } = useAuth()
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [esqueci, setEsqueci] = useState(false)

  if (autenticado) return <Navigate to="/" replace />

  async function entrar(e) {
    e.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await login(usuario, senha)
      navigate('/')
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div style={styles.tela}>
      <div style={styles.cartao}>
        <div style={styles.titulo}>Acessar sistema</div>

        <form onSubmit={entrar} style={{ marginTop: 24 }}>
          {erro && <div className="error-banner">{erro}</div>}

          <div className="field" style={{ marginBottom: 16 }}>
            <label>Usuário</label>
            <input
              type="text"
              required
              autoFocus
              autoCapitalize="off"
              autoCorrect="off"
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
              placeholder="seu.usuario"
            />
          </div>
          <div className="field" style={{ marginBottom: 24 }}>
            <label>Senha</label>
            <input
              type="password"
              required
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={enviando} style={{ width: '100%', justifyContent: 'center' }}>
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>

          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <button type="button" className="link-btn" style={{ color: 'var(--ink-soft)' }} onClick={() => setEsqueci(!esqueci)}>
              Esqueci minha senha
            </button>
          </div>
          {esqueci && (
            <div style={styles.ajuda}>
              Procure o <strong>administrador</strong> da sua empresa. Ele define uma nova senha para você
              em <strong>Usuários → Redefinir senha</strong>. Depois de entrar, você pode trocá-la em
              <strong> Alterar senha</strong>.
            </div>
          )}
        </form>
      </div>
    </div>
  )
}

const styles = {
  tela: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'var(--graphite)',
    padding: 20,
  },
  cartao: {
    width: '100%',
    maxWidth: 360,
    background: 'var(--surface)',
    border: '1px solid var(--line)',
    padding: '32px 28px',
  },
  ajuda: {
    marginTop: 12,
    padding: '12px 14px',
    background: 'var(--bg, #F4F3EE)',
    border: '1px solid var(--line)',
    fontSize: 13,
    lineHeight: 1.5,
    color: 'var(--ink-soft)',
  },
  titulo: {
    fontFamily: 'var(--font-display)',
    fontSize: 22,
    fontWeight: 600,
  },
}
