export default function Modal({ title, onClose, children, larg = false }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className={`modal${larg ? ' modal-largo' : ''}`} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Fechar">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}
