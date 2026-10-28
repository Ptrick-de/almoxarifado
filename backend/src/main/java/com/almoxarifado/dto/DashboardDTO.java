package com.almoxarifado.dto;

import com.almoxarifado.model.Material;
import com.almoxarifado.model.Movimentacao;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {
    private long totalMateriais;
    private long totalCategorias;
    private long totalFornecedores;
    private long itensEstoqueBaixo;
    private BigDecimal valorTotalEstoque;
    private List<Material> materiaisEstoqueBaixo;
    private List<Movimentacao> ultimasMovimentacoes;
    private long itensPerdidosMes;
    private BigDecimal valorPerdidoMes;
    private long emprestimosAbertos;
    private long emprestimosAtrasados;

    public long getTotalMateriais() { return totalMateriais; }
    public void setTotalMateriais(long totalMateriais) { this.totalMateriais = totalMateriais; }

    public long getTotalCategorias() { return totalCategorias; }
    public void setTotalCategorias(long totalCategorias) { this.totalCategorias = totalCategorias; }

    public long getTotalFornecedores() { return totalFornecedores; }
    public void setTotalFornecedores(long totalFornecedores) { this.totalFornecedores = totalFornecedores; }

    public long getItensEstoqueBaixo() { return itensEstoqueBaixo; }
    public void setItensEstoqueBaixo(long itensEstoqueBaixo) { this.itensEstoqueBaixo = itensEstoqueBaixo; }

    public BigDecimal getValorTotalEstoque() { return valorTotalEstoque; }
    public void setValorTotalEstoque(BigDecimal valorTotalEstoque) { this.valorTotalEstoque = valorTotalEstoque; }

    public List<Material> getMateriaisEstoqueBaixo() { return materiaisEstoqueBaixo; }
    public void setMateriaisEstoqueBaixo(List<Material> materiaisEstoqueBaixo) { this.materiaisEstoqueBaixo = materiaisEstoqueBaixo; }

    public List<Movimentacao> getUltimasMovimentacoes() { return ultimasMovimentacoes; }
    public void setUltimasMovimentacoes(List<Movimentacao> ultimasMovimentacoes) { this.ultimasMovimentacoes = ultimasMovimentacoes; }

    public long getItensPerdidosMes() { return itensPerdidosMes; }
    public void setItensPerdidosMes(long itensPerdidosMes) { this.itensPerdidosMes = itensPerdidosMes; }

    public BigDecimal getValorPerdidoMes() { return valorPerdidoMes; }
    public void setValorPerdidoMes(BigDecimal valorPerdidoMes) { this.valorPerdidoMes = valorPerdidoMes; }

    public long getEmprestimosAbertos() { return emprestimosAbertos; }
    public void setEmprestimosAbertos(long emprestimosAbertos) { this.emprestimosAbertos = emprestimosAbertos; }

    public long getEmprestimosAtrasados() { return emprestimosAtrasados; }
    public void setEmprestimosAtrasados(long emprestimosAtrasados) { this.emprestimosAtrasados = emprestimosAtrasados; }
}
