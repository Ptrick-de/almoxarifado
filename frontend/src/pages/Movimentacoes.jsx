import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from '../components/Modal.jsx'

const TIPOS = [
  { valor: 'ENTRADA', label: 'Entrada', classe: 'active-entrada' },
  { valor: 'SAIDA', label: 'Saída', classe: 'active-saida' },
  { valor: 'PERDA', label: 'Perda', classe: 'active-perda' },
  { valor: 'AVARIA', label: 'Avaria', classe: 'active-avaria' },
]

const RECURSOS_TIPO = {
  ENTRADA: { badge: 'badge-entrada', label: 'Entrada' },
  SAIDA: { badge: 'badge-saida', label: 'Saída' },
  PERDA: { badge: 'badge-perda', label: 'Perda' },
  AVARIA: { badge: 'badge-avaria', label: 'Avaria' },
}

export default function Movimentacoes() {
  const [movimentacoes, setMovimentacoes] = useState([])
  const [materiais, setMateriais] = useState([])
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [tipo, setTipo] = useState('ENTRADA')
  const [form, setForm] = useState({ materialId: '', quantidade: '', responsavel: '', observacao: '' })

  useEffect(() => { carregarTudo() }, [])

  function carregarTudo() {
    Promise.all([api.listarMovimentacoes(), api.listarMateriais()])
      .then(([mv, m]) => { setMovimentacoes(mv); setMateriais(m) })
      .catch((e) => setErro(mensagemDeErro(e)))
  }

  function abrirModal(tipoInicial) {
    setTipo(tipoInicial)
    setForm({ materialId: '', quantidade: '', responsavel: '', observacao: '' })
    setErro('')
    setModalAberto(true)
  }

  const motivoObrigatorio = tipo === 'PERDA' || tipo === 'AVARIA'

  async function registrar(e) {
    e.preventDefault()
    setErro('')
    if (motivoObrigatorio && !form.observacao.trim()) {
      setErro(`Descreva o motivo da ${tipo === 'PERDA' ? 'perda' : 'avaria'} antes de continuar.`)
      return
    }
    try {
      await api.registrarMovimentacao({
        materialId: Number(form.materialId),
        tipo,
        quantidade: Number(form.quantidade),
        responsavel: form.responsavel,
        observacao: form.observacao,
      })
      setModalAberto(false)
      carregarTudo()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    }
  }

  function formatarData(dataStr) {
    return new Date(dataStr).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  }

  const materialSelecionado = materiais.find((m) => String(m.id) === String(form.materialId))

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Movimentações</h1>
          <div className="topbar-desc">Entradas, saídas, perdas e avarias de estoque</div>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary" onClick={() => abrirModal('AVARIA')}>Registrar avaria</button>
          <button className="btn btn-secondary" onClick={() => abrirModal('PERDA')}>Registrar perda</button>
          <button className="btn btn-secondary" onClick={() => abrirModal('SAIDA')}>Registrar saída</button>
          <button className="btn btn-primary" onClick={() => abrirModal('ENTRADA')}>Registrar entrada</button>
        </div>
      </header>

      <div className="content">
        {erro && !modalAberto && <div className="error-banner">{erro}</div>}

        <div className="panel">
          <div className="panel-body">
            {movimentacoes.length === 0 ? (
              <div className="empty-state">
                <div className="empty-title">Nenhuma movimentação registrada</div>
                Registre a primeira entrada, saída, perda ou avaria de material.
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Código</th>
                    <th>Material</th>
                    <th>Tipo</th>
                    <th>Quantidade</th>
                    <th>Responsável</th>
                    <th>Observação</th>
                  </tr>
                </thead>
                <tbody>
                  {movimentacoes.map((mv) => {
                    const recurso = RECURSOS_TIPO[mv.tipo] || { badge: '', label: mv.tipo }
                    return (
                      <tr key={mv.id}>
                        <td>{formatarData(mv.dataMovimentacao)}</td>
                        <td className="mono">{mv.material?.codigo}</td>
                        <td>{mv.material?.nome}</td>
                        <td><span className={`badge ${recurso.badge}`}>{recurso.label}</span></td>
                        <td>{mv.quantidade}</td>
                        <td>{mv.responsavel || '—'}</td>
                        <td>{mv.observacao || '—'}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {modalAberto && (
        <Modal title="Registrar movimentação" onClose={() => setModalAberto(false)}>
          <form onSubmit={registrar}>
            <div style={{ padding: '20px 20px 0' }}>
              <div className="movement-toggle">
                {TIPOS.map((t) => (
                  <button
                    key={t.valor}
                    type="button"
                    className={tipo === t.valor ? t.classe : ''}
                    onClick={() => setTipo(t.valor)}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            </div>

            {erro && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro}</div>}

            <div className="form-grid">
              <div className="field full">
                <label>Material</label>
                <select required value={form.materialId} onChange={(e) => setForm({ ...form, materialId: e.target.value })}>
                  <option value="">Selecione um material</option>
                  {materiais.map((m) => (
                    <option key={m.id} value={m.id}>{m.codigo} — {m.nome} (estoque: {m.quantidadeAtual} {m.unidade})</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Quantidade {materialSelecionado ? `(${materialSelecionado.unidade})` : ''}</label>
                <input required type="number" min="1" value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })} />
              </div>
              <div className="field">
                <label>Responsável</label>
                <input value={form.responsavel} onChange={(e) => setForm({ ...form, responsavel: e.target.value })} />
              </div>
              <div className="field full">
                <label>
                  {motivoObrigatorio
                    ? `Motivo da ${tipo === 'PERDA' ? 'perda' : 'avaria'} (obrigatório)`
                    : 'Observação'}
                </label>
                <textarea
                  required={motivoObrigatorio}
                  placeholder={motivoObrigatorio ? 'Ex: caixa molhada no transporte, item quebrado ao manusear, extraviado na conferência...' : ''}
                  value={form.observacao}
                  onChange={(e) => setForm({ ...form, observacao: e.target.value })}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className="btn btn-primary">
                {tipo === 'ENTRADA' && 'Confirmar entrada'}
                {tipo === 'SAIDA' && 'Confirmar saída'}
                {tipo === 'PERDA' && 'Registrar perda'}
                {tipo === 'AVARIA' && 'Registrar avaria'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
