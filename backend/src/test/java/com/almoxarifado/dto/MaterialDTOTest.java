package com.almoxarifado.dto;

import com.almoxarifado.model.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialDTOTest {

    private Material material(int atual, int minimo, boolean ativo) {
        Material m = new Material();
        m.setId(1L);
        m.setCodigo("MAT-001");
        m.setNome("Furadeira");
        m.setUnidade("UNIDADE");
        m.setQuantidadeAtual(atual);
        m.setQuantidadeMinima(minimo);
        m.setAtivo(ativo);
        return m;
    }

    @Test
    void disponivelQuandoSaldoAcimaDoMinimo() {
        assertThat(MaterialDTO.calcularSituacao(material(10, 2, true), 0)).isEqualTo("DISPONIVEL");
    }

    @Test
    void estoqueBaixoQuandoSaldoNoMinimoOuAbaixo() {
        assertThat(MaterialDTO.calcularSituacao(material(2, 2, true), 0)).isEqualTo("ESTOQUE_BAIXO");
        assertThat(MaterialDTO.calcularSituacao(material(1, 2, true), 0)).isEqualTo("ESTOQUE_BAIXO");
    }

    @Test
    void semEstoqueQuandoSaldoZeroENadaEmprestado() {
        assertThat(MaterialDTO.calcularSituacao(material(0, 2, true), 0)).isEqualTo("SEM_ESTOQUE");
    }

    @Test
    void emprestadoQuandoSaldoZeroEHaItensFora() {
        assertThat(MaterialDTO.calcularSituacao(material(0, 2, true), 3)).isEqualTo("EMPRESTADO");
    }

    @Test
    void indisponivelQuandoInativoMesmoComSaldo() {
        assertThat(MaterialDTO.calcularSituacao(material(50, 2, false), 0)).isEqualTo("INDISPONIVEL");
    }

    @Test
    void totalSomaSaldoEEmprestado_eDisponivelNuncaIncluiEmprestado() {
        MaterialDTO dto = new MaterialDTO(material(4, 1, true), List.of(), 3);
        assertThat(dto.getQuantidadeTotal()).isEqualTo(7);
        assertThat(dto.getQuantidadeEmprestada()).isEqualTo(3);
        assertThat(dto.getQuantidadeDisponivel()).isEqualTo(4);
        assertThat(dto.getQuantidadeReservada()).isZero();
    }

    @Test
    void materialInativoNaoTemQuantidadeDisponivel() {
        MaterialDTO dto = new MaterialDTO(material(9, 1, false), List.of(), 0);
        assertThat(dto.getQuantidadeDisponivel()).isZero();
        assertThat(dto.getSituacao()).isEqualTo("INDISPONIVEL");
    }
}
