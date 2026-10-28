package com.almoxarifado.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cada linha representa uma VARIAÇÃO do produto (marca + modelo + C.A.).
 * O "código" identifica o produto e é compartilhado entre as variações;
 * o identificador único da variação é o id interno (e a combinação
 * código + marca + modelo + C.A. + empresa não pode se repetir).
 */
@Entity
@Table(name = "materiais", uniqueConstraints =
        @UniqueConstraint(name = "uk_material_variacao", columnNames = {"codigo", "marca", "modelo", "ca", "empresa_id"}))
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false, length = 80, columnDefinition = "varchar(80) not null default ''")
    private String marca = "";

    @Column(nullable = false, length = 80, columnDefinition = "varchar(80) not null default ''")
    private String modelo = "";

    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) not null default ''")
    private String ca = "";

    @NotBlank(message = "Unidade de medida é obrigatória")
    private String unidade;

    @NotNull(message = "Quantidade atual é obrigatória")
    @PositiveOrZero(message = "Quantidade atual não pode ser negativa")
    @Column(name = "quantidade_atual")
    private Integer quantidadeAtual = 0;

    @NotNull(message = "Quantidade mínima é obrigatória")
    @PositiveOrZero(message = "Quantidade mínima não pode ser negativa")
    @Column(name = "quantidade_minima")
    private Integer quantidadeMinima = 0;

    @PositiveOrZero(message = "Preço unitário não pode ser negativo")
    @Column(name = "preco_unitario")
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    private String localizacao;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro = LocalDateTime.now();

    /** Inativar preserva o histórico: o material deixa de aceitar novas movimentações. */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"materiais", "empresa"})
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnore
    private Empresa empresa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca == null ? "" : marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo == null ? "" : modelo; }

    public String getCa() { return ca; }
    public void setCa(String ca) { this.ca = ca == null ? "" : ca; }

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

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    @Transient
    public boolean isEstoqueBaixo() {
        return quantidadeAtual != null && quantidadeMinima != null && quantidadeAtual <= quantidadeMinima;
    }
}
