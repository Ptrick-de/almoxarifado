// Funções de formatação e constantes compartilhadas entre as telas.

export function moeda(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function formatarDataHora(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// Aceita 'AAAA-MM-DD' (LocalDate da API) ou data-hora ISO.
export function formatarData(iso) {
  if (!iso) return '—'
  const [ano, mes, dia] = String(iso).slice(0, 10).split('-')
  return `${dia}/${mes}/${ano}`
}

// Data de hoje no fuso local, no formato aceito por <input type="date">.
export function hojeISO() {
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

// Chave única por tentativa de operação: evita lançamentos duplicados se o envio for repetido.
export function novaChave() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(16).slice(2)}-${Math.random().toString(16).slice(2)}`
}

export function nomeCompletoMaterial(m) {
  if (!m) return ''
  const extras = [m.marca, m.modelo].filter(Boolean).join(' ')
  return extras ? `${m.nome} — ${extras}` : m.nome
}

export const SITUACOES_MATERIAL = {
  DISPONIVEL: { label: 'Disponível', classe: 'badge-ok' },
  ESTOQUE_BAIXO: { label: 'Estoque baixo', classe: 'badge-low' },
  SEM_ESTOQUE: { label: 'Sem estoque', classe: 'badge-out' },
  EMPRESTADO: { label: 'Emprestado', classe: 'badge-emprestimo' },
  INDISPONIVEL: { label: 'Indisponível', classe: 'badge-muted' },
}

export const TIPOS_MOVIMENTACAO = {
  ENTRADA: { label: 'Entrada', classe: 'badge-entrada' },
  SAIDA: { label: 'Saída (consumo)', classe: 'badge-saida' },
  PERDA: { label: 'Perda', classe: 'badge-perda' },
  AVARIA: { label: 'Avaria', classe: 'badge-avaria' },
  EMPRESTIMO: { label: 'Empréstimo', classe: 'badge-emprestimo' },
  DEVOLUCAO: { label: 'Devolução', classe: 'badge-devolucao' },
}

export const SITUACOES_EMPRESTIMO = {
  ATIVO: { label: 'Ativo', classe: 'badge-emprestimo' },
  ATRASADO: { label: 'Atrasado', classe: 'badge-out' },
  DEVOLVIDO: { label: 'Devolvido', classe: 'badge-ok' },
}

// Monta a query string ignorando filtros vazios.
export function montarQuery(filtros) {
  const p = new URLSearchParams()
  Object.entries(filtros || {}).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) p.append(k, v)
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}

// Exporta linhas (array de arrays) como CSV compatível com o Excel brasileiro.
export function baixarCsv(nomeArquivo, cabecalho, linhas) {
  const escapar = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const texto = [cabecalho, ...linhas].map((l) => l.map(escapar).join(';')).join('\r\n')
  const blob = new Blob(['﻿' + texto], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nomeArquivo
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
