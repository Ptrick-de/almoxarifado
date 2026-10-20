package com.almoxarifado.controller;

import com.almoxarifado.dto.MovimentacaoRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.Material;
import com.almoxarifado.model.Movimentacao;
import com.almoxarifado.model.TipoMovimentacao;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.MaterialRepository;
import com.almoxarifado.repository.MovimentacaoRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoRepository movimentacaoRepository;
    private final MaterialRepository materialRepository;

    public MovimentacaoController(MovimentacaoRepository movimentacaoRepository,
                                   MaterialRepository materialRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.materialRepository = materialRepository;
    }

    @GetMapping
    public List<Movimentacao> listar(Authentication authentication) {
        return movimentacaoRepository.findAllByEmpresaIdOrderByDataMovimentacaoDesc(empresaId(authentication));
    }

    @GetMapping("/material/{materialId}")
    public List<Movimentacao> listarPorMaterial(@PathVariable Long materialId, Authentication authentication) {
        return movimentacaoRepository.findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(materialId, empresaId(authentication));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Movimentacao> registrar(@Valid @RequestBody MovimentacaoRequest request, Authentication authentication) {
        Usuario logado = usuarioLogado(authentication);

        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado: " + request.getMaterialId()));

        if (!material.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            throw new RecursoNaoEncontradoException("Material não encontrado: " + request.getMaterialId());
        }

        boolean exigeMotivo = request.getTipo() == TipoMovimentacao.PERDA || request.getTipo() == TipoMovimentacao.AVARIA;
        if (exigeMotivo && (request.getObservacao() == null || request.getObservacao().isBlank())) {
            throw new RegraDeNegocioException("Descreva o motivo da " +
                    (request.getTipo() == TipoMovimentacao.PERDA ? "perda" : "avaria") + " no campo de observação.");
        }

        boolean reduzEstoque = request.getTipo() != TipoMovimentacao.ENTRADA;

        if (reduzEstoque) {
            if (material.getQuantidadeAtual() < request.getQuantidade()) {
                throw new RegraDeNegocioException(
                        "Estoque insuficiente para " + material.getNome() +
                        ". Disponível: " + material.getQuantidadeAtual() + " " + material.getUnidade());
            }
            material.setQuantidadeAtual(material.getQuantidadeAtual() - request.getQuantidade());
        } else {
            material.setQuantidadeAtual(material.getQuantidadeAtual() + request.getQuantidade());
        }
        materialRepository.save(material);

        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setMaterial(material);
        movimentacao.setTipo(request.getTipo());
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setResponsavel(request.getResponsavel());
        movimentacao.setObservacao(request.getObservacao());
        movimentacao.setEmpresa(logado.getEmpresa());

        Movimentacao salva = movimentacaoRepository.save(movimentacao);
        return ResponseEntity.ok(salva);
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
