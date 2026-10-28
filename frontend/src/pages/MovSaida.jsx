import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import AbasSecao, { ABAS_MOVIMENTACAO } from '../components/AbasSecao.jsx'
import HistoricoMovimentacoes from '../components/HistoricoMovimentacoes.jsx'
import SeletorMaterial from '../components/SeletorMaterial.jsx'
import { useMateriais } from '../hooks/useMateriais.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { hojeISO, novaChave } from '../utils/formatos.js'

export default function MovSaida() {
  const { usuario } = useAuth()
  const { materiais, recarregar, erroCarga } = useMateriais()
  const vazio = () => ({
    materialId: '', quantidade: '', dataMovimento: hojeISO(), destinatario: '', setor: '',
    responsavel: usuario?.nome || '', observacao: '',
  })
  const [form, setForm] = useState(vazio)
  const [chave, setChave] = useState(novaChave)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [registrada, setRegistrada] = useState(null) // { id, texto }
  const [versao, setVersao] = useState(0)

  const material = materiais.find((m) => String(m.id) === String(form.materialId))

  async function registrar(e) {
    e.preventDefault()
    if (enviando) return
    setErro(''); setRegistrada(null)
    setEnviando(true)
    try {
      const mov = await api.registrarMovimentacao({
        tipo: 'SAIDA',
        materialId: Number(form.materialId),
        quantidade: Number(form.quantidade),
        dataMovimento: form.dataMovimento || null,
        destinatario: form.destinatario,
        setor: form.setor,
        responsavel: form.responsavel,
        observacao: form.observacao,
        chaveIdempotencia: chave,
      })
      setRegistrada({ id: mov.id, texto: `Saída registrada: ${form.quantidade} ${material?.unidade || ''} de ${material?.nome || 'material'} para ${form.destinatario}.` })
      setForm(vazio())
      setChave(novaChave())
      setVersao((v) => v + 1)
      recarregar()
    } catch (e2) {
      setErro(mensagemDeErro(e2))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <h1>Saída para uso e consumo</h1>
          <div className="topbar-desc">Material entregue para uso; não retorna ao estoque. Para itens que voltam, use Empréstimos.</div>
        </div>
      </header>
      <AbasSecao abas={ABAS_MOVIMENTACAO} />

      <div className="content">
        <div className="panel" style={{ marginBottom: 24 }}>
          <form onSubmit={registrar}>
            {(erro || erroCarga) && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro || erroCarga}</div>}
            {registrada && (
              <div style={{ margin: '16px 20px 0', display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }} className="success-banner">
                <span>{registrada.texto}</span>
                <Link className="btn btn-primary btn-sm" to={`/documentos/ficha/${registrada.id}`}>Ficha de Entrega de Material</Link>
              </div>
            )}

            <div className="form-section">Material retirado</div>
            <div className="form-grid">
              <SeletorMaterial materiais={materiais} value={form.materialId} onChange={(v) => setForm({ ...form, materialId: v })} exigirSaldo />
              <div className="field">
                <label>Quantidade retirada {material ? `(${material.unidade})` : ''} *</label>
                <input
                  required type="number" min="1" step="1" max={material ? material.quantidadeAtual : undefined}
                  value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })}
                />
                {material && (
                  <span className="field-hint">
                    Disponível: {material.quantidadeAtual} {material.unidade}
                    {material.quantidadeEmprestada > 0 ? ` (${material.quantidadeEmprestada} emprestados, fora do estoque)` : ''}
                  </span>
                )}
              </div>
              <div className="field">
                <label>Data da saída *</label>
                <input required type="date" max={hojeISO()} value={form.dataMovimento} onChange={(e) => setForm({ ...form, dataMovimento: e.target.value })} />
              </div>
            </div>

            <div className="form-section">Quem recebeu</div>
            <div className="form-grid">
              <div className="field">
                <label>Pessoa ou setor que recebeu *</label>
                <input required maxLength={150} value={form.destinatario} onChange={(e) => setForm({ ...form, destinatario: e.target.value })} />
              </div>
              <div className="field">
                <label>Setor / departamento</label>
                <input maxLength={100} value={form.setor} onChange={(e) => setForm({ ...form, setor: e.target.value })} />
              </div>
              <div className="field">
                <label>Responsável pela entrega *</label>
                <input required maxLength={150} value={form.responsavel} onChange={(e) => setForm({ ...form, responsavel: e.target.value })} />
              </div>
              <div className="field full">
                <label>Observações</label>
                <textarea maxLength={1000} value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => { setForm(vazio()); setErro(''); setRegistrada(null) }}>Limpar</button>
              <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Registrando...' : 'Confirmar saída'}</button>
            </div>
          </form>
        </div>

        <HistoricoMovimentacoes tipos={['SAIDA']} recarregar={versao} titulo="Saídas para consumo" />
      </div>
    </>
  )
}
