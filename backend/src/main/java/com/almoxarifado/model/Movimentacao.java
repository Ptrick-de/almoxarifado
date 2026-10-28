package com.almoxarifado.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes", uniqueConstraints = @UniqueConstraint(
        name = "uk_movimentacao_chave", columnNames = {"empresa_id", "chave_idempotencia"}))
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    @JsonIgnoreProperties({"categoria"})
    @NotNull(message = "Material é obrigatório")
    private Material material;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    @NotNull(message = "Tipo de movimentação é obrigatório")
    private TipoMovimentacao tipo;

    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    /** Responsável pelo registro / pela entrega. */
    private String responsavel;

    @Column(length = 1000)
    private String observacao;

    /** Quem recebeu o material (saída/empréstimo) ou devolveu (devolução). */
    private String destinatario;

    private String setor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fornecedor_id")
    @JsonIgnoreProperties({"materiais", "vinculos", "empresa"})
    private Fornecedor fornecedor;

    @Column(name = "preco_unitario")
    private BigDecimal precoUnitario;

    /** Preenchido nas movimentações EMPRESTIMO e DEVOLUCAO. */
    @Column(name = "emprestimo_id")
    private Long emprestimoId;

    /** Saldo disponível logo após a movimentação (trilha de auditoria). */
    @Column(name = "saldo_apos")
    private Integer saldoApos;

    @Column(name = "chave_idempotencia", length = 64)
    @JsonIgnore
    private String chaveIdempotencia;

    @Column(name = "data_movimentacao")
    private LocalDateTime dataMovimentacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnore
    private Empresa empresa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public Long getEmprestimoId() { return emprestimoId; }
    public void setEmprestimoId(Long emprestimoId) { this.emprestimoId = emprestimoId; }

    public Integer getSaldoApos() { return saldoApos; }
    public void setSaldoApos(Integer saldoApos) { this.saldoApos = saldoApos; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public LocalDateTime getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(LocalDateTime dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}
