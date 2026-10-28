package com.almoxarifado.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emprestimos", uniqueConstraints =
        @UniqueConstraint(name = "uk_emprestimo_chave", columnNames = {"empresa_id", "chave_idempotencia"}))
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    @JsonIgnoreProperties({"categoria"})
    private Material material;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "quantidade_devolvida", nullable = false, columnDefinition = "int not null default 0")
    private Integer quantidadeDevolvida = 0;

    @Column(nullable = false)
    private String tomador;

    private String setor;

    @Column(name = "responsavel_entrega", nullable = false)
    private String responsavelEntrega;

    @Column(name = "data_emprestimo", nullable = false)
    private LocalDate dataEmprestimo;

    @Column(name = "data_prevista_devolucao", nullable = false)
    private LocalDate dataPrevistaDevolucao;

    /** Preenchida quando o último item é devolvido. */
    @Column(name = "data_devolucao_efetiva")
    private LocalDate dataDevolucaoEfetiva;

    @Column(length = 1000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusEmprestimo status = StatusEmprestimo.ATIVO;

    @Column(name = "chave_idempotencia", length = 64)
    @JsonIgnore
    private String chaveIdempotencia;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnore
    private Empresa empresa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getQuantidadeDevolvida() { return quantidadeDevolvida; }
    public void setQuantidadeDevolvida(Integer quantidadeDevolvida) { this.quantidadeDevolvida = quantidadeDevolvida; }

    public String getTomador() { return tomador; }
    public void setTomador(String tomador) { this.tomador = tomador; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public String getResponsavelEntrega() { return responsavelEntrega; }
    public void setResponsavelEntrega(String responsavelEntrega) { this.responsavelEntrega = responsavelEntrega; }

    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDate dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }

    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDate dataPrevistaDevolucao) { this.dataPrevistaDevolucao = dataPrevistaDevolucao; }

    public LocalDate getDataDevolucaoEfetiva() { return dataDevolucaoEfetiva; }
    public void setDataDevolucaoEfetiva(LocalDate dataDevolucaoEfetiva) { this.dataDevolucaoEfetiva = dataDevolucaoEfetiva; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public StatusEmprestimo getStatus() { return status; }
    public void setStatus(StatusEmprestimo status) { this.status = status; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    @Transient
    public int getQuantidadePendente() {
        return quantidade - (quantidadeDevolvida == null ? 0 : quantidadeDevolvida);
    }

    /** ATIVO, ATRASADO (ativo com data prevista vencida) ou DEVOLVIDO. */
    @Transient
    public String getSituacao() {
        if (status == StatusEmprestimo.DEVOLVIDO) return "DEVOLVIDO";
        return dataPrevistaDevolucao != null && dataPrevistaDevolucao.isBefore(LocalDate.now()) ? "ATRASADO" : "ATIVO";
    }
}
