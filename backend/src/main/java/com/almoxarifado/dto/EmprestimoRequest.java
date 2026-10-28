package com.almoxarifado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EmprestimoRequest {

    @NotNull(message = "Material é obrigatório")
    private Long materialId;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @NotBlank(message = "Informe o nome de quem está retirando o material")
    @Size(max = 150, message = "Nome de quem retira deve ter no máximo 150 caracteres")
    private String tomador;

    @Size(max = 100, message = "Setor deve ter no máximo 100 caracteres")
    private String setor;

    @Size(max = 150, message = "Responsável pela entrega deve ter no máximo 150 caracteres")
    private String responsavelEntrega;

    private LocalDate dataEmprestimo;

    @NotNull(message = "Informe a data prevista de devolução")
    private LocalDate dataPrevistaDevolucao;

    @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
    private String observacao;

    @Size(max = 64, message = "Chave de operação inválida")
    private String chaveIdempotencia;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getTomador() { return tomador; }
    public void setTomador(String tomador) { this.tomador = tomador; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public String getResponsavelEntrega() { return responsavelEntrega; }
    public void setResponsavelEntrega(String responsavelEntrega) { this.responsavelEntrega = responsavelEntrega; }

    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDate dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }

    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDate d) { this.dataPrevistaDevolucao = d; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String c) { this.chaveIdempotencia = c; }
}
