package com.almoxarifado.model;

/**
 * ENTRADA e DEVOLUCAO aumentam o saldo disponível no almoxarifado.
 * SAIDA (consumo), EMPRESTIMO, PERDA e AVARIA reduzem o saldo disponível.
 */
public enum TipoMovimentacao {
    ENTRADA, SAIDA, PERDA, AVARIA, EMPRESTIMO, DEVOLUCAO;

    public boolean aumentaEstoque() {
        return this == ENTRADA || this == DEVOLUCAO;
    }
}
