// Barra de filtros padrão das telas de consulta: campos combináveis + "Limpar filtros".
// campos: [{ nome, rotulo, tipo: 'texto' | 'select' | 'data', opcoes?: [{valor, rotulo}], placeholder? }]
export default function Filtros({ campos, valores, onChange, onLimpar, total, children }) {
  const algumPreenchido = Object.values(valores).some((v) => v !== '' && v != null)
  return (
    <div className="filtros">
      <div className="filtros-grid">
        {campos.map((c) => (
          <div className="field" key={c.nome}>
            <label htmlFor={`filtro-${c.nome}`}>{c.rotulo}</label>
            {c.tipo === 'select' ? (
              <select id={`filtro-${c.nome}`} value={valores[c.nome] ?? ''} onChange={(e) => onChange(c.nome, e.target.value)}>
                <option value="">Todos</option>
                {c.opcoes.map((o) => <option key={o.valor} value={o.valor}>{o.rotulo}</option>)}
              </select>
            ) : (
              <input
                id={`filtro-${c.nome}`}
                type={c.tipo === 'data' ? 'date' : 'text'}
                placeholder={c.placeholder || ''}
                value={valores[c.nome] ?? ''}
                onChange={(e) => onChange(c.nome, e.target.value)}
              />
            )}
          </div>
        ))}
      </div>
      <div className="filtros-acoes">
        <button type="button" className="btn btn-secondary btn-sm" onClick={onLimpar} disabled={!algumPreenchido}>
          Limpar filtros
        </button>
        {children}
        {total != null && <span className="filtros-contagem">{total} {total === 1 ? 'resultado' : 'resultados'}</span>}
      </div>
    </div>
  )
}
