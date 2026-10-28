import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/api.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatarData, formatarDataHora } from '../utils/formatos.js'

// Termo de Empréstimo: gerado com os dados já registrados no empréstimo.
export default function DocTermo() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const [emp, setEmp] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    api.buscarEmprestimo(id).then(setEmp).catch((e) => setErro(mensagemDeErro(e)))
  }, [id])

  return (
    <>
      <div className="doc-barra">
        <button className="btn btn-primary" onClick={() => window.print()} disabled={!emp}>Imprimir / Salvar em PDF</button>
        <Link className="btn btn-secondary" to="/movimentacao/emprestimos">Voltar</Link>
      </div>
      {erro && <div className="error-banner" style={{ margin: 24 }}>{erro}</div>}
      {emp && (
        <div className="doc-folha">
          <div className="doc-cabecalho">
            <div className="doc-empresa">{usuario?.empresaNome}</div>
            <div>
              <div className="doc-titulo">Termo de Empréstimo</div>
              <div className="doc-numero">Nº {String(emp.id).padStart(6, '0')}</div>
            </div>
          </div>

          <div className="doc-secao">Material emprestado</div>
          <table className="doc-tabela">
            <tbody>
              <tr><th>Material</th><td>{emp.material?.nome}</td></tr>
              <tr><th>Código</th><td>{emp.material?.codigo}</td></tr>
              <tr><th>Marca / modelo</th><td>{[emp.material?.marca, emp.material?.modelo].filter(Boolean).join(' / ') || '—'}</td></tr>
              {emp.material?.ca && <tr><th>C.A.</th><td>{emp.material.ca}</td></tr>}
              <tr><th>Quantidade emprestada</th><td>{emp.quantidade} {emp.material?.unidade}</td></tr>
            </tbody>
          </table>

          <div className="doc-secao">Responsáveis e prazo</div>
          <table className="doc-tabela">
            <tbody>
              <tr><th>Recebido por</th><td>{emp.tomador}</td></tr>
              <tr><th>Setor / departamento</th><td>{emp.setor || '—'}</td></tr>
              <tr><th>Responsável pela entrega</th><td>{emp.responsavelEntrega}</td></tr>
              <tr><th>Data do empréstimo</th><td>{formatarData(emp.dataEmprestimo)}</td></tr>
              <tr><th>Data prevista de devolução</th><td>{formatarData(emp.dataPrevistaDevolucao)}</td></tr>
              {emp.observacao && <tr><th>Observações</th><td>{emp.observacao}</td></tr>}
            </tbody>
          </table>

          <div className="doc-secao">Condições de conservação e devolução</div>
          <p className="doc-texto">
            O recebedor declara ter recebido o material descrito em perfeitas condições de uso e se compromete a
            conservá-lo, utilizá-lo apenas para a finalidade para a qual foi emprestado e devolvê-lo ao almoxarifado
            até a data prevista, nas mesmas condições em que o recebeu.
          </p>
          <p className="doc-texto">
            Em caso de perda, extravio ou dano por mau uso, o recebedor deverá comunicar imediatamente o responsável
            pelo almoxarifado, ficando sujeito às providências de reposição ou ressarcimento previstas pela empresa.
          </p>

          <div className="doc-assinaturas">
            <div className="doc-assinatura"><div className="linha">{emp.tomador}<br />Recebedor</div></div>
            <div className="doc-assinatura"><div className="linha">{emp.responsavelEntrega}<br />Responsável pela entrega</div></div>
          </div>
          <div className="doc-rodape">Documento gerado pelo sistema em {formatarDataHora(new Date().toISOString())}</div>
        </div>
      )}
    </>
  )
}
