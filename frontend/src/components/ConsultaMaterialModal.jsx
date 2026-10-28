import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'
import Modal from './Modal.jsx'
import Filtros from './Filtros.jsx'
import { moeda, SITUACOES_MATERIAL } from '../utils/formatos.js'

const CAMPOS = (fornecedores) => [
  { nome: 'busca', rotulo: 'Pesquisar', tipo: 'texto', placeholder: 'Código, nome, marca, modelo, C.A. ou fornecedor' },
  { nome: 'marca', rotulo: 'Marca', tipo: 'texto' },
  { nome: 'modelo', rotulo: 'Modelo', tipo: 'texto' },
  { nome: 'ca', rotulo: 'C.A.', tipo: 'texto' },
  { nome: 'fornecedorId', rotulo: 'Fornecedor', tipo: 'select', opcoes: fornecedores.map((f) => ({ valor: f.id, rotulo: f.nome })) },
  {
    nome: 'situacao', rotulo: 'Situação', tipo: 'select',
    opcoes: Object.entries(SITUACOES_MATERIAL).map(([valor, s]) => ({ valor, rotulo: s.label })),
  },
]

const FILTROS_VAZIOS = { busca: '', marca: '', modelo: '', ca: '', fornecedorId: '', situacao: '' }

// "Consultar Material": pesquisa e mostra o que existe, o que está emprestado e o que está de fato disponível.
export default function ConsultaMaterialModal({ onClose, materialInicial = null }) {
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS)
  const [fornecedores, setFornecedores] = useState([])
  const [resultados, setResultados] = useState([])
  const [selecionado, setSelecionado] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    api.listarFornecedores().then(setFornecedores).catch(() => {})
  }, [])

  useEffect(() => {
    const t = setTimeout(() => {
      api.listarMateriais(filtros)
        .then((lista) => {
          setResultados(lista)
          setErro('')
          if (materialInicial && !selecionado) {
            setSelecionado(lista.find((m) => m.id === materialInicial.id) || null)
          }
        })
        .catch((e) => setErro(mensagemDeErro(e)))
    }, 250)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtros])

  function alterar(nome, valor) {
    setFiltros((f) => ({ ...f, [nome]: valor }))
  }

  // Mantém os detalhes sincronizados com a lista recarregada.
  const detalhe = selecionado ? resultados.find((m) => m.id === selecionado.id) || selecionado : null

  return (
    <Modal title="Consultar material" onClose={onClose} larg>
      <div style={{ padding: '16px 20px 0' }}>
        <Filtros
          campos={CAMPOS(fornecedores)}
          valores={filtros}
          onChange={alterar}
          onLimpar={() => setFiltros(FILTROS_VAZIOS)}
          total={resultados.length}
        />
        {erro && <div className="error-banner">{erro}</div>}
      </div>

      <div style={{ maxHeight: '38vh', overflow: 'auto' }}>
        {resultados.length === 0 ? (
          <div className="empty-state">
            <div className="empty-title">Nenhum material encontrado</div>
            Ajuste ou limpe os filtros para ver mais resultados.
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Código</th><th>Material</th><th>Marca / modelo</th><th>C.A.</th>
                <th>Total</th><th>Emprestada</th><th>Disponível</th><th>Situação</th>
              </tr>
            </thead>
            <tbody>
              {resultados.map((m) => {
                const s = SITUACOES_MATERIAL[m.situacao]
                return (
                  <tr key={m.id} onClick={() => setSelecionado(m)} style={{ cursor: 'pointer', background: detalhe?.id === m.id ? '#FAF6EA' : undefined }}>
                    <td className="mono">{m.codigo}</td>
                    <td>{m.nome}</td>
                    <td>{[m.marca, m.modelo].filter(Boolean).join(' / ') || '—'}</td>
                    <td>{m.ca || '—'}</td>
                    <td>{m.quantidadeTotal} {m.unidade}</td>
                    <td>{m.quantidadeEmprestada}</td>
                    <td><strong>{m.quantidadeDisponivel}</strong> {m.unidade}</td>
                    <td><span className={`badge ${s.classe}`}>{s.label}</span></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {detalhe && <DetalheMaterial material={detalhe} />}

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onClose}>Fechar</button>
      </div>
    </Modal>
  )
}

function DetalheMaterial({ material: m }) {
  const s = SITUACOES_MATERIAL[m.situacao]
  const itens = [
    ['Código', m.codigo], ['Material', m.nome], ['Marca', m.marca || '—'], ['Modelo', m.modelo || '—'],
    ['C.A.', m.ca || '—'], ['Categoria', m.categoria?.nome || '—'], ['Localização', m.localizacao || '—'],
    ['Estoque mínimo', `${m.quantidadeMinima} ${m.unidade}`],
  ]
  return (
    <div style={{ borderTop: '1px solid var(--line)', marginTop: 8 }}>
      <div className="detail-grid">
        {itens.map(([rotulo, valor]) => (
          <div className="detail-item" key={rotulo}>
            <div className="rotulo">{rotulo}</div>
            <div className="valor">{valor}</div>
          </div>
        ))}
        <div className="detail-item"><div className="rotulo">Quantidade total</div><div className="valor grande">{m.quantidadeTotal}</div></div>
        <div className="detail-item"><div className="rotulo">Emprestada</div><div className="valor grande">{m.quantidadeEmprestada}</div></div>
        <div className="detail-item"><div className="rotulo">Reservada</div><div className="valor grande">{m.quantidadeReservada}</div><div className="texto-pequeno">O sistema não usa reservas</div></div>
        <div className="detail-item"><div className="rotulo">Disponível agora</div><div className="valor grande">{m.quantidadeDisponivel}</div></div>
        <div className="detail-item"><div className="rotulo">Situação</div><div className="valor"><span className={`badge ${s.classe}`}>{s.label}</span></div></div>
      </div>
      <div style={{ padding: '0 20px 16px' }}>
        <div className="rotulo texto-pequeno" style={{ fontWeight: 600, marginBottom: 6 }}>FORNECEDORES E PREÇOS</div>
        {m.fornecedores.length === 0 ? (
          <div className="texto-pequeno">Nenhum fornecedor associado. Preço de referência: {moeda(m.precoUnitario)}</div>
        ) : (
          <table>
            <thead><tr><th>Fornecedor</th><th>Preço unitário</th></tr></thead>
            <tbody>
              {m.fornecedores.map((f) => (
                <tr key={f.fornecedorId}><td>{f.fornecedorNome}</td><td>{moeda(f.precoUnitario)}</td></tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
