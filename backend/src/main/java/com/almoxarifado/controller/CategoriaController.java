package com.almoxarifado.controller;

import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.model.Categoria;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.CategoriaRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<Categoria> listar(Authentication authentication) {
        return categoriaRepository.findAllByEmpresaIdOrderByNome(empresaId(authentication));
    }

    @GetMapping("/{id}")
    public Categoria buscarPorId(@PathVariable Long id, Authentication authentication) {
        return buscarDaMesmaEmpresa(id, authentication);
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(@Valid @RequestBody Categoria categoria, Authentication authentication) {
        categoria.setEmpresa(usuarioLogado(authentication).getEmpresa());
        Categoria salva = categoriaRepository.save(categoria);
        return ResponseEntity.ok(salva);
    }

    @PutMapping("/{id}")
    public Categoria atualizar(@PathVariable Long id, @Valid @RequestBody Categoria dados, Authentication authentication) {
        Categoria categoria = buscarDaMesmaEmpresa(id, authentication);
        categoria.setNome(dados.getNome());
        categoria.setDescricao(dados.getDescricao());
        return categoriaRepository.save(categoria);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        Categoria categoria = buscarDaMesmaEmpresa(id, authentication);
        categoriaRepository.deleteById(categoria.getId());
        return ResponseEntity.noContent().build();
    }

    private Categoria buscarDaMesmaEmpresa(Long id, Authentication authentication) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
        if (!categoria.getEmpresa().getId().equals(empresaId(authentication))) {
            throw new RecursoNaoEncontradoException("Categoria não encontrada: " + id);
        }
        return categoria;
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
