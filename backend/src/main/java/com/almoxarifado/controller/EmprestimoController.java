package com.almoxarifado.controller;

import com.almoxarifado.dto.DevolucaoRequest;
import com.almoxarifado.dto.EmprestimoRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.model.Emprestimo;
import com.almoxarifado.model.Movimentacao;
import com.almoxarifado.model.StatusEmprestimo;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.EmprestimoRepository;
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
@RequestMapping("/api/emprestimos")
public class EmprestimoController {

    private static final int LIMITE = 500;

    private final EmprestimoRepository emprestimoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final MovimentacaoService movimentacaoService;

    public EmprestimoController(EmprestimoRepository emprestimoRepository,
                                MovimentacaoRepository movimentacaoRepository,
                                MovimentacaoService movimentacaoService) {
        this.emprestimoRepository = emprestimoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.movimentacaoService = movimentacaoService;
    }

    /**
     * Filtros: material, busca (texto do material), pessoa (quem retirou ou quem entregou), setor, período do empréstimo
     * e situação (ABERTOS, ATIVO, ATRASADO, DEVOLVIDO).
     */
    @GetMapping
    public List<Emprestimo> listar(@RequestParam(required = false) Long materialId,
                                   @RequestParam(required = false) String busca,
                                   @RequestParam(required = false) String pessoa,
                                   @RequestParam(required = false) String setor,
                                   @RequestParam(required = false) String situacao,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                   Authentication authentication) {
        Long empresaId = empresaId(authentication);
        LocalDate hoje = LocalDate.now();
        Specification<Emprestimo> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("empresa").get("id"), empresaId));
            Join<Object, Object> material = root.join("material");
            if (materialId != null) p.add(cb.equal(material.get("id"), materialId));
            if (busca != null && !busca.isBlank()) {
                String like = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(material.get("nome")), like),
                        cb.like(cb.lower(material.get("codigo")), like),
                        cb.like(cb.lower(material.get("marca")), like),
                        cb.like(cb.lower(material.get("modelo")), like)));
            }
            if (pessoa != null && !pessoa.isBlank()) {
                String like = "%" + pessoa.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("tomador")), like),
                        cb.like(cb.lower(root.get("responsavelEntrega")), like)));
            }
            if (setor != null && !setor.isBlank()) {
                p.add(cb.like(cb.lower(root.get("setor")), "%" + setor.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (de != null) p.add(cb.greaterThanOrEqualTo(root.get("dataEmprestimo"), de));
            if (ate != null) p.add(cb.lessThanOrEqualTo(root.get("dataEmprestimo"), ate));
            if (situacao != null && !situacao.isBlank()) {
                switch (situacao.trim().toUpperCase(Locale.ROOT)) {
                    case "ABERTOS" -> p.add(cb.equal(root.get("status"), StatusEmprestimo.ATIVO));
                    case "ATIVO" -> {
                        p.add(cb.equal(root.get("status"), StatusEmprestimo.ATIVO));
                        p.add(cb.greaterThanOrEqualTo(root.get("dataPrevistaDevolucao"), hoje));
                    }
                    case "ATRASADO" -> {
                        p.add(cb.equal(root.get("status"), StatusEmprestimo.ATIVO));
                        p.add(cb.lessThan(root.get("dataPrevistaDevolucao"), hoje));
                    }
                    case "DEVOLVIDO" -> p.add(cb.equal(root.get("status"), StatusEmprestimo.DEVOLVIDO));
                    default -> p.add(cb.disjunction());
                }
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return emprestimoRepository.findAll(spec,
                PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "dataEmprestimo", "id"))).getContent();
    }

    @GetMapping("/{id}")
    public Emprestimo buscarPorId(@PathVariable Long id, Authentication authentication) {
        return emprestimoRepository.findByIdAndEmpresaId(id, empresaId(authentication))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + id));
    }

    /** Devoluções (e a saída) registradas para o empréstimo, em ordem cronológica. */
    @GetMapping("/{id}/historico")
    public List<Movimentacao> historico(@PathVariable Long id, Authentication authentication) {
        Long empresaId = empresaId(authentication);
        emprestimoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + id));
        return movimentacaoRepository.findAllByEmprestimoIdAndEmpresaIdOrderByDataMovimentacaoAsc(id, empresaId);
    }

    @PostMapping
    public ResponseEntity<Emprestimo> emprestar(@Valid @RequestBody EmprestimoRequest request, Authentication authentication) {
        return ResponseEntity.ok(movimentacaoService.emprestar(request, usuarioLogado(authentication)));
    }

    @PostMapping("/{id}/devolucoes")
    public ResponseEntity<Emprestimo> devolver(@PathVariable Long id, @Valid @RequestBody DevolucaoRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(movimentacaoService.devolver(id, request, usuarioLogado(authentication)));
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
