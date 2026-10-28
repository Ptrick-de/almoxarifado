package com.almoxarifado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MaterialRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String nome;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @Size(max = 80, message = "Marca deve ter no máximo 80 caracteres")
    private String marca;

    @Size(max = 80, message = "Modelo deve ter no máximo 80 caracteres")
    private String modelo;

    @Size(max = 30, message = "C.A. deve ter no máximo 30 caracteres")
    private String ca;

    @NotBlank(message = "Unidade de medida é obrigatória")
    private String unidade;

    /** Só é usada no cadastro (saldo inicial); depois disso o saldo muda apenas por movimentações. */
    @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
    private Integer quantidadeAtual = 0;

    @NotNull(message = "Quantidade mínima é obrigatória")
    @PositiveOrZero(message = "Quantidade mínima não pode ser negativa")
    private Integer quantidadeMinima = 0;

    @PositiveOrZero(message = "Preço unitário não pode ser negativo")
    private BigDecimal precoUnitario;

    @Size(max = 255, message = "Localização deve ter no máximo 255 caracteres")
    private String localizacao;

    private Long categoriaId;

    private Boolean ativo;

    /** Código de um produto já cadastrado, para criar uma nova variação (marca/modelo/C.A.) dele. */
    private String codigoBase;

    @Valid
    private List<FornecedorPreco> fornecedores = new ArrayList<>();

    public static class FornecedorPreco {
        @NotNull(message = "Fornecedor é obrigatório")
        private Long fornecedorId;

        @NotNull(message = "Informe o preço do fornecedor")
        @PositiveOrZero(message = "Preço do fornecedor não pode ser negativo")
        private BigDecimal precoUnitario;

        public Long getFornecedorId() { return fornecedorId; }
        public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getCa() { return ca; }
    public void setCa(String ca) { this.ca = ca; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public Integer getQuantidadeAtual() { return quantidadeAtual; }
    public void setQuantidadeAtual(Integer quantidadeAtual) { this.quantidadeAtual = quantidadeAtual; }

    public Integer getQuantidadeMinima() { return quantidadeMinima; }
    public void setQuantidadeMinima(Integer quantidadeMinima) { this.quantidadeMinima = quantidadeMinima; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getCodigoBase() { return codigoBase; }
    public void setCodigoBase(String codigoBase) { this.codigoBase = codigoBase; }

    public List<FornecedorPreco> getFornecedores() { return fornecedores; }
    public void setFornecedores(List<FornecedorPreco> fornecedores) {
        this.fornecedores = fornecedores == null ? new ArrayList<>() : fornecedores;
    }
}
