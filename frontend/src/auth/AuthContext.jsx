import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../api/api.js'
import { getToken, setToken, guardarAvisoLogin, CHAVE_TOKEN, SESSAO_EXPIRADA_EVENTO } from '../api/api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    async function iniciar() {
      const token = getToken()
      if (token) {
        try {
          const dados = await api.buscarUsuarioLogado()
          setUsuario({
            id: dados.id,
            nome: dados.nome,
            usuario: dados.usuario,
            papel: dados.papel,
            empresaNome: dados.empresa?.nome,
          })
        } catch (_) {
          setToken(null)
          setUsuario(null)
        }
      }
      setCarregando(false)
    }
    iniciar()

    function aoExpirar() { setUsuario(null) }
    // Saiu (ou foi desconectado) em outra aba do mesmo navegador: esta aba também deve sair.
    function aoMudarArmazenamento(e) {
      if (e.key === CHAVE_TOKEN && !e.newValue) setUsuario(null)
    }
    window.addEventListener(SESSAO_EXPIRADA_EVENTO, aoExpirar)
    window.addEventListener('storage', aoMudarArmazenamento)
    return () => {
      window.removeEventListener(SESSAO_EXPIRADA_EVENTO, aoExpirar)
      window.removeEventListener('storage', aoMudarArmazenamento)
    }
  }, [])

  async function login(usuario, senha) {
    const resposta = await api.login(usuario, senha)
    setToken(resposta.token)
    setUsuario({
      id: resposta.id,
      nome: resposta.nome,
      usuario: resposta.usuario,
      papel: resposta.papel,
      empresaNome: resposta.empresaNome,
    })
  }

  // motivo (opcional): texto exibido na tela de login, ex.: "encerrada por inatividade".
  function logout(motivo) {
    if (motivo) guardarAvisoLogin(motivo)
    setToken(null)
    setUsuario(null)
  }

  return (
    <AuthContext.Provider value={{ usuario, carregando, login, logout, autenticado: !!usuario }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
