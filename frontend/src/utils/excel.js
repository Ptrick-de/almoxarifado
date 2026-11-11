// Exportação para Excel (.xlsx) com formatação: título, cabeçalho colorido, larguras,
// datas e números de verdade (dá para filtrar, ordenar e somar), cores por situação e filtro automático.
// A biblioteca (exceljs) só é carregada quando a pessoa clica em exportar.

const COR_CABECALHO = 'FF1F2933'
const COR_FAIXA = 'FFF5F6F8'
const COR_BORDA = 'FFD5D9DF'
const COR_TITULO = 'FF9A4A00'

// "2026-11-10T14:30:00" ou "2026-11-10" -> Date que o Excel mostra exatamente nesse horário
// (o Excel não tem fuso: guardamos os mesmos números da tela dentro de um Date em UTC).
export function paraDataExcel(valor) {
  if (!valor) return null
  const m = String(valor).match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/)
  if (!m) return null
  const [, a, mes, d, h = '0', mi = '0', s = '0'] = m
  return new Date(Date.UTC(+a, +mes - 1, +d, +h, +mi, +s))
}

const FORMATOS = {
  data: 'dd/mm/yyyy',
  datahora: 'dd/mm/yyyy hh:mm',
  moeda: '"R$" #,##0.00',
  inteiro: '0',
  numero: '#,##0.##',
}

function alinhamentoPadrao(c) {
  if (c.alinhar) return c.alinhar
  if (c.formato === 'moeda') return 'right'
  if (['data', 'datahora', 'inteiro', 'numero'].includes(c.formato)) return 'center'
  return 'left'
}

function celulaPreenchida(cor) {
  return { type: 'pattern', pattern: 'solid', fgColor: { argb: cor } }
}

/**
 * @param {object} opcoes
 *  arquivo      nome do arquivo (sem extensão)
 *  aba          nome da planilha
 *  titulo       título grande no topo
 *  subtitulo    linhas de texto abaixo do título (ex.: filtros aplicados)
 *  colunas      [{ cabecalho, chave, largura, formato: 'texto'|'data'|'datahora'|'moeda'|'inteiro'|'numero',
 *                  alinhar: 'left'|'center'|'right', quebrar: boolean, cor: (valor, linha) => [fundoARGB, textoARGB] | null }]
 *  linhas       array de objetos; datas devem vir como Date (use paraDataExcel)
 *  totais       { chave: 'soma' } opcional: linha de total no fim
 */
