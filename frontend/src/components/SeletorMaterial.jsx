import { useMemo, useState } from 'react'
import { nomeCompletoMaterial } from '../utils/formatos.js'

// Seleção de material com busca por texto (código, nome, marca, modelo, C.A.).
// exigirSaldo: desabilita itens sem saldo disponível (retiradas, empréstimos e baixas).
export default function SeletorMaterial({ materiais, value, onChange, exigirSaldo = false, rotulo = 'Material' }) {
  const [filtro, setFiltro] = useState('')

  const opcoes = useMemo(() => {
    const termo = filtro.trim().toLowerCase()
    return materiais.filter((m) => {
      if (m.ativo === false && String(m.id) !== String(value)) return false
      if (!termo) return true
      return [m.codigo, m.nome, m.marca, m.modelo, m.ca].join(' ').toLowerCase().includes(termo)
    })
  }, [materiais, filtro, value])

  return (
    <div className="field full">
      <label>{rotulo}</label>
      <input
        type="search"
        placeholder="Digite para buscar por código, nome, marca, modelo ou C.A."
        aria-label="Buscar material"
        value={filtro}
        onChange={(e) => setFiltro(e.target.value)}
      />
      <select required value={value} onChange={(e) => onChange(e.target.value)} aria-label={rotulo}>
        <option value="">{opcoes.length ? 'Selecione um material' : 'Nenhum material encontrado'}</option>
        {opcoes.map((m) => {
          const semSaldo = exigirSaldo && m.quantidadeAtual <= 0
          return (
            <option key={m.id} value={m.id} disabled={semSaldo}>
              {m.codigo} — {nomeCompletoMaterial(m)}{m.ca ? ` (C.A. ${m.ca})` : ''} · disponível: {m.quantidadeAtual} {m.unidade}
              {semSaldo ? (m.quantidadeEmprestada > 0 ? ' — emprestado' : ' — sem estoque') : ''}
            </option>
          )
        })}
      </select>
    </div>
  )
}
