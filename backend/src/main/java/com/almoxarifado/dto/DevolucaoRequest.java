package com.almoxarifado.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DevolucaoRequest {

    @NotNull(message = "Informe a quantidade devolvida")
    @Positive(message = "Quantidade devolvida deve ser maior que zero")
    private Integer quantidade;

    /** Data efetiva da devolução (padrão: hoje). */
    private LocalDate dataDevolucao;

    /** Quem recebeu a devolução no almoxarifado. */
    @Size(max = 150, message = "Responsável deve ter no máximo 150 caracteres")
    private String responsavel;

    @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
    private String observacao;

    @Size(max = 64, message = "Chave de operação inválida")
    private String chaveIdempotencia;

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDate dataDevolucao) { this.dataDevolucao = dataDevolucao; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String c) { this.chaveIdempotencia = c; }
}