export async function baixarExcel({ arquivo, aba, titulo, subtitulo = [], colunas, linhas, totais = null }) {
  const ExcelJS = (await import('exceljs')).default
  const wb = new ExcelJS.Workbook()
  wb.creator = 'Almoxarifado'
  wb.created = new Date()
  const ws = wb.addWorksheet(aba || 'Relatório', {
    views: [{ showGridLines: false }],
    pageSetup: { orientation: 'landscape', fitToPage: true, fitToWidth: 1, fitToHeight: 0, paperSize: 9, margins: { left: 0.4, right: 0.4, top: 0.6, bottom: 0.6, header: 0.3, footer: 0.3 } },
  })
  const n = colunas.length
  ws.columns = colunas.map((c) => ({ key: c.chave, width: c.largura || 16 }))

  // ---- Título e subtítulos
  ws.mergeCells(1, 1, 1, n)
  const t = ws.getCell(1, 1)
  t.value = titulo
  t.font = { name: 'Calibri', size: 18, bold: true, color: { argb: COR_TITULO } }
  t.alignment = { vertical: 'middle' }
  ws.getRow(1).height = 30
  let linha = 2
  for (const texto of subtitulo) {
    ws.mergeCells(linha, 1, linha, n)
    const c = ws.getCell(linha, 1)
    c.value = texto
    c.font = { name: 'Calibri', size: 10, italic: true, color: { argb: 'FF5B6470' } }
    c.alignment = { vertical: 'middle', wrapText: true }
    linha += 1
  }
  linha += 1 // linha em branco

  // ---- Cabeçalho
  const linhaCab = linha
  const cab = ws.getRow(linhaCab)
  colunas.forEach((c, i) => {
    const cel = cab.getCell(i + 1)
    cel.value = c.cabecalho
    cel.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } }
    cel.fill = celulaPreenchida(COR_CABECALHO)
    cel.alignment = { vertical: 'middle', horizontal: alinhamentoPadrao(c), indent: alinhamentoPadrao(c) === 'center' ? 0 : 1, wrapText: true }
    cel.border = { bottom: { style: 'medium', color: { argb: 'FFE08A2E' } } }
  })
  cab.height = 26

  // ---- Linhas
  linhas.forEach((item, idx) => {
    const r = ws.getRow(linhaCab + 1 + idx)
    colunas.forEach((c, i) => {
      const cel = r.getCell(i + 1)
      const v = item[c.chave]
      cel.value = v === undefined || v === '' ? null : v
      if (FORMATOS[c.formato]) cel.numFmt = FORMATOS[c.formato]
      cel.font = { name: 'Calibri', size: 11, color: { argb: 'FF1F2933' } }
      cel.alignment = {
        vertical: 'middle',
        wrapText: !!c.quebrar,
        horizontal: alinhamentoPadrao(c),
        indent: alinhamentoPadrao(c) === 'center' ? 0 : 1,
      }
      if (idx % 2 === 1) cel.fill = celulaPreenchida(COR_FAIXA)
      cel.border = { bottom: { style: 'thin', color: { argb: COR_BORDA } } }
      const cor = c.cor ? c.cor(v, item) : null
      if (cor) {
        cel.fill = celulaPreenchida(cor[0])
        cel.font = { name: 'Calibri', size: 11, bold: true, color: { argb: cor[1] } }
        cel.alignment = { vertical: 'middle', horizontal: 'center' }
      }
    })
    if (!colunas.some((c) => c.quebrar)) r.height = 20
  })

  // ---- Linha de totais
  const ultima = linhaCab + linhas.length
  if (totais && linhas.length) {
    const r = ws.getRow(ultima + 1)
    colunas.forEach((c, i) => {
      const cel = r.getCell(i + 1)
      if (i === 0) cel.value = 'Total'
      if (totais[c.chave] === 'soma') {
        const letra = ws.getColumn(i + 1).letter
        cel.value = { formula: `SUBTOTAL(109,${letra}${linhaCab + 1}:${letra}${ultima})` }
        if (FORMATOS[c.formato]) cel.numFmt = FORMATOS[c.formato]
        cel.alignment = { horizontal: alinhamentoPadrao(c), vertical: 'middle' }
      }
      cel.font = { name: 'Calibri', size: 11, bold: true }
      cel.border = { top: { style: 'medium', color: { argb: COR_CABECALHO } } }
    })
    r.height = 22
  }

  // ---- Filtro automático, cabeçalho fixo e repetição na impressão
  ws.autoFilter = { from: { row: linhaCab, column: 1 }, to: { row: ultima, column: n } }
  ws.views = [{ state: 'frozen', ySplit: linhaCab, showGridLines: false }]
  ws.pageSetup.printTitlesRow = `${linhaCab}:${linhaCab}`
  ws.headerFooter.oddFooter = '&L&8Almoxarifado&R&8Página &P de &N'

  const buffer = await wb.xlsx.writeBuffer()
  const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${arquivo}.xlsx`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

// Cores (fundo, texto) por tipo e situação, no mesmo espírito dos selos da tela.
export const COR_TIPO = {
  ENTRADA: ['FFDDF3E4', 'FF1E6B3A'],
  SAIDA: ['FFFDE8CF', 'FF9A4A00'],
  PERDA: ['FFFBDADA', 'FF9B1C1C'],
  AVARIA: ['FFF7D9E7', 'FF8A1C55'],
  EMPRESTIMO: ['FFFFF1BF', 'FF7A5A00'],
  DEVOLUCAO: ['FFD9EEF2', 'FF0F5F6B'],
}
export const COR_SITUACAO_MATERIAL = {
  DISPONIVEL: ['FFDDF3E4', 'FF1E6B3A'],
  ESTOQUE_BAIXO: ['FFFFF1BF', 'FF7A5A00'],
  SEM_ESTOQUE: ['FFFBDADA', 'FF9B1C1C'],
  EMPRESTADO: ['FFD9EEF2', 'FF0F5F6B'],
  INDISPONIVEL: ['FFE6E8EB', 'FF4B5563'],
}
export const COR_SITUACAO_EMPRESTIMO = {
  ATIVO: ['FFD9EEF2', 'FF0F5F6B'],
  ATRASADO: ['FFFBDADA', 'FF9B1C1C'],
  DEVOLVIDO: ['FFDDF3E4', 'FF1E6B3A'],
}
