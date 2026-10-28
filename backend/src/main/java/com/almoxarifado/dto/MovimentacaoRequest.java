package com.almoxarifado.dto;

import com.almoxarifado.model.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimentacaoRequest {

    @NotNull(message = "Material é obrigatório")
    private Long materialId;

    @NotNull(message = "Tipo de movimentação é obrigatório")
    private TipoMovimentacao tipo;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @Size(max = 150, message = "Responsável deve ter no máximo 150 caracteres")
    private String responsavel;

    @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
    private String observacao;

    /** Quem recebeu o material (obrigatório na saída para consumo). */
    @Size(max = 150, message = "Nome de quem recebeu deve ter no máximo 150 caracteres")
    private String destinatario;

    @Size(max = 100, message = "Setor deve ter no máximo 100 caracteres")
    private String setor;

    private Long fornecedorId;

    @PositiveOrZero(message = "Preço unitário não pode ser negativo")
    private BigDecimal precoUnitario;

    /** Data em que a operação ocorreu (padrão: hoje). */
    private LocalDate dataMovimento;

    /** Identificador gerado pelo formulário para evitar lançamentos duplicados em reenvios. */
    @Size(max = 64, message = "Chave de operação inválida")
    private String chaveIdempotencia;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

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

    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public LocalDate getDataMovimento() { return dataMovimento; }
    public void setDataMovimento(LocalDate dataMovimento) { this.dataMovimento = dataMovimento; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
}
