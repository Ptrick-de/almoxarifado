import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Caixa de confirmação no padrão visual do sistema (substitui o window.confirm do navegador).
 *
 * Uso numa tela:
 *   const [confirmar, dialogoConfirmacao] = useConfirmacao()
 *   ...
 *   if (!(await confirmar({ titulo: 'Excluir fornecedor', mensagem: <>...</>, rotuloConfirmar: 'Excluir', perigo: true }))) return
 *   ...
 *   return (<> ... {dialogoConfirmacao} </>)
 */
export function useConfirmacao() {
  const [pedido, setPedido] = useState(null)
  const resolver = useRef(null)

  const confirmar = useCallback((opcoes) => new Promise((resolve) => {
    resolver.current = resolve
    setPedido(opcoes)
  }), [])

  const responder = useCallback((valor) => {
    if (resolver.current) resolver.current(valor)
    resolver.current = null
    setPedido(null)
  }, [])

  const dialogo = pedido ? <ConfirmDialog {...pedido} onResponder={responder} /> : null
  return [confirmar, dialogo]
}

function ConfirmDialog({
  titulo = 'Confirmar ação',
  mensagem,
  detalhe,
  rotuloConfirmar = 'Confirmar',
  rotuloCancelar = 'Cancelar',
  perigo = false,
  onResponder,
}) {
  const botaoCancelar = useRef(null)

  useEffect(() => {
    // Foco no "Cancelar": evita excluir sem querer apertando Enter.
    botaoCancelar.current?.focus()
    function aoTeclar(e) {
      if (e.key === 'Escape') onResponder(false)
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [onResponder])

  return (
    <div className="modal-backdrop" onClick={() => onResponder(false)}>
      <div
        className="modal modal-confirm"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="confirm-corpo">
          <div className={`confirm-icone ${perigo ? 'perigo' : ''}`} aria-hidden="true">
            {perigo ? (
              <svg viewBox="0 0 24 24" fill="none">
                <path d="M4 7H20M9 7V4.5H15V7M6.5 7L7.5 20H16.5L17.5 7M10 11V16M14 11V16" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.7" />
                <path d="M12 8V12.5M12 16V16.01" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
              </svg>
            )}
          </div>
          <div>
            <h2 id="confirm-titulo" className="confirm-titulo">{titulo}</h2>
            <div className="confirm-mensagem">{mensagem}</div>
            {detalhe && <div className="confirm-detalhe">{detalhe}</div>}
          </div>
        </div>
        <div className="form-actions">
          <button type="button" ref={botaoCancelar} className="btn btn-secondary" onClick={() => onResponder(false)}>
            {rotuloCancelar}
          </button>
          <button type="button" className={`btn ${perigo ? 'btn-danger-solid' : 'btn-primary'}`} onClick={() => onResponder(true)}>
            {rotuloConfirmar}
          </button>
        </div>
      </div>
    </div>
  )
}
