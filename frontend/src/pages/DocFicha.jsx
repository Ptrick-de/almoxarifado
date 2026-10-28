import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatarDataHora } from '../utils/formatos.js'

// Ficha de Entrega de Material: gerada a partir da saída para consumo já registrada.
export default function DocFicha() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const [mov, setMov] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    api.buscarMovimentacao(id)
      .then((m) => {
        if (m.tipo !== 'SAIDA') setErro('A Ficha de Entrega de Material só pode ser gerada para saídas de uso e consumo.')
        else setMov(m)
      })
      .catch((e) => setErro(mensagemDeErro(e)))
  }, [id])

  return (
    <>
      <div className="doc-barra">
        <button className="btn btn-primary" onClick={() => window.print()} disabled={!mov}>Imprimir / Salvar em PDF</button>
        <Link className="btn btn-secondary" to="/movimentacao/saida">Voltar</Link>
      </div>
      {erro && <div className="error-banner" style={{ margin: 24 }}>{erro}</div>}
      {mov && (
        <div className="doc-folha">
          <div className="doc-cabecalho">
            <div className="doc-empresa">{usuario?.empresaNome}</div>
            <div>
              <div className="doc-titulo">Ficha de Entrega de Material</div>
              <div className="doc-numero">Nº {String(mov.id).padStart(6, '0')}</div>
            </div>
          </div>

          <table className="doc-tabela">
            <tbody>
              <tr><th>Data da entrega</th><td>{formatarDataHora(mov.dataMovimentacao)}</td></tr>
              <tr><th>Material</th><td>{mov.material?.nome}</td></tr>
              <tr><th>Código</th><td>{mov.material?.codigo}</td></tr>
              <tr><th>Marca / modelo</th><td>{[mov.material?.marca, mov.material?.modelo].filter(Boolean).join(' / ') || '—'}</td></tr>
              {mov.material?.ca && <tr><th>C.A.</th><td>{mov.material.ca}</td></tr>}
              <tr><th>Quantidade entregue</th><td>{mov.quantidade} {mov.material?.unidade}</td></tr>
              <tr><th>Recebido por</th><td>{mov.destinatario}</td></tr>
              <tr><th>Setor / departamento</th><td>{mov.setor || '—'}</td></tr>
              <tr><th>Responsável pela entrega</th><td>{mov.responsavel}</td></tr>
              {mov.observacao && <tr><th>Observações</th><td>{mov.observacao}</td></tr>}
            </tbody>
          </table>

          <p className="doc-texto">Declaro ter recebido o material acima descrito, em perfeitas condições, para uso e consumo.</p>

          <div className="doc-assinaturas">
            <div className="doc-assinatura"><div className="linha">{mov.destinatario}<br />Recebedor</div></div>
            <div className="doc-assinatura"><div className="linha">{mov.responsavel}<br />Responsável pela entrega</div></div>
          </div>
          <div className="doc-rodape">Documento gerado pelo sistema em {formatarDataHora(new Date().toISOString())}</div>
        </div>
      )}
    </>
  )
}
