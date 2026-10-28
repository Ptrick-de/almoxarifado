package com.almoxarifado.controller;

import com.almoxarifado.dto.MovimentacaoRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.model.Movimentacao;
import com.almoxarifado.model.TipoMovimentacao;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.MovimentacaoRepository;
import com.almoxarifado.service.MovimentacaoService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private static final int LIMITE = 500;

    private final MovimentacaoRepository movimentacaoRepository;
    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoRepository movimentacaoRepository,
                                  MovimentacaoService movimentacaoService) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.movimentacaoService = movimentacaoService;
    }

    /** Histórico com filtros combináveis (material, responsável, tipo, período, fornecedor). Mais recentes primeiro. */
    @GetMapping
    public List<Movimentacao> listar(@RequestParam(required = false) Long materialId,
                                     @RequestParam(required = false) String busca,
                                     @RequestParam(required = false) String responsavel,
                                     @RequestParam(required = false) String tipo,
                                     @RequestParam(required = false) Long fornecedorId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                     Authentication authentication) {
        Long empresaId = empresaId(authentication);
        Specification<Movimentacao> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("empresa").get("id"), empresaId));
            Join<Object, Object> material = root.join("material");
            if (materialId != null) p.add(cb.equal(material.get("id"), materialId));
            if (fornecedorId != null) p.add(cb.equal(root.get("fornecedor").get("id"), fornecedorId));
            if (tipo != null && !tipo.isBlank()) {
                try {
                    p.add(cb.equal(root.get("tipo"), TipoMovimentacao.valueOf(tipo.trim().toUpperCase(Locale.ROOT))));
                } catch (IllegalArgumentException e) {
                    p.add(cb.disjunction());
                }
            }
            if (busca != null && !busca.isBlank()) {
                String like = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(material.get("nome")), like),
                        cb.like(cb.lower(material.get("codigo")), like),
                        cb.like(cb.lower(material.get("marca")), like),
                        cb.like(cb.lower(material.get("modelo")), like)));
            }
            if (responsavel != null && !responsavel.isBlank()) {
                String like = "%" + responsavel.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("responsavel")), like),
                        cb.like(cb.lower(root.get("destinatario")), like)));
            }
            if (de != null) p.add(cb.greaterThanOrEqualTo(root.get("dataMovimentacao"), de.atStartOfDay()));
            if (ate != null) p.add(cb.lessThan(root.get("dataMovimentacao"), ate.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return movimentacaoRepository.findAll(spec,
                PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "dataMovimentacao", "id"))).getContent();
    }

    @GetMapping("/{id}")
    public Movimentacao buscarPorId(@PathVariable Long id, Authentication authentication) {
        return movimentacaoRepository.findById(id)
                .filter(m -> m.getEmpresa().getId().equals(empresaId(authentication)))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentação não encontrada: " + id));
    }

    @GetMapping("/material/{materialId}")
    public List<Movimentacao> listarPorMaterial(@PathVariable Long materialId, Authentication authentication) {
        return movimentacaoRepository.findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(materialId, empresaId(authentication));
    }

    /** Entrada, saída para consumo, perda e avaria (empréstimos/devoluções usam /api/emprestimos). */
    @PostMapping
    public ResponseEntity<Movimentacao> registrar(@Valid @RequestBody MovimentacaoRequest request, Authentication authentication) {
        return ResponseEntity.ok(movimentacaoService.registrar(request, usuarioLogado(authentication)));
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
