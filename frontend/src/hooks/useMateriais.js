import { useCallback, useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../api/api.js'

// Carrega materiais e fornecedores para os formulários de movimentação e permite recarregar após cada operação
// (o saldo disponível exibido no seletor sempre vem do servidor).
export function useMateriais() {
  const [materiais, setMateriais] = useState([])
  const [fornecedores, setFornecedores] = useState([])
  const [erroCarga, setErroCarga] = useState('')

  const recarregar = useCallback(() => {
    return Promise.all([api.listarMateriais(), api.listarFornecedores()])
      .then(([m, f]) => { setMateriais(m); setFornecedores(f); setErroCarga('') })
      .catch((e) => setErroCarga(mensagemDeErro(e)))
  }, [])

  useEffect(() => { recarregar() }, [recarregar])

  return { materiais, fornecedores, recarregar, erroCarga }
}
