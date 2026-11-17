import { useEffect, useRef, useState } from 'react'
import { api } from '../api/api.js'
import { useAuth } from '../auth/AuthContext.jsx'
import Modal from './Modal.jsx'

// Quanto tempo a pessoa pode ficar sem mexer no sistema antes de ser desconectada.
// Pode ser mudado sem alterar o código: crie frontend/.env com VITE_INATIVIDADE_MINUTOS=10
const LIMITE_MIN = Number(import.meta.env.VITE_INATIVIDADE_MINUTOS) || 15
const LIMITE_MS = LIMITE_MIN * 60 * 1000
// Aviso "Você ainda está aí?" nos últimos segundos (no máximo 60).
const AVISO_SEG = Math.min(60, Math.max(3, Math.floor(LIMITE_MS / 1000 / 3)))
// Enquanto a pessoa trabalha sem chamar a API (ex.: preenchendo um formulário longo),
// renovamos a sessão no servidor de tempos em tempos.
const RENOVAR_A_CADA_MS = 4 * 60 * 1000
const CHAVE_ATIVIDADE = 'almoxarifado_ultima_atividade'

function lerAtividade() {
  try {
    const v = Number(localStorage.getItem(CHAVE_ATIVIDADE))
    return v > 0 ? v : null
  } catch (_) {
    return null
  }
}

function gravarAtividade(quando) {
  try { localStorage.setItem(CHAVE_ATIVIDADE, String(quando)) } catch (_) { /* sem armazenamento: vale só esta aba */ }
}

// Desconecta quem ficou parado e avisa antes. A atividade é compartilhada entre abas do mesmo
// navegador (mexer em uma aba mantém as outras conectadas) e contada pelo relógio, então um
// computador que "dormiu" também é desconectado ao acordar.
export default function InatividadeAviso() {
  const { logout } = useAuth()
  const ultimaAtividade = useRef(Date.now())
  const ultimaRenovacao = useRef(Date.now())
  const avisando = useRef(false)
  const [restante, setRestante] = useState(null)

  function continuarConectado() {
    const agora = Date.now()
    ultimaAtividade.current = agora
    ultimaRenovacao.current = agora
    gravarAtividade(agora)
    avisando.current = false
    setRestante(null)
    api.buscarUsuarioLogado().catch(() => {})
  }

  useEffect(() => {
    ultimaAtividade.current = Date.now()
    gravarAtividade(ultimaAtividade.current)

    function registrarAtividade() {
      if (avisando.current) return // durante o aviso, só o botão conta
      const agora = Date.now()
      if (agora - ultimaAtividade.current < 1000) return
      ultimaAtividade.current = agora
      gravarAtividade(agora)
    }
    const eventos = ['mousemove', 'mousedown', 'keydown', 'wheel', 'scroll', 'touchstart']
    eventos.forEach((e) => window.addEventListener(e, registrarAtividade, { passive: true, capture: true }))

    const relogio = setInterval(() => {
      const agora = Date.now()
      const deOutraAba = lerAtividade()
      if (deOutraAba && deOutraAba > ultimaAtividade.current) ultimaAtividade.current = deOutraAba

      const parado = agora - ultimaAtividade.current
      const faltam = Math.ceil((LIMITE_MS - parado) / 1000)

      if (faltam <= 0) {
        clearInterval(relogio)
        logout('Sua sessão foi encerrada por inatividade. Entre novamente para continuar.')
        return
      }
      if (faltam <= AVISO_SEG) {
        avisando.current = true
        setRestante(faltam)
      } else {
        avisando.current = false
        setRestante(null)
        if (parado < 60_000 && agora - ultimaRenovacao.current >= RENOVAR_A_CADA_MS) {
          ultimaRenovacao.current = agora
          api.buscarUsuarioLogado().catch(() => {})
        }
      }
    }, 1000)

    return () => {
      clearInterval(relogio)
      eventos.forEach((e) => window.removeEventListener(e, registrarAtividade, { capture: true }))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (restante === null) return null

  return (
    <Modal title="Você ainda está aí?" onClose={continuarConectado}>
      <div style={{ padding: 20 }}>
        <p style={{ margin: '0 0 8px' }}>
          Por segurança, sua sessão será encerrada em <strong>{restante} {restante === 1 ? 'segundo' : 'segundos'}</strong> por falta de atividade.
        </p>
        <p style={{ margin: 0, color: 'var(--ink-soft)' }}>O que você estava preenchendo e ainda não salvou será perdido.</p>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={() => logout()}>Sair agora</button>
        <button type="button" className="btn btn-primary" autoFocus onClick={continuarConectado}>Continuar conectado</button>
      </div>
    </Modal>
  )
}
