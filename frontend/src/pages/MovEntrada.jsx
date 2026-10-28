import { useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import AbasSecao, { ABAS_MOVIMENTACAO } from '../components/AbasSecao.jsx'
import HistoricoMovimentacoes from '../components/HistoricoMovimentacoes.jsx'
import SeletorMaterial from '../components/SeletorMaterial.jsx'
import { useMateriais } from '../hooks/useMateriais.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { hojeISO, novaChave, moeda } from '../utils/formatos.js'

export default function MovEntrada() {
  const { usuario } = useAuth()
  const { materiais, fornecedores, recarregar, erroCarga } = useMateriais()
  const vazio = () => ({
    materialId: '', quantidade: '', fornecedorId: '', precoUnitario: '', dataMovimento: hojeISO(),
    responsavel: usuario?.nome || '', observacao: '',
  })
  const [form, setForm] = useState(vazio)
  const [chave, setChave] = useState(novaChave)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [versao, setVersao] = useState(0)

  const material = materiais.find((m) => String(m.id) === String(form.materialId))

  // Ao escolher o fornecedor, sugere o último preço dele para este material (editável).
  function escolherFornecedor(id) {
    const preco = material?.fornecedores.find((f) => String(f.fornecedorId) === String(id))?.precoUnitario
    setForm((f) => ({ ...f, fornecedorId: id, precoUnitario: preco != null ? preco : f.precoUnitario }))
  }

  async function registrar(e) {
    e.preventDefault()
    if (enviando) return
    setErro(''); setSucesso('')
    setEnviando(true)
    try {
      await api.registrarMovimentacao({
        tipo: 'ENTRADA',
        materialId: Number(form.materialId),
        quantidade: Number(form.quantidade),
        fornecedorId: form.fornecedorId ? Number(form.fornecedorId) : null,
        precoUnitario: form.precoUnitario === '' ? null : Number(form.precoUnitario),
        dataMovimento: form.dataMovimento || null,
        responsavel: form.responsavel,
        observacao: form.observacao,
        chaveIdempotencia: chave,
      })
      setSucesso(`Entrada registrada: ${form.quantidade} ${material?.unidade || ''} de ${material?.nome || 'material'}.`)
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
          <h1>Criar entrada</h1>
          <div className="topbar-desc">Registre o recebimento de materiais e atualize o saldo do estoque</div>
        </div>
      </header>
      <AbasSecao abas={ABAS_MOVIMENTACAO} />

      <div className="content">
        <div className="panel" style={{ marginBottom: 24 }}>
          <form onSubmit={registrar}>
            {(erro || erroCarga) && <div style={{ margin: '16px 20px 0' }} className="error-banner">{erro || erroCarga}</div>}
            {sucesso && <div style={{ margin: '16px 20px 0' }} className="success-banner">{sucesso}</div>}

            <div className="form-section">Material recebido</div>
            <div className="form-grid">
              <SeletorMaterial materiais={materiais} value={form.materialId} onChange={(v) => setForm({ ...form, materialId: v, fornecedorId: '', precoUnitario: '' })} />
              <div className="field">
                <label>Quantidade recebida {material ? `(${material.unidade})` : ''} *</label>
                <input required type="number" min="1" step="1" value={form.quantidade} onChange={(e) => setForm({ ...form, quantidade: e.target.value })} />
                {material && <span className="field-hint">Saldo atual: {material.quantidadeAtual} {material.unidade}</span>}
              </div>
              <div className="field">
                <label>Data da entrada *</label>
                <input required type="date" max={hojeISO()} value={form.dataMovimento} onChange={(e) => setForm({ ...form, dataMovimento: e.target.value })} />
              </div>
            </div>

            <div className="form-section">Fornecedor e preço</div>
            <div className="form-grid">
              <div className="field">
                <label>Fornecedor</label>
                <select value={form.fornecedorId} onChange={(e) => escolherFornecedor(e.target.value)}>
                  <option value="">Não informado</option>
                  {fornecedores.map((f) => {
                    const preco = material?.fornecedores.find((x) => x.fornecedorId === f.id)?.precoUnitario
                    return <option key={f.id} value={f.id}>{f.nome}{preco != null ? ` — ${moeda(preco)}` : ''}</option>
                  })}
                </select>
              </div>
              <div className="field">
                <label>Preço unitário (R$)</label>
                <input type="number" min="0" step="0.01" value={form.precoUnitario} onChange={(e) => setForm({ ...form, precoUnitario: e.target.value })} />
                <span className="field-hint">Com fornecedor informado, o preço fica registrado para ele neste material.</span>
              </div>
            </div>

            <div className="form-section">Responsável</div>
            <div className="form-grid">
              <div className="field">
                <label>Responsável pelo registro *</label>
                <input required maxLength={150} value={form.responsavel} onChange={(e) => setForm({ ...form, responsavel: e.target.value })} />
              </div>
              <div className="field full">
                <label>Observações</label>
                <textarea maxLength={1000} placeholder="Ex.: nota fiscal, número do pedido..." value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => { setForm(vazio()); setErro(''); setSucesso('') }}>Limpar</button>
              <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Registrando...' : 'Confirmar entrada'}</button>
            </div>
          </form>
        </div>

        <HistoricoMovimentacoes tipos={['ENTRADA']} recarregar={versao} titulo="Entradas registradas" />
      </div>
    </>
  )
}
