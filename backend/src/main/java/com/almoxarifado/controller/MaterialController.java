package com.almoxarifado.controller;

import com.almoxarifado.dto.MaterialDTO;
import com.almoxarifado.dto.MaterialRequest;
import com.almoxarifado.model.Material;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.MaterialRepository;
import com.almoxarifado.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialRepository materialRepository;

    public MaterialController(MaterialService materialService, MaterialRepository materialRepository) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
    }

    /** Lista com filtros combináveis: busca geral, código, nome, marca, modelo, C.A., fornecedor, categoria e situação. */
    @GetMapping
    public List<MaterialDTO> listar(@RequestParam(required = false) String busca,
                                    @RequestParam(required = false) String codigo,
                                    @RequestParam(required = false) String nome,
                                    @RequestParam(required = false) String marca,
                                    @RequestParam(required = false) String modelo,
                                    @RequestParam(required = false) String ca,
                                    @RequestParam(required = false) String fornecedor,
                                    @RequestParam(required = false) Long fornecedorId,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(required = false) String situacao,
                                    Authentication authentication) {
        MaterialService.Filtro f = new MaterialService.Filtro();
        f.busca = busca;
        f.codigo = codigo;
        f.nome = nome;
        f.marca = marca;
        f.modelo = modelo;
        f.ca = ca;
        f.fornecedor = fornecedor;
        f.fornecedorId = fornecedorId;
        f.categoriaId = categoriaId;
        f.situacao = situacao;
        return materialService.listar(empresaId(authentication), f);
    }

    @GetMapping("/estoque-baixo")
    public List<Material> listarEstoqueBaixo(Authentication authentication) {
        return materialRepository.findEstoqueBaixoByEmpresa(empresaId(authentication));
    }

    @GetMapping("/{id}")
    public MaterialDTO buscarPorId(@PathVariable Long id, Authentication authentication) {
        return materialService.obter(id, empresaId(authentication));
    }

    @PostMapping
    public ResponseEntity<MaterialDTO> criar(@Valid @RequestBody MaterialRequest request, Authentication authentication) {
        return ResponseEntity.ok(materialService.criar(request, usuarioLogado(authentication)));
    }

    @PutMapping("/{id}")
    public MaterialDTO atualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest request,
                                 Authentication authentication) {
        return materialService.atualizar(id, request, empresaId(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        materialService.excluir(id, empresaId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    private Long empresaId(Authentication authentication) {
        return usuarioLogado(authentication).getEmpresa().getId();
    }
}
