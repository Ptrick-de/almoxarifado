package com.almoxarifado.controller;

import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.model.Fornecedor;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.FornecedorRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorController(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    public List<Fornecedor> listar(Authentication authentication) {
        return fornecedorRepository.findAllByEmpresaIdOrderByNome(empresaId(authentication));
    }

    @GetMapping("/{id}")
    public Fornecedor buscarPorId(@PathVariable Long id, Authentication authentication) {
        return buscarDaMesmaEmpresa(id, authentication);
    }

    @PostMapping
    public ResponseEntity<Fornecedor> criar(@Valid @RequestBody Fornecedor fornecedor, Authentication authentication) {
        fornecedor.setEmpresa(usuarioLogado(authentication).getEmpresa());
        Fornecedor salvo = fornecedorRepository.save(fornecedor);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    public Fornecedor atualizar(@PathVariable Long id, @Valid @RequestBody Fornecedor dados, Authentication authentication) {
        Fornecedor fornecedor = buscarDaMesmaEmpresa(id, authentication);
        fornecedor.setNome(dados.getNome());
        fornecedor.setCnpj(dados.getCnpj());
        fornecedor.setTelefone(dados.getTelefone());
        fornecedor.setEmail(dados.getEmail());
        fornecedor.setEndereco(dados.getEndereco());
        return fornecedorRepository.save(fornecedor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        Fornecedor fornecedor = buscarDaMesmaEmpresa(id, authentication);
        fornecedorRepository.deleteById(fornecedor.getId());
        return ResponseEntity.noContent().build();
    }

    private Fornecedor buscarDaMesmaEmpresa(Long id, Authentication authentication) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
        if (!fornecedor.getEmpresa().getId().equals(empresaId(authentication))) {
            throw new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id);
        }
        return fornecedor;
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
