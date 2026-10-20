const BASE_URL = 'http://localhost:8080/api'
const CHAVE_TOKEN = 'almoxarifado_token'

export function getToken() {
  return localStorage.getItem(CHAVE_TOKEN)
}

export function setToken(token) {
  if (token) localStorage.setItem(CHAVE_TOKEN, token)
  else localStorage.removeItem(CHAVE_TOKEN)
}

// Disparado quando a API responde 401 (token ausente/expirado), para que o
// AuthProvider possa encerrar a sessão e redirecionar ao login.
export const SESSAO_EXPIRADA_EVENTO = 'almoxarifado:sessao-expirada'

const MSG_GENERICA =
  'Ocorreu um erro inesperado. Tente novamente e, se o problema persistir, entre em contato com o suporte.'

const MSG_POR_STATUS = {
  400: 'Os dados informados são inválidos. Verifique os campos e tente novamente.',
  403: 'Você não tem permissão para realizar esta ação.',
  404: 'O registro solicitado não foi encontrado. Ele pode ter sido removido; atualize a página.',
  405: 'Esta operação não está disponível.',
  409: 'Não foi possível concluir a operação porque há conflito com outros registros.',
  503: 'O sistema está temporariamente indisponível. Tente novamente em instantes.',
}

// Erro já traduzido para o usuário final: a mensagem pode ser exibida diretamente na tela.
export class ApiError extends Error {
  constructor(mensagem, status) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
  }
}

// Usado pelas telas: só exibe a mensagem se ela veio de um ApiError (segura para o usuário).
// Qualquer outro erro (bug de JavaScript, etc.) vira a mensagem genérica.
export function mensagemDeErro(erro) {
  return erro instanceof ApiError ? erro.message : MSG_GENERICA
}

// Extrai a mensagem do corpo de erro padrão da API: { erro, campos }.
async function lerMensagemDaResposta(resposta) {
  try {
    const corpo = await resposta.json()
    if (typeof corpo?.erro === 'string' && corpo.erro.trim()) return corpo.erro
    if (corpo?.campos && typeof corpo.campos === 'object') {
      const textos = Object.values(corpo.campos).filter((t) => typeof t === 'string')
      if (textos.length) return textos.join(' ')
    }
  } catch (_) { /* corpo vazio ou não-JSON */ }
  return null
}

async function requisitar(caminho, opcoes = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...(opcoes.headers || {}) }
  if (token) headers.Authorization = `Bearer ${token}`

  let resposta
  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, { ...opcoes, headers })
  } catch (_) {
    // Servidor desligado, sem rede, CORS etc.
    throw new ApiError('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.', 0)
  }

  if (resposta.status === 401) {
    setToken(null)
    window.dispatchEvent(new Event(SESSAO_EXPIRADA_EVENTO))
    throw new ApiError('Sessão expirada. Faça login novamente.', 401)
  }

  if (!resposta.ok) {
    const doServidor = await lerMensagemDaResposta(resposta)
    const mensagem = doServidor
      || MSG_POR_STATUS[resposta.status]
      || (resposta.status >= 500 ? MSG_GENERICA : 'Não foi possível concluir a operação. Tente novamente.')
    throw new ApiError(mensagem, resposta.status)
  }

  if (resposta.status === 204) return null
  try {
    return await resposta.json()
  } catch (_) {
    throw new ApiError(MSG_GENERICA, resposta.status)
  }
}

export const api = {
  // Autenticação
  login: (usuario, senha) => requisitar('/auth/login', { method: 'POST', body: JSON.stringify({ usuario, senha }) }),
  buscarUsuarioLogado: () => requisitar('/usuarios/eu'),

  // Usuários (admin)
  listarUsuarios: () => requisitar('/usuarios'),
  criarUsuario: (dados) => requisitar('/usuarios', { method: 'POST', body: JSON.stringify(dados) }),
  alterarStatusUsuario: (id, ativo) => requisitar(`/usuarios/${id}/status`, { method: 'PATCH', body: JSON.stringify({ ativo }) }),
  redefinirSenhaUsuario: (id, novaSenha) => requisitar(`/usuarios/${id}/senha`, { method: 'PATCH', body: JSON.stringify({ novaSenha }) }),
  alterarMinhaSenha: (senhaAtual, novaSenha) => requisitar('/usuarios/eu/senha', { method: 'POST', body: JSON.stringify({ senhaAtual, novaSenha }) }),
  excluirUsuario: (id) => requisitar(`/usuarios/${id}`, { method: 'DELETE' }),

  // Materiais
  listarMateriais: (busca) => requisitar(`/materiais${busca ? `?busca=${encodeURIComponent(busca)}` : ''}`),
  buscarMaterial: (id) => requisitar(`/materiais/${id}`),
  criarMaterial: (dados) => requisitar('/materiais', { method: 'POST', body: JSON.stringify(dados) }),
  atualizarMaterial: (id, dados) => requisitar(`/materiais/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluirMaterial: (id) => requisitar(`/materiais/${id}`, { method: 'DELETE' }),
  listarEstoqueBaixo: () => requisitar('/materiais/estoque-baixo'),

  // Categorias
  listarCategorias: () => requisitar('/categorias'),
  criarCategoria: (dados) => requisitar('/categorias', { method: 'POST', body: JSON.stringify(dados) }),
  atualizarCategoria: (id, dados) => requisitar(`/categorias/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluirCategoria: (id) => requisitar(`/categorias/${id}`, { method: 'DELETE' }),

  // Fornecedores
  listarFornecedores: () => requisitar('/fornecedores'),
  criarFornecedor: (dados) => requisitar('/fornecedores', { method: 'POST', body: JSON.stringify(dados) }),
  atualizarFornecedor: (id, dados) => requisitar(`/fornecedores/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluirFornecedor: (id) => requisitar(`/fornecedores/${id}`, { method: 'DELETE' }),

  // Movimentacoes
  listarMovimentacoes: () => requisitar('/movimentacoes'),
  listarMovimentacoesPorMaterial: (materialId) => requisitar(`/movimentacoes/material/${materialId}`),
  registrarMovimentacao: (dados) => requisitar('/movimentacoes', { method: 'POST', body: JSON.stringify(dados) }),

  // Dashboard
  buscarDashboard: () => requisitar('/dashboard'),
}
