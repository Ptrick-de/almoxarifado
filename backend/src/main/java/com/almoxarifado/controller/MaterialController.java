package com.almoxarifado.controller;

import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.Categoria;
import com.almoxarifado.model.Fornecedor;
import com.almoxarifado.model.Material;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.CategoriaRepository;
import com.almoxarifado.repository.FornecedorRepository;
import com.almoxarifado.repository.MaterialRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialRepository materialRepository;
    private final CategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;

    public MaterialController(MaterialRepository materialRepository,
                               CategoriaRepository categoriaRepository,
                               FornecedorRepository fornecedorRepository) {
        this.materialRepository = materialRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    public List<Material> listar(@RequestParam(required = false) String busca, Authentication authentication) {
        Long empresaId = empresaId(authentication);
        if (busca != null && !busca.isBlank()) {
            return materialRepository.buscarPorNomeOuCodigo(empresaId, busca);
        }
        return materialRepository.findAllByEmpresaIdOrderByNome(empresaId);
    }

    @GetMapping("/estoque-baixo")
    public List<Material> listarEstoqueBaixo(Authentication authentication) {
        return materialRepository.findEstoqueBaixoByEmpresa(empresaId(authentication));
    }

    @GetMapping("/{id}")
    public Material buscarPorId(@PathVariable Long id, Authentication authentication) {
        return buscarDaMesmaEmpresa(id, authentication);
    }

    @PostMapping
    public ResponseEntity<Material> criar(@Valid @RequestBody Material material, Authentication authentication) {
        Usuario logado = usuarioLogado(authentication);
        if (materialRepository.existsByCodigoAndEmpresaId(material.getCodigo(), logado.getEmpresa().getId())) {
            throw new RegraDeNegocioException("Já existe um material com o código " + material.getCodigo());
        }
        material.setEmpresa(logado.getEmpresa());
        vincularRelacionamentos(material, logado.getEmpresa().getId());
        Material salvo = materialRepository.save(material);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    public Material atualizar(@PathVariable Long id, @Valid @RequestBody Material dados, Authentication authentication) {
        Usuario logado = usuarioLogado(authentication);
        Material material = buscarDaMesmaEmpresa(id, authentication);

        materialRepository.findByCodigoAndEmpresaId(dados.getCodigo(), logado.getEmpresa().getId()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new RegraDeNegocioException("Já existe um material com o código " + dados.getCodigo());
            }
        });

        material.setCodigo(dados.getCodigo());
        material.setNome(dados.getNome());
        material.setDescricao(dados.getDescricao());
        material.setUnidade(dados.getUnidade());
        material.setQuantidadeAtual(dados.getQuantidadeAtual());
        material.setQuantidadeMinima(dados.getQuantidadeMinima());
        material.setPrecoUnitario(dados.getPrecoUnitario());
        material.setLocalizacao(dados.getLocalizacao());
        material.setCategoria(dados.getCategoria());
        material.setFornecedor(dados.getFornecedor());
        vincularRelacionamentos(material, logado.getEmpresa().getId());

        return materialRepository.save(material);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        Material material = buscarDaMesmaEmpresa(id, authentication);
        materialRepository.deleteById(material.getId());
        return ResponseEntity.noContent().build();
    }

    private Material buscarDaMesmaEmpresa(Long id, Authentication authentication) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado: " + id));
        if (!material.getEmpresa().getId().equals(empresaId(authentication))) {
            throw new RecursoNaoEncontradoException("Material não encontrado: " + id);
        }
        return material;
    }

    private void vincularRelacionamentos(Material material, Long empresaId) {
        if (material.getCategoria() != null && material.getCategoria().getId() != null) {
            Categoria categoria = categoriaRepository.findById(material.getCategoria().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
            if (!categoria.getEmpresa().getId().equals(empresaId)) {
                throw new RecursoNaoEncontradoException("Categoria não encontrada");
            }
            material.setCategoria(categoria);
        } else {
            material.setCategoria(null);
        }

        if (material.getFornecedor() != null && material.getFornecedor().getId() != null) {
            Fornecedor fornecedor = fornecedorRepository.findById(material.getFornecedor().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
            if (!fornecedor.getEmpresa().getId().equals(empresaId)) {
                throw new RecursoNaoEncontradoException("Fornecedor não encontrado");
            }
            material.setFornecedor(fornecedor);
        } else {
            material.setFornecedor(null);
        }
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
