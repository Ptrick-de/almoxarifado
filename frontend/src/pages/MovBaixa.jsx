import { useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import AbasSecao, { ABAS_MOVIMENTACAO } from '../components/AbasSecao.jsx'
import HistoricoMovimentacoes from '../components/HistoricoMovimentacoes.jsx'
import SeletorMaterial from '../components/SeletorMaterial.jsx'
import { useMateriais } from '../hooks/useMateriais.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { hojeISO, novaChave } from '../utils/formatos.js'

export default function MovBaixa() {
  const { usuario } = useAuth()
  const { materiais, recarregar, erroCarga } = useMateriais()
  const vazio = () => ({
    tipo: 'PERDA', materialId: '', quantidade: '', dataMovimento: hojeISO(),
    responsavel: usuario?.nome || '', descricao: '', observacao: '',
  })
  const [form, setForm] = useState(vazio)
  const [chave, setChave] = useState(novaChave)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [versao, setVersao] = useState(0)

  const material = materiais.find((m) => String(m.id) === String(form.materialId))
  const rotulo = form.tipo === 'PERDA' ? 'perda' : 'avaria'

  async function registrar(e) {
    e.preventDefault()
    if (enviando) return
    setErro(''); setSucesso('')
    if (!form.descricao.trim()) { setErro(`Descreva o motivo da ${rotulo} antes de continuar.`); return }
    setEnviando(true)
    try {
      await api.registrarMovimentacao({
        tipo: form.tipo,
        materialId: Number(form.materialId),
        quantidade: Number(form.quantidade),
        dataMovimento: form.dataMovimento || null,
        responsavel: form.responsavel,
        // A API guarda uma única observação: justificativa + observações adicionais.
        observacao: form.observacao.trim() ? `${form.descricao.trim()}\nObservações: ${form.observacao.trim()}` : form.descricao.trim(),
        chaveIdempotencia: chave,
      })
      setSucesso(`Baixa por ${rotulo} registrada: ${form.quantidade} ${material?.unidade || ''} de ${material?.nome || 'material'}.`)
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
          <h1>Baixa por perda ou avaria</h1>
          <div className="topbar-desc">Retira do estoque itens perdidos ou danificados; o registro fica no histórico</div>
        </div>
      </header>
      <AbasSecao abas={ABAS_MOVIMENTACAO} />

      <div className="content">
        <div className="panel" style={{ marginBottom: 24 }}>
          <form onSubmit={registrar}>
            {(erro || erroCarga) && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro || erroCarga}</div>}
            {sucesso && <div style={{ margin: '16px 20px 0' }} className="success-banner">{sucesso}</div>}

            <div style={{ padding: '20px 20px 0' }}>
              <div className="movement-toggle" role="group" aria-label="Tipo de ocorrência">
                <button type="button" className={form.tipo === 'PERDA' ? 'active-perda' : ''} onClick={() => setForm({ ...form, tipo: 'PERDA' })}>Perda</button>
                <button type="button" className={form.tipo === 'AVARIA' ? 'active-avaria' : ''} onClick={() => setForm({ ...form, tipo: 'AVARIA' })}>Avaria</button>
              </div>
            </div>

            <div className="form-grid">
              <SeletorMaterial materiais={materiais} value={form.materialId} onChange={(v) => setForm({ ...form, materialId: v })} exigirSaldo />
              <div className="field">
                <label>Quantidade {material ? `(${material.unidade})` : ''} *</label>
                <input
                  required type="number" min="1" step="1" max={material ? material.quantidadeAtual : undefined}
                  value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })}
                />
                {material && <span className="field-hint">Disponível: {material.quantidadeAtual} {material.unidade}</span>}
              </div>
              <div className="field">
                <label>Data da ocorrência *</label>
                <input required type="date" max={hojeISO()} value={form.dataMovimento} onChange={(e) => setForm({ ...form, dataMovimento: e.target.value })} />
              </div>
              <div className="field">
                <label>Responsável pelo registro *</label>
                <input required maxLength={150} value={form.responsavel} onChange={(e) => setForm({ ...form, responsavel: e.target.value })} />
              </div>
              <div className="field full">
                <label>Descrição / justificativa da {rotulo} *</label>
                <textarea
                  required maxLength={800}
                  placeholder="Ex.: caixa molhada no transporte, item quebrado ao manusear, extraviado na conferência..."
                  value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                />
              </div>
              <div className="field full">
                <label>Observações adicionais</label>
                <textarea maxLength={150} value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => { setForm(vazio()); setErro(''); setSucesso('') }}>Limpar</button>
              <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Registrando...' : `Registrar ${rotulo}`}</button>
            </div>
          </form>
        </div>

        <HistoricoMovimentacoes tipos={['PERDA', 'AVARIA']} recarregar={versao} titulo="Perdas e avarias registradas" />
      </div>
    </>
  )
}
