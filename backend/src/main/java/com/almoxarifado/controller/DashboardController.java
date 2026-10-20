package com.almoxarifado.controller;

import com.almoxarifado.dto.DashboardDTO;
import com.almoxarifado.model.Material;
import com.almoxarifado.model.Movimentacao;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.CategoriaRepository;
import com.almoxarifado.repository.FornecedorRepository;
import com.almoxarifado.repository.MaterialRepository;
import com.almoxarifado.repository.MovimentacaoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MaterialRepository materialRepository;
    private final CategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public DashboardController(MaterialRepository materialRepository,
                                CategoriaRepository categoriaRepository,
                                FornecedorRepository fornecedorRepository,
                                MovimentacaoRepository movimentacaoRepository) {
        this.materialRepository = materialRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @GetMapping
    public DashboardDTO resumo(Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        Long empresaId = logado.getEmpresa().getId();

        List<Material> materiais = materialRepository.findAllByEmpresaIdOrderByNome(empresaId);
        List<Material> estoqueBaixo = materialRepository.findEstoqueBaixoByEmpresa(empresaId);

        BigDecimal valorTotal = materiais.stream()
                .map(m -> m.getPrecoUnitario().multiply(BigDecimal.valueOf(m.getQuantidadeAtual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Movimentacao> ultimas = movimentacaoRepository.findAllByEmpresaIdOrderByDataMovimentacaoDesc(empresaId);
        List<Movimentacao> ultimasLimitadas = ultimas.size() > 8 ? ultimas.subList(0, 8) : ultimas;

        DashboardDTO dto = new DashboardDTO();
        dto.setTotalMateriais(materiais.size());
        dto.setTotalCategorias(categoriaRepository.findAllByEmpresaIdOrderByNome(empresaId).size());
        dto.setTotalFornecedores(fornecedorRepository.findAllByEmpresaIdOrderByNome(empresaId).size());
        dto.setItensEstoqueBaixo(estoqueBaixo.size());
        dto.setValorTotalEstoque(valorTotal);
        dto.setMateriaisEstoqueBaixo(estoqueBaixo);
        dto.setUltimasMovimentacoes(ultimasLimitadas);
        return dto;
    }
}
