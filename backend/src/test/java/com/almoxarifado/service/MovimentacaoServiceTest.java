package com.almoxarifado.service;

import com.almoxarifado.dto.DevolucaoRequest;
import com.almoxarifado.dto.EmprestimoRequest;
import com.almoxarifado.dto.MovimentacaoRequest;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.*;
import com.almoxarifado.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Regras de estoque, empréstimo e devolução testadas sem banco (repositórios simulados). */
class MovimentacaoServiceTest {

    private MovimentacaoRepository movimentacaoRepository;
    private MaterialRepository materialRepository;
    private FornecedorRepository fornecedorRepository;
    private MaterialFornecedorRepository vinculoRepository;
    private EmprestimoRepository emprestimoRepository;

    private MovimentacaoService service;
    private Usuario usuario;
    private Empresa empresa;
    private Material material;

    @BeforeEach
    void preparar() {
        movimentacaoRepository = mock(MovimentacaoRepository.class);
        materialRepository = mock(MaterialRepository.class);
        fornecedorRepository = mock(FornecedorRepository.class);
        vinculoRepository = mock(MaterialFornecedorRepository.class);
        emprestimoRepository = mock(EmprestimoRepository.class);
        service = new MovimentacaoService(movimentacaoRepository, materialRepository, fornecedorRepository,
                vinculoRepository, emprestimoRepository);

        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Empresa Teste");
        usuario = new Usuario();
        usuario.setNome("Operador");
        usuario.setEmpresa(empresa);

        material = new Material();
        material.setId(1L);
        material.setCodigo("MAT-001");
        material.setNome("Furadeira");
        material.setUnidade("UNIDADE");
        material.setQuantidadeAtual(10);
        material.setQuantidadeMinima(1);
        material.setEmpresa(empresa);

        when(materialRepository.buscarParaAtualizar(1L, 1L)).thenReturn(Optional.of(material));
        when(movimentacaoRepository.save(any(Movimentacao.class))).thenAnswer(i -> i.getArgument(0));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(i -> {
            Emprestimo e = i.getArgument(0);
            if (e.getId() == null) e.setId(50L);
            return e;
        });
        when(emprestimoRepository.somarPendentesDoMaterial(anyLong(), any())).thenReturn(0L);
        when(movimentacaoRepository.findByChaveIdempotenciaAndEmpresaId(any(), anyLong())).thenReturn(Optional.empty());
        when(emprestimoRepository.findByChaveIdempotenciaAndEmpresaId(any(), anyLong())).thenReturn(Optional.empty());
    }

    private MovimentacaoRequest movimento(TipoMovimentacao tipo, int quantidade) {
        MovimentacaoRequest r = new MovimentacaoRequest();
        r.setMaterialId(1L);
        r.setTipo(tipo);
        r.setQuantidade(quantidade);
        r.setResponsavel("Responsável");
        return r;
    }

    private EmprestimoRequest emprestimo(int quantidade) {
        EmprestimoRequest r = new EmprestimoRequest();
        r.setMaterialId(1L);
        r.setQuantidade(quantidade);
        r.setTomador("Carlos Lima");
        r.setSetor("Manutenção");
        r.setResponsavelEntrega("Maria");
        r.setDataPrevistaDevolucao(LocalDate.now().plusDays(7));
        return r;
    }

    private Emprestimo emprestimoAtivo(int quantidade, int devolvida) {
        Emprestimo e = new Emprestimo();
        e.setId(50L);
        e.setMaterial(material);
        e.setQuantidade(quantidade);
        e.setQuantidadeDevolvida(devolvida);
        e.setTomador("Carlos Lima");
        e.setResponsavelEntrega("Maria");
        e.setDataEmprestimo(LocalDate.now().minusDays(2));
        e.setDataPrevistaDevolucao(LocalDate.now().plusDays(5));
        e.setStatus(StatusEmprestimo.ATIVO);
        e.setEmpresa(empresa);
        when(emprestimoRepository.buscarParaAtualizar(50L, 1L)).thenReturn(Optional.of(e));
        return e;
    }

    // ---------------------------- entrada / saída ----------------------------

