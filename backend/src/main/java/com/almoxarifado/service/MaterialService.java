package com.almoxarifado.service;

import com.almoxarifado.dto.MaterialDTO;
import com.almoxarifado.dto.MaterialRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.*;
import com.almoxarifado.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final CategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final MaterialFornecedorRepository vinculoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final EmpresaRepository empresaRepository;

    public MaterialService(MaterialRepository materialRepository,
                           CategoriaRepository categoriaRepository,
                           FornecedorRepository fornecedorRepository,
                           MaterialFornecedorRepository vinculoRepository,
                           MovimentacaoRepository movimentacaoRepository,
                           EmprestimoRepository emprestimoRepository,
                           EmpresaRepository empresaRepository) {
        this.materialRepository = materialRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.vinculoRepository = vinculoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.empresaRepository = empresaRepository;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    public static class Filtro {
        public String busca;       // procura em código, nome, descrição, marca, modelo, C.A. e fornecedor
        public String codigo;
        public String nome;        // nome ou descrição
        public String marca;
        public String modelo;
        public String ca;
        public String fornecedor;  // nome do fornecedor
        public Long fornecedorId;
        public Long categoriaId;
        public String situacao;    // DISPONIVEL, ESTOQUE_BAIXO, SEM_ESTOQUE, EMPRESTADO, INDISPONIVEL
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO> listar(Long empresaId, Filtro filtro) {
        List<Material> materiais = materialRepository.findAllByEmpresaIdOrderByNome(empresaId);
        Map<Long, List<MaterialFornecedor>> vinculos = vinculoRepository.findAllByEmpresaId(empresaId).stream()
                .collect(Collectors.groupingBy(v -> v.getMaterial().getId()));
        Map<Long, Integer> emprestadas = emprestadasPorMaterial(empresaId);

        List<MaterialDTO> resultado = new ArrayList<>();
        for (Material m : materiais) {
            MaterialDTO dto = montar(m, vinculos.getOrDefault(m.getId(), List.of()), emprestadas.getOrDefault(m.getId(), 0));
            if (atende(dto, filtro)) resultado.add(dto);
        }
        resultado.sort(Comparator.comparing((MaterialDTO d) -> d.getNome().toLowerCase(Locale.ROOT))
                .thenComparing(MaterialDTO::getMarca, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    @Transactional(readOnly = true)
    public MaterialDTO obter(Long id, Long empresaId) {
        Material m = buscar(id, empresaId);
        int emprestada = emprestimoRepository.somarPendentesDoMaterial(m.getId(), StatusEmprestimo.ATIVO).intValue();
        return montar(m, vinculoRepository.findAllByMaterialId(m.getId()), emprestada);
    }

    private Map<Long, Integer> emprestadasPorMaterial(Long empresaId) {
        Map<Long, Integer> mapa = new HashMap<>();
        for (Object[] linha : emprestimoRepository.somarPendentesPorMaterial(empresaId, StatusEmprestimo.ATIVO)) {
            mapa.put((Long) linha[0], ((Number) linha[1]).intValue());
        }
        return mapa;
    }

    private MaterialDTO montar(Material m, List<MaterialFornecedor> vinculos, int emprestada) {
        List<MaterialDTO.FornecedorPrecoDTO> fornecedores = vinculos.stream()
                .sorted(Comparator.comparing(v -> v.getFornecedor().getNome().toLowerCase(Locale.ROOT)))
                .map(MaterialDTO.FornecedorPrecoDTO::new)
                .collect(Collectors.toList());
        return new MaterialDTO(m, fornecedores, emprestada);
    }

    private boolean atende(MaterialDTO d, Filtro f) {
        if (f == null) return true;
        if (!vazio(f.busca)) {
            String termo = normalizar(f.busca);
            String alvo = normalizar(String.join(" ", d.getCodigo(), d.getNome(), nz(d.getDescricao()), d.getMarca(),
                    d.getModelo(), d.getCa(),
                    d.getFornecedores().stream().map(MaterialDTO.FornecedorPrecoDTO::getFornecedorNome)
                            .collect(Collectors.joining(" "))));
            if (!alvo.contains(termo)) return false;
        }
        if (!contem(d.getCodigo(), f.codigo)) return false;
        if (!vazio(f.nome) && !(contem(d.getNome(), f.nome) || contem(d.getDescricao(), f.nome))) return false;
        if (!contem(d.getMarca(), f.marca)) return false;
        if (!contem(d.getModelo(), f.modelo)) return false;
        if (!contem(d.getCa(), f.ca)) return false;
        if (!vazio(f.fornecedor) && d.getFornecedores().stream().noneMatch(x -> contem(x.getFornecedorNome(), f.fornecedor))) return false;
        if (f.fornecedorId != null && d.getFornecedores().stream().noneMatch(x -> f.fornecedorId.equals(x.getFornecedorId()))) return false;
        if (f.categoriaId != null && (d.getCategoria() == null || !f.categoriaId.equals(d.getCategoria().getId()))) return false;
        if (!vazio(f.situacao) && !f.situacao.equalsIgnoreCase(d.getSituacao())) return false;
        return true;
    }

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Transactional
    public MaterialDTO criar(MaterialRequest req, Usuario usuario) {
        Empresa empresa = empresaRepository.buscarParaAtualizar(usuario.getEmpresa().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada"));
        Long empresaId = empresa.getId();

        Categoria categoria = buscarCategoria(req.getCategoriaId(), empresaId);
        List<MaterialFornecedor> novosVinculos = prepararVinculos(req, empresa);

        String nome = req.getNome().trim();
        String codigo = definirCodigo(req.getCodigoBase(), nome, empresaId);

        Material material = new Material();
        material.setEmpresa(empresa);
        material.setCodigo(codigo);
        validarVariacaoUnica(codigo, req.getMarca(), req.getModelo(), req.getCa(), null, empresaId);
        aplicarDados(material, req, categoria, nome);

        int saldoInicial = req.getQuantidadeAtual() == null ? 0 : req.getQuantidadeAtual();
        material.setQuantidadeAtual(saldoInicial);
        material.setPrecoUnitario(precoReferencia(req, novosVinculos));
        Material salvo = materialRepository.save(material);

        for (MaterialFornecedor v : novosVinculos) {
            v.setMaterial(salvo);
            vinculoRepository.save(v);
        }

        if (saldoInicial > 0) {
            Movimentacao mov = new Movimentacao();
            mov.setMaterial(salvo);
            mov.setTipo(TipoMovimentacao.ENTRADA);
            mov.setQuantidade(saldoInicial);
            mov.setResponsavel(usuario.getNome());
            mov.setObservacao("Saldo inicial informado no cadastro do material");
            mov.setPrecoUnitario(salvo.getPrecoUnitario());
            mov.setSaldoApos(saldoInicial);
            mov.setEmpresa(empresa);
            movimentacaoRepository.save(mov);
        }
        return obter(salvo.getId(), empresaId);
    }

    @Transactional
    public MaterialDTO atualizar(Long id, MaterialRequest req, Long empresaId) {
        Material material = buscar(id, empresaId);
        Empresa empresa = material.getEmpresa();

        Categoria categoria = buscarCategoria(req.getCategoriaId(), empresaId);
        List<MaterialFornecedor> desejados = prepararVinculos(req, empresa);

        // O código identifica o produto e nunca muda; o saldo só muda por movimentações.
        validarVariacaoUnica(material.getCodigo(), req.getMarca(), req.getModelo(), req.getCa(), material.getId(), empresaId);
        aplicarDados(material, req, categoria, req.getNome().trim());
        if (req.getAtivo() != null) material.setAtivo(req.getAtivo());
        if (req.getPrecoUnitario() != null) material.setPrecoUnitario(req.getPrecoUnitario());
        materialRepository.save(material);

        sincronizarVinculos(material, desejados);
        return obter(material.getId(), empresaId);
    }

    @Transactional
    public void excluir(Long id, Long empresaId) {
        Material material = buscar(id, empresaId);
        if (movimentacaoRepository.existsByMaterialId(material.getId())
                || emprestimoRepository.existsByMaterialId(material.getId())) {
            throw new RegraDeNegocioException(
                    "Este material possui histórico de movimentações e não pode ser excluído. "
                    + "Para tirá-lo de uso, edite o cadastro e marque como inativo — o histórico é preservado.");
        }
        vinculoRepository.deleteAllByMaterialId(material.getId());
        materialRepository.delete(material);
    }

    // ------------------------------------------------------------------
    // Regras de código e variações
    // ------------------------------------------------------------------

    /**
     * Regra de código (documentada no README):
     * 1) se foi pedida uma nova variação de um produto existente (codigoBase), reutiliza o código dele;
     * 2) senão, se já existe produto com o mesmo nome, reutiliza o código desse produto;
     * 3) caso contrário gera o próximo código sequencial (MAT-001, MAT-002...).
     */
    private String definirCodigo(String codigoBase, String nome, Long empresaId) {
        if (!vazio(codigoBase)) {
            String base = codigoBase.trim();
            List<Material> existentes = materialRepository.findAllByCodigoAndEmpresaId(base, empresaId);
            if (existentes.isEmpty()) {
                throw new RegraDeNegocioException("O produto de referência (código " + base + ") não foi encontrado.");
            }
            return existentes.get(0).getCodigo();
        }
        List<Material> mesmoNome = materialRepository.findPorNomeExato(empresaId, nome);
        if (!mesmoNome.isEmpty()) {
            return mesmoNome.get(0).getCodigo();
        }
        int maior = 0;
        for (String c : materialRepository.findCodigosGerados(empresaId)) {
            String sufixo = c.substring(4);
            if (sufixo.matches("\\d{1,9}")) maior = Math.max(maior, Integer.parseInt(sufixo));
        }
        return String.format("MAT-%03d", maior + 1);
    }

    /** Deve ser chamado ANTES de alterar a entidade gerenciada, para não disparar flush com dados duplicados. */
    private void validarVariacaoUnica(String codigo, String marca, String modelo, String ca, Long idAtual, Long empresaId) {
        String m1 = marca == null ? "" : marca.trim();
        String m2 = modelo == null ? "" : modelo.trim();
        String m3 = ca == null ? "" : ca.trim();
        for (Material outro : materialRepository.findAllByCodigoAndEmpresaId(codigo, empresaId)) {
            if (idAtual != null && outro.getId().equals(idAtual)) continue;
            if (outro.getMarca().equalsIgnoreCase(m1)
                    && outro.getModelo().equalsIgnoreCase(m2)
                    && outro.getCa().equalsIgnoreCase(m3)) {
                throw new RegraDeNegocioException(
                        "Já existe este material (código " + codigo + ") com a mesma marca, modelo e C.A. "
                        + "Para cadastrar uma nova variação, altere a marca, o modelo ou o C.A.; "
                        + "para incluir outro fornecedor, edite o cadastro existente.");
            }
        }
    }

    private void aplicarDados(Material m, MaterialRequest req, Categoria categoria, String nome) {
        m.setNome(nome);
        m.setDescricao(limpar(req.getDescricao()));
        m.setMarca(req.getMarca() == null ? "" : req.getMarca().trim());
        m.setModelo(req.getModelo() == null ? "" : req.getModelo().trim());
        m.setCa(req.getCa() == null ? "" : req.getCa().trim());
        m.setUnidade(req.getUnidade().trim());
        m.setQuantidadeMinima(req.getQuantidadeMinima() == null ? 0 : req.getQuantidadeMinima());
        m.setLocalizacao(limpar(req.getLocalizacao()));
        m.setCategoria(categoria);
    }

    private BigDecimal precoReferencia(MaterialRequest req, List<MaterialFornecedor> vinculos) {
        if (req.getPrecoUnitario() != null) return req.getPrecoUnitario();
        return vinculos.stream().map(MaterialFornecedor::getPrecoUnitario).min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------
    // Fornecedores x preços
    // ------------------------------------------------------------------

    private List<MaterialFornecedor> prepararVinculos(MaterialRequest req, Empresa empresa) {
        List<MaterialFornecedor> lista = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        for (MaterialRequest.FornecedorPreco item : req.getFornecedores()) {
            if (!vistos.add(item.getFornecedorId())) {
                throw new RegraDeNegocioException("O mesmo fornecedor foi informado mais de uma vez para este material.");
            }
            Fornecedor fornecedor = fornecedorRepository.findById(item.getFornecedorId())
                    .filter(f -> f.getEmpresa().getId().equals(empresa.getId()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
            MaterialFornecedor v = new MaterialFornecedor();
            v.setFornecedor(fornecedor);
            v.setPrecoUnitario(item.getPrecoUnitario());
            v.setEmpresa(empresa);
            lista.add(v);
        }
        return lista;
    }

    private void sincronizarVinculos(Material material, List<MaterialFornecedor> desejados) {
        Map<Long, MaterialFornecedor> atuais = vinculoRepository.findAllByMaterialId(material.getId()).stream()
                .collect(Collectors.toMap(v -> v.getFornecedor().getId(), v -> v));
        Set<Long> manter = new HashSet<>();
        for (MaterialFornecedor novo : desejados) {
            Long fid = novo.getFornecedor().getId();
            manter.add(fid);
            MaterialFornecedor existente = atuais.get(fid);
            if (existente == null) {
                novo.setMaterial(material);
                vinculoRepository.save(novo);
            } else if (existente.getPrecoUnitario().compareTo(novo.getPrecoUnitario()) != 0) {
                existente.setPrecoUnitario(novo.getPrecoUnitario());
                existente.setDataAtualizacao(LocalDateTime.now());
                vinculoRepository.save(existente);
            }
        }
        for (Map.Entry<Long, MaterialFornecedor> e : atuais.entrySet()) {
            if (!manter.contains(e.getKey())) vinculoRepository.delete(e.getValue());
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Material buscar(Long id, Long empresaId) {
        return materialRepository.findById(id)
                .filter(m -> m.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado: " + id));
    }

    private Categoria buscarCategoria(Long id, Long empresaId) {
        if (id == null) return null;
        return categoriaRepository.findById(id)
                .filter(c -> c.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean contem(String texto, String trecho) {
        if (vazio(trecho)) return true;
        return normalizar(nz(texto)).contains(normalizar(trecho));
    }

    /** Minúsculas e sem acentos, para a pesquisa ignorar maiúsculas e acentuação. */
    static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
