package com.almoxarifado.service;

import com.almoxarifado.dto.DevolucaoRequest;
import com.almoxarifado.dto.EmprestimoRequest;
import com.almoxarifado.dto.MovimentacaoRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.*;
import com.almoxarifado.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Único ponto que altera o saldo do estoque. Cada operação roda em uma transação: se qualquer etapa
 * falhar, nada é gravado. O material é lido com bloqueio de escrita, então duas retiradas simultâneas
 * nunca consomem o mesmo saldo.
 */
@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final MaterialRepository materialRepository;
    private final FornecedorRepository fornecedorRepository;
    private final MaterialFornecedorRepository vinculoRepository;
    private final EmprestimoRepository emprestimoRepository;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository,
                               MaterialRepository materialRepository,
                               FornecedorRepository fornecedorRepository,
                               MaterialFornecedorRepository vinculoRepository,
                               EmprestimoRepository emprestimoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.materialRepository = materialRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.vinculoRepository = vinculoRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    // ------------------------------------------------------------------
    // Entrada, saída para consumo, perda e avaria
    // ------------------------------------------------------------------

    @Transactional
    public Movimentacao registrar(MovimentacaoRequest req, Usuario usuario) {
        TipoMovimentacao tipo = req.getTipo();
        if (tipo == TipoMovimentacao.EMPRESTIMO || tipo == TipoMovimentacao.DEVOLUCAO) {
            throw new RegraDeNegocioException("Empréstimos e devoluções são registrados na tela de Empréstimos.");
        }
        Empresa empresa = usuario.getEmpresa();
        Material material = materialRepository.buscarParaAtualizar(req.getMaterialId(), empresa.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado: " + req.getMaterialId()));

        String chave = limpar(req.getChaveIdempotencia());
        if (chave != null) {
            Optional<Movimentacao> repetida = movimentacaoRepository.findByChaveIdempotenciaAndEmpresaId(chave, empresa.getId());
            if (repetida.isPresent()) return repetida.get();
        }

        exigirAtivo(material);

        String destinatario = limpar(req.getDestinatario());
        String observacao = limpar(req.getObservacao());
        if (tipo == TipoMovimentacao.SAIDA && destinatario == null) {
            throw new RegraDeNegocioException("Informe a pessoa ou o setor que recebeu o material.");
        }
        if ((tipo == TipoMovimentacao.PERDA || tipo == TipoMovimentacao.AVARIA) && observacao == null) {
            throw new RegraDeNegocioException("Descreva o motivo da " +
                    (tipo == TipoMovimentacao.PERDA ? "perda" : "avaria") + " no campo de descrição/justificativa.");
        }

        Fornecedor fornecedor = null;
        if (tipo == TipoMovimentacao.ENTRADA && req.getFornecedorId() != null) {
            fornecedor = fornecedorRepository.findById(req.getFornecedorId())
                    .filter(f -> f.getEmpresa().getId().equals(empresa.getId()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
        }

        Movimentacao mov = new Movimentacao();
        mov.setMaterial(material);
        mov.setTipo(tipo);
        mov.setQuantidade(req.getQuantidade());
        mov.setResponsavel(limpar(req.getResponsavel()) != null ? limpar(req.getResponsavel()) : usuario.getNome());
        mov.setObservacao(observacao);
        mov.setDestinatario(destinatario);
        mov.setSetor(limpar(req.getSetor()));
        mov.setFornecedor(fornecedor);
        mov.setPrecoUnitario(tipo == TipoMovimentacao.ENTRADA ? req.getPrecoUnitario() : null);
        mov.setDataMovimentacao(resolverData(req.getDataMovimento(), "da operação"));
        mov.setChaveIdempotencia(chave);
        mov.setEmpresa(empresa);

        aplicarNoSaldo(material, mov);

        if (tipo == TipoMovimentacao.ENTRADA && req.getPrecoUnitario() != null) {
            material.setPrecoUnitario(req.getPrecoUnitario());
            if (fornecedor != null) registrarPreco(material, fornecedor, req.getPrecoUnitario(), empresa);
        } else if (tipo == TipoMovimentacao.ENTRADA && fornecedor != null) {
            garantirVinculo(material, fornecedor, empresa);
        }
        materialRepository.save(material);
        return movimentacaoRepository.save(mov);
    }

    // ------------------------------------------------------------------
    // Empréstimos e devoluções
    // ------------------------------------------------------------------

    @Transactional
    public Emprestimo emprestar(EmprestimoRequest req, Usuario usuario) {
        Empresa empresa = usuario.getEmpresa();
        Material material = materialRepository.buscarParaAtualizar(req.getMaterialId(), empresa.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado: " + req.getMaterialId()));

        String chave = limpar(req.getChaveIdempotencia());
        if (chave != null) {
            Optional<Emprestimo> repetido = emprestimoRepository.findByChaveIdempotenciaAndEmpresaId(chave, empresa.getId());
            if (repetido.isPresent()) return repetido.get();
        }

        exigirAtivo(material);

        LocalDate hoje = LocalDate.now();
        LocalDate data = req.getDataEmprestimo() != null ? req.getDataEmprestimo() : hoje;
        if (data.isAfter(hoje)) {
            throw new RegraDeNegocioException("A data do empréstimo não pode ser futura.");
        }
        if (req.getDataPrevistaDevolucao().isBefore(data)) {
            throw new RegraDeNegocioException("A data prevista de devolução não pode ser anterior à data do empréstimo.");
        }

        Emprestimo emp = new Emprestimo();
        emp.setMaterial(material);
        emp.setQuantidade(req.getQuantidade());
        emp.setTomador(req.getTomador().trim());
        emp.setSetor(limpar(req.getSetor()));
        emp.setResponsavelEntrega(limpar(req.getResponsavelEntrega()) != null
                ? limpar(req.getResponsavelEntrega()) : usuario.getNome());
        emp.setDataEmprestimo(data);
        emp.setDataPrevistaDevolucao(req.getDataPrevistaDevolucao());
        emp.setObservacao(limpar(req.getObservacao()));
        emp.setStatus(StatusEmprestimo.ATIVO);
        emp.setChaveIdempotencia(chave);
        emp.setEmpresa(empresa);

        Movimentacao mov = new Movimentacao();
        mov.setMaterial(material);
        mov.setTipo(TipoMovimentacao.EMPRESTIMO);
        mov.setQuantidade(req.getQuantidade());
        mov.setResponsavel(emp.getResponsavelEntrega());
        mov.setDestinatario(emp.getTomador());
        mov.setSetor(emp.getSetor());
        mov.setObservacao(emp.getObservacao());
        mov.setDataMovimentacao(resolverData(data, "do empréstimo"));
        mov.setEmpresa(empresa);

        aplicarNoSaldo(material, mov);   // o material sai do saldo disponível UMA única vez
        materialRepository.save(material);
        Emprestimo salvo = emprestimoRepository.save(emp);
        mov.setEmprestimoId(salvo.getId());
        movimentacaoRepository.save(mov);
        return salvo;
    }

    @Transactional
    public Emprestimo devolver(Long emprestimoId, DevolucaoRequest req, Usuario usuario) {
        Empresa empresa = usuario.getEmpresa();
        Emprestimo emp = emprestimoRepository.buscarParaAtualizar(emprestimoId, empresa.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + emprestimoId));
        Material material = materialRepository.buscarParaAtualizar(emp.getMaterial().getId(), empresa.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado"));

        String chave = limpar(req.getChaveIdempotencia());
        if (chave != null && movimentacaoRepository.findByChaveIdempotenciaAndEmpresaId(chave, empresa.getId()).isPresent()) {
            return emp;   // reenvio da mesma devolução: nada é lançado de novo
        }

        if (emp.getStatus() == StatusEmprestimo.DEVOLVIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi totalmente devolvido.");
        }
        int pendente = emp.getQuantidadePendente();
        if (req.getQuantidade() > pendente) {
            throw new RegraDeNegocioException("A quantidade devolvida (" + req.getQuantidade()
                    + ") é maior que a quantidade ainda emprestada (" + pendente + ").");
        }

        LocalDate hoje = LocalDate.now();
        LocalDate data = req.getDataDevolucao() != null ? req.getDataDevolucao() : hoje;
        if (data.isAfter(hoje)) {
            throw new RegraDeNegocioException("A data da devolução não pode ser futura.");
        }
        if (data.isBefore(emp.getDataEmprestimo())) {
            throw new RegraDeNegocioException("A data da devolução não pode ser anterior à data do empréstimo.");
        }

        Movimentacao mov = new Movimentacao();
        mov.setMaterial(material);
        mov.setTipo(TipoMovimentacao.DEVOLUCAO);
        mov.setQuantidade(req.getQuantidade());
        mov.setResponsavel(limpar(req.getResponsavel()) != null ? limpar(req.getResponsavel()) : usuario.getNome());
        mov.setDestinatario(emp.getTomador());
        mov.setSetor(emp.getSetor());
        mov.setObservacao(limpar(req.getObservacao()));
        mov.setEmprestimoId(emp.getId());
        mov.setDataMovimentacao(resolverData(data, "da devolução"));
        mov.setChaveIdempotencia(chave);
        mov.setEmpresa(empresa);

        aplicarNoSaldo(material, mov);
        emp.setQuantidadeDevolvida(emp.getQuantidadeDevolvida() + req.getQuantidade());
        if (emp.getQuantidadePendente() == 0) {
            emp.setStatus(StatusEmprestimo.DEVOLVIDO);
            emp.setDataDevolucaoEfetiva(data);
        }
        materialRepository.save(material);
        movimentacaoRepository.save(mov);
        return emprestimoRepository.save(emp);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Aplica a movimentação ao saldo, bloqueando retiradas acima do disponível. */
    private void aplicarNoSaldo(Material material, Movimentacao mov) {
        int saldo = material.getQuantidadeAtual();
        if (mov.getTipo().aumentaEstoque()) {
            saldo += mov.getQuantidade();
        } else {
            if (saldo < mov.getQuantidade()) {
                long emprestada = emprestimoRepository.somarPendentesDoMaterial(material.getId(), StatusEmprestimo.ATIVO);
                String extra = emprestada > 0 ? " (" + emprestada + " " + material.getUnidade() + " estão emprestados e fora do estoque)" : "";
                throw new RegraDeNegocioException("Estoque insuficiente para " + descricaoCurta(material)
                        + ". Disponível: " + saldo + " " + material.getUnidade() + extra + ".");
            }
            saldo -= mov.getQuantidade();
        }
        material.setQuantidadeAtual(saldo);
        mov.setSaldoApos(saldo);
    }

    private void exigirAtivo(Material material) {
        if (!material.isAtivo()) {
            throw new RegraDeNegocioException("O material " + descricaoCurta(material)
                    + " está inativo e não pode receber movimentações. Reative o cadastro para continuar.");
        }
    }

    private String descricaoCurta(Material m) {
        StringBuilder sb = new StringBuilder(m.getNome());
        if (!m.getMarca().isEmpty()) sb.append(" - ").append(m.getMarca());
        if (!m.getModelo().isEmpty()) sb.append(" ").append(m.getModelo());
        return sb.toString();
    }

    private void registrarPreco(Material material, Fornecedor fornecedor, java.math.BigDecimal preco, Empresa empresa) {
        MaterialFornecedor v = vinculoRepository.findByMaterialIdAndFornecedorId(material.getId(), fornecedor.getId())
                .orElseGet(() -> novoVinculo(material, fornecedor, empresa));
        v.setPrecoUnitario(preco);
        v.setDataAtualizacao(LocalDateTime.now());
        vinculoRepository.save(v);
    }

    private void garantirVinculo(Material material, Fornecedor fornecedor, Empresa empresa) {
        if (vinculoRepository.findByMaterialIdAndFornecedorId(material.getId(), fornecedor.getId()).isEmpty()) {
            MaterialFornecedor v = novoVinculo(material, fornecedor, empresa);
            v.setPrecoUnitario(material.getPrecoUnitario() == null ? java.math.BigDecimal.ZERO : material.getPrecoUnitario());
            vinculoRepository.save(v);
        }
    }

    private MaterialFornecedor novoVinculo(Material material, Fornecedor fornecedor, Empresa empresa) {
        MaterialFornecedor v = new MaterialFornecedor();
        v.setMaterial(material);
        v.setFornecedor(fornecedor);
        v.setEmpresa(empresa);
        return v;
    }

    /** Data informada (hoje por padrão, nunca futura) combinada com a hora atual, para manter a ordem do histórico. */
    private LocalDateTime resolverData(LocalDate data, String descricao) {
        LocalDate hoje = LocalDate.now();
        if (data == null || data.equals(hoje)) return LocalDateTime.now();
        if (data.isAfter(hoje)) {
            throw new RegraDeNegocioException("A data " + descricao + " não pode ser futura.");
        }
        return data.atTime(LocalTime.now());
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
