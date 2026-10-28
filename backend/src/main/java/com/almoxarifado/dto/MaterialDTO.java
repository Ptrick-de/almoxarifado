package com.almoxarifado.dto;

import com.almoxarifado.model.Categoria;
import com.almoxarifado.model.Material;
import com.almoxarifado.model.MaterialFornecedor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Visão de um material (variação) com disponibilidade calculada a partir de estoque e empréstimos reais. */
public class MaterialDTO {

    public static class FornecedorPrecoDTO {
        private Long fornecedorId;
        private String fornecedorNome;
        private BigDecimal precoUnitario;
        private LocalDateTime dataAtualizacao;

        public FornecedorPrecoDTO(MaterialFornecedor v) {
            this.fornecedorId = v.getFornecedor().getId();
            this.fornecedorNome = v.getFornecedor().getNome();
            this.precoUnitario = v.getPrecoUnitario();
            this.dataAtualizacao = v.getDataAtualizacao();
        }

        public Long getFornecedorId() { return fornecedorId; }
        public String getFornecedorNome() { return fornecedorNome; }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    }

    private final Long id;
    private final String codigo;
    private final String nome;
    private final String descricao;
    private final String marca;
    private final String modelo;
    private final String ca;
    private final String unidade;
    private final Integer quantidadeAtual;
    private final Integer quantidadeMinima;
    private final BigDecimal precoUnitario;
    private final String localizacao;
    private final LocalDateTime dataCadastro;
    private final boolean ativo;
    private final Categoria categoria;
    private final List<FornecedorPrecoDTO> fornecedores;
    private final int quantidadeEmprestada;
    private final int quantidadeTotal;
    private final int quantidadeReservada;
    private final int quantidadeDisponivel;
    private final String situacao;

    public MaterialDTO(Material m, List<FornecedorPrecoDTO> fornecedores, int emprestada) {
        this.id = m.getId();
        this.codigo = m.getCodigo();
        this.nome = m.getNome();
        this.descricao = m.getDescricao();
        this.marca = m.getMarca();
        this.modelo = m.getModelo();
        this.ca = m.getCa();
        this.unidade = m.getUnidade();
        this.quantidadeAtual = m.getQuantidadeAtual();
        this.quantidadeMinima = m.getQuantidadeMinima();
        this.precoUnitario = m.getPrecoUnitario();
        this.localizacao = m.getLocalizacao();
        this.dataCadastro = m.getDataCadastro();
        this.ativo = m.isAtivo();
        this.categoria = m.getCategoria();
        this.fornecedores = fornecedores;
        this.quantidadeEmprestada = emprestada;
        // O saldo do almoxarifado já exclui o que está emprestado; o total patrimonial soma os dois.
        this.quantidadeTotal = m.getQuantidadeAtual() + emprestada;
        // O sistema não possui o conceito de reserva: sempre zero.
        this.quantidadeReservada = 0;
        this.quantidadeDisponivel = m.isAtivo() ? m.getQuantidadeAtual() : 0;
        this.situacao = calcularSituacao(m, emprestada);
    }

    public static String calcularSituacao(Material m, int emprestada) {
        if (!m.isAtivo()) return "INDISPONIVEL";
        int disponivel = m.getQuantidadeAtual();
        if (disponivel <= 0) return emprestada > 0 ? "EMPRESTADO" : "SEM_ESTOQUE";
        if (disponivel <= m.getQuantidadeMinima()) return "ESTOQUE_BAIXO";
        return "DISPONIVEL";
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getCa() { return ca; }
    public String getUnidade() { return unidade; }
    public Integer getQuantidadeAtual() { return quantidadeAtual; }
    public Integer getQuantidadeMinima() { return quantidadeMinima; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public String getLocalizacao() { return localizacao; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public boolean isAtivo() { return ativo; }
    public Categoria getCategoria() { return categoria; }
    public List<FornecedorPrecoDTO> getFornecedores() { return fornecedores; }
    public int getQuantidadeEmprestada() { return quantidadeEmprestada; }
    public int getQuantidadeTotal() { return quantidadeTotal; }
    public int getQuantidadeReservada() { return quantidadeReservada; }
    public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public String getSituacao() { return situacao; }
}