    @Test
    void entradaAumentaOSaldoERegistraSaldoAposNoHistorico() {
        Movimentacao mov = service.registrar(movimento(TipoMovimentacao.ENTRADA, 5), usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(15);
        assertThat(mov.getSaldoApos()).isEqualTo(15);
        assertThat(mov.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        verify(movimentacaoRepository).save(any(Movimentacao.class));
    }

    @Test
    void saidaParaConsumoReduzOSaldo() {
        MovimentacaoRequest r = movimento(TipoMovimentacao.SAIDA, 4);
        r.setDestinatario("Maria Souza");

        Movimentacao mov = service.registrar(r, usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(6);
        assertThat(mov.getSaldoApos()).isEqualTo(6);
        assertThat(mov.getDestinatario()).isEqualTo("Maria Souza");
    }

    @Test
    void saidaAcimaDoSaldoEhBloqueadaENadaEhGravado() {
        MovimentacaoRequest r = movimento(TipoMovimentacao.SAIDA, 11);
        r.setDestinatario("Maria");

        assertThatThrownBy(() -> service.registrar(r, usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
        verify(movimentacaoRepository, never()).save(any(Movimentacao.class));
    }

    @Test
    void saidaSemQuemRecebeuEhRecusada() {
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.SAIDA, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("recebeu");
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void perdaEAvariaExigemJustificativaEReduzemOSaldo() {
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.PERDA, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("perda");
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.AVARIA, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("avaria");
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);

        MovimentacaoRequest perda = movimento(TipoMovimentacao.PERDA, 2);
        perda.setObservacao("Caixa molhada");
        service.registrar(perda, usuario);
        MovimentacaoRequest avaria = movimento(TipoMovimentacao.AVARIA, 3);
        avaria.setObservacao("Quebrou na queda");
        service.registrar(avaria, usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(5);
    }

    @Test
    void baixaAcimaDoDisponivelEhBloqueada() {
        MovimentacaoRequest r = movimento(TipoMovimentacao.AVARIA, 99);
        r.setObservacao("Teste");
        assertThatThrownBy(() -> service.registrar(r, usuario)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void emprestimoEDevolucaoNaoPassamPeloEndpointGenerico() {
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.EMPRESTIMO, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.DEVOLUCAO, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void materialInativoNaoRecebeMovimentacao() {
        material.setAtivo(false);
        assertThatThrownBy(() -> service.registrar(movimento(TipoMovimentacao.ENTRADA, 1), usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void reenvioComAMesmaChaveNaoLancaDeNovo() {
        Movimentacao existente = new Movimentacao();
        existente.setId(7L);
        existente.setMaterial(material);
        when(movimentacaoRepository.findByChaveIdempotenciaAndEmpresaId("abc", 1L)).thenReturn(Optional.of(existente));

        MovimentacaoRequest r = movimento(TipoMovimentacao.ENTRADA, 5);
        r.setChaveIdempotencia("abc");
        Movimentacao devolvida = service.registrar(r, usuario);

        assertThat(devolvida).isSameAs(existente);
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
        verify(movimentacaoRepository, never()).save(any(Movimentacao.class));
    }

    @Test
    void dataFuturaEhRecusada() {
        MovimentacaoRequest r = movimento(TipoMovimentacao.ENTRADA, 1);
        r.setDataMovimento(LocalDate.now().plusDays(3));
        assertThatThrownBy(() -> service.registrar(r, usuario)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void entradaComFornecedorEPrecoAtualizaOPrecoDaqueleFornecedor() {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(9L);
        fornecedor.setNome("Ferragens Sul");
        fornecedor.setEmpresa(empresa);
        when(fornecedorRepository.findById(9L)).thenReturn(Optional.of(fornecedor));
        when(vinculoRepository.findByMaterialIdAndFornecedorId(1L, 9L)).thenReturn(Optional.empty());

        MovimentacaoRequest r = movimento(TipoMovimentacao.ENTRADA, 2);
        r.setFornecedorId(9L);
        r.setPrecoUnitario(new BigDecimal("12.50"));
        service.registrar(r, usuario);

        ArgumentCaptor<MaterialFornecedor> captor = ArgumentCaptor.forClass(MaterialFornecedor.class);
        verify(vinculoRepository).save(captor.capture());
        assertThat(captor.getValue().getFornecedor()).isSameAs(fornecedor);
        assertThat(captor.getValue().getPrecoUnitario()).isEqualByComparingTo("12.50");
        assertThat(material.getPrecoUnitario()).isEqualByComparingTo("12.50");
    }

    // ---------------------------- empréstimo / devolução ----------------------------

    @Test
    void emprestimoTiraOMaterialDoSaldoUmaUnicaVezERegistraNoHistorico() {
        Emprestimo emp = service.emprestar(emprestimo(4), usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(6);
        assertThat(emp.getStatus()).isEqualTo(StatusEmprestimo.ATIVO);
        assertThat(emp.getQuantidadePendente()).isEqualTo(4);

        ArgumentCaptor<Movimentacao> captor = ArgumentCaptor.forClass(Movimentacao.class);
        verify(movimentacaoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.EMPRESTIMO);
        assertThat(captor.getValue().getEmprestimoId()).isEqualTo(50L);
        assertThat(captor.getValue().getSaldoApos()).isEqualTo(6);
    }

    @Test
    void emprestimoAcimaDoSaldoEhBloqueado() {
        assertThatThrownBy(() -> service.emprestar(emprestimo(11), usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void materialJaEmprestadoNaoPodeSerRetiradoDeNovo() {
        service.emprestar(emprestimo(10), usuario);
        assertThat(material.getQuantidadeAtual()).isZero();

        MovimentacaoRequest r = movimento(TipoMovimentacao.SAIDA, 1);
        r.setDestinatario("Outra pessoa");
        assertThatThrownBy(() -> service.registrar(r, usuario)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.emprestar(emprestimo(1), usuario)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void dataPrevistaAnteriorAoEmprestimoEhRecusada() {
        EmprestimoRequest r = emprestimo(1);
        r.setDataEmprestimo(LocalDate.now());
        r.setDataPrevistaDevolucao(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> service.emprestar(r, usuario)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void emprestimoRepetidoComAMesmaChaveNaoDescontaDeNovo() {
        Emprestimo existente = emprestimoAtivo(3, 0);
        when(emprestimoRepository.findByChaveIdempotenciaAndEmpresaId("k1", 1L)).thenReturn(Optional.of(existente));

        EmprestimoRequest r = emprestimo(3);
        r.setChaveIdempotencia("k1");
        Emprestimo devolvido = service.emprestar(r, usuario);

        assertThat(devolvido).isSameAs(existente);
        assertThat(material.getQuantidadeAtual()).isEqualTo(10);
    }

    @Test
    void devolucaoParcialDevolveAoSaldoEMantemOEmprestimoAtivo() {
        material.setQuantidadeAtual(6);
        Emprestimo emp = emprestimoAtivo(4, 0);

        DevolucaoRequest r = new DevolucaoRequest();
        r.setQuantidade(1);
        service.devolver(50L, r, usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(7);
        assertThat(emp.getQuantidadeDevolvida()).isEqualTo(1);
        assertThat(emp.getQuantidadePendente()).isEqualTo(3);
        assertThat(emp.getStatus()).isEqualTo(StatusEmprestimo.ATIVO);
        assertThat(emp.getDataDevolucaoEfetiva()).isNull();
    }

    @Test
    void devolucaoTotalFechaOEmprestimoERegistraADataEfetiva() {
        material.setQuantidadeAtual(6);
        Emprestimo emp = emprestimoAtivo(4, 1);

        DevolucaoRequest r = new DevolucaoRequest();
        r.setQuantidade(3);
        service.devolver(50L, r, usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(9);
        assertThat(emp.getStatus()).isEqualTo(StatusEmprestimo.DEVOLVIDO);
        assertThat(emp.getDataDevolucaoEfetiva()).isEqualTo(LocalDate.now());
    }

    @Test
    void devolucaoAcimaDoPendenteEhRecusada() {
        material.setQuantidadeAtual(6);
        Emprestimo emp = emprestimoAtivo(4, 0);

        DevolucaoRequest r = new DevolucaoRequest();
        r.setQuantidade(5);
        assertThatThrownBy(() -> service.devolver(50L, r, usuario)).isInstanceOf(RegraDeNegocioException.class);

        assertThat(material.getQuantidadeAtual()).isEqualTo(6);
        assertThat(emp.getQuantidadeDevolvida()).isZero();
    }

    @Test
    void emprestimoJaDevolvidoNaoAceitaNovaDevolucao() {
        Emprestimo emp = emprestimoAtivo(2, 2);
        emp.setStatus(StatusEmprestimo.DEVOLVIDO);

        DevolucaoRequest r = new DevolucaoRequest();
        r.setQuantidade(1);
        assertThatThrownBy(() -> service.devolver(50L, r, usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("devolvido");
    }

    @Test
    void devolucaoReenviadaComAMesmaChaveNaoSomaDeNovo() {
        material.setQuantidadeAtual(6);
        Emprestimo emp = emprestimoAtivo(4, 1);
        Movimentacao anterior = new Movimentacao();
        when(movimentacaoRepository.findByChaveIdempotenciaAndEmpresaId("dev-1", 1L)).thenReturn(Optional.of(anterior));

        DevolucaoRequest r = new DevolucaoRequest();
        r.setQuantidade(1);
        r.setChaveIdempotencia("dev-1");
        service.devolver(50L, r, usuario);

        assertThat(material.getQuantidadeAtual()).isEqualTo(6);
        assertThat(emp.getQuantidadeDevolvida()).isEqualTo(1);
    }

    @Test
    void emprestimoAtivoComDataVencidaApareceComoAtrasado() {
        Emprestimo emp = emprestimoAtivo(1, 0);
        assertThat(emp.getSituacao()).isEqualTo("ATIVO");
        emp.setDataPrevistaDevolucao(LocalDate.now().minusDays(1));
        assertThat(emp.getSituacao()).isEqualTo("ATRASADO");
        emp.setStatus(StatusEmprestimo.DEVOLVIDO);
        assertThat(emp.getSituacao()).isEqualTo("DEVOLVIDO");
    }
}
