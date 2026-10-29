package com.almoxarifado.integracao;

import com.almoxarifado.model.*;
import com.almoxarifado.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxos completos pela API real (controllers, serviços, JPA e segurança JWT) contra um H2 em memória.
 * Cada teste começa com o banco limpo e duas empresas, para também validar o isolamento entre elas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoAlmoxarifadoIntegracaoTest {

    private static final String SENHA = "senha-de-teste-123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired EmpresaRepository empresaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired FornecedorRepository fornecedorRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired MaterialFornecedorRepository vinculoRepository;
    @Autowired MovimentacaoRepository movimentacaoRepository;
    @Autowired EmprestimoRepository emprestimoRepository;

    private String tokenA;
    private String tokenB;
    private Long fornecedorA1;
    private Long fornecedorA2;

    @BeforeEach
    void limparEPopular() throws Exception {
        movimentacaoRepository.deleteAll();
        emprestimoRepository.deleteAll();
        vinculoRepository.deleteAll();
        materialRepository.deleteAll();
        fornecedorRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        Empresa a = criarEmpresa("Empresa A");
        Empresa b = criarEmpresa("Empresa B");
        criarUsuario("usuario.a", a);
        criarUsuario("usuario.b", b);
        fornecedorA1 = criarFornecedor("Papelaria Central", a);
        fornecedorA2 = criarFornecedor("Ferragens Sul", a);

        tokenA = login("usuario.a");
        tokenB = login("usuario.b");
    }

    // ------------------------------------------------------------------ cadastro e códigos

    @Test
    void cadastroGeraCodigoAutomaticoESequencial() throws Exception {
        JsonNode m1 = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null);
        JsonNode m2 = criarMaterial(tokenA, "Furadeira", "Bosch", "GSB", "", 2, null);

        assertThat(m1.get("codigo").asText()).isEqualTo("MAT-001");
        assertThat(m2.get("codigo").asText()).isEqualTo("MAT-002");
    }

    @Test
    void mesmoProdutoComMarcasEFornecedoresDiferentesMantemOMesmoCodigo() throws Exception {
        JsonNode a = criarMaterial(tokenA, "Caneta azul", "Marca A", "X1", "", 8, fornecedorA1);
        JsonNode b = criarMaterial(tokenA, "Caneta azul", "Marca B", "Z9", "", 30, fornecedorA2);

        assertThat(b.get("codigo").asText()).isEqualTo(a.get("codigo").asText());
        assertThat(b.get("id").asLong()).isNotEqualTo(a.get("id").asLong());
        assertThat(a.get("fornecedores").get(0).get("fornecedorNome").asText()).isEqualTo("Papelaria Central");
        assertThat(b.get("fornecedores").get(0).get("fornecedorNome").asText()).isEqualTo("Ferragens Sul");
        assertThat(materialRepository.findAllByCodigoAndEmpresaId(a.get("codigo").asText(), empresaId("usuario.a")))
                .hasSize(2);
    }

    @Test
    void variacaoIdenticaNaoPodeSerCadastradaDuasVezes() throws Exception {
        criarMaterial(tokenA, "Caneta azul", "Marca A", "X1", "", 8, null);

        mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoMaterial("Caneta azul", "Marca A", "X1", "", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codigoBaseCriaNovaVariacaoDoProdutoExistente() throws Exception {
        JsonNode base = criarMaterial(tokenA, "Luva de segurança", "Marca A", "", "12345", 5, null);

        Map<String, Object> corpo = corpoMaterialMapa("Luva nitrílica", "Marca B", "", "67890", 3, null);
        corpo.put("codigoBase", base.get("codigo").asText());
        JsonNode nova = ler(mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isOk()).andReturn());

        assertThat(nova.get("codigo").asText()).isEqualTo(base.get("codigo").asText());
        assertThat(nova.get("ca").asText()).isEqualTo("67890");
    }

    @Test
    void saldoInicialGeraEntradaNoHistorico() throws Exception {
        JsonNode m = criarMaterial(tokenA, "Papel A4", "", "", "", 12, null);

        List<Movimentacao> historico = movimentacaoRepository
                .findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(m.get("id").asLong(), empresaId("usuario.a"));
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(historico.get(0).getQuantidade()).isEqualTo(12);
    }

    // ------------------------------------------------------------------ entradas, saídas, baixas

    @Test
    void entradaAumentaEstoqueESaidaReduz() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null).get("id").asLong();

        movimentar(tokenA, id, "ENTRADA", 5, null, null).andExpect(status().isOk());
        assertThat(saldo(tokenA, id)).isEqualTo(15);

        movimentar(tokenA, id, "SAIDA", 4, "Maria Souza", null).andExpect(status().isOk());
        assertThat(saldo(tokenA, id)).isEqualTo(11);
    }

    @Test
    void saidaAcimaDoSaldoRetornaMensagemAmigavelENaoAlteraOEstoque() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 3, null).get("id").asLong();

        MvcResult r = movimentar(tokenA, id, "SAIDA", 4, "Maria", null).andExpect(status().isBadRequest()).andReturn();

        assertThat(ler(r).get("erro").asText()).contains("Estoque insuficiente");
        assertThat(saldo(tokenA, id)).isEqualTo(3);
    }

    @Test
    void perdaEAvariaExigemJustificativaEReduzemSaldo() throws Exception {
        long id = criarMaterial(tokenA, "Luva", "", "", "", 10, null).get("id").asLong();

        movimentar(tokenA, id, "PERDA", 1, null, null).andExpect(status().isBadRequest());
        assertThat(saldo(tokenA, id)).isEqualTo(10);

        movimentar(tokenA, id, "PERDA", 2, null, "Extraviada no depósito").andExpect(status().isOk());
        movimentar(tokenA, id, "AVARIA", 3, null, "Rasgada").andExpect(status().isOk());
        assertThat(saldo(tokenA, id)).isEqualTo(5);

        movimentar(tokenA, id, "AVARIA", 6, null, "Mais do que existe").andExpect(status().isBadRequest());
        assertThat(saldo(tokenA, id)).isEqualTo(5);
    }

    @Test
    void entradaComFornecedorEPrecoRegistraOPrecoDoFornecedor() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 0, null).get("id").asLong();

        Map<String, Object> corpo = movimentoMapa(id, "ENTRADA", 10, null, null);
        corpo.put("fornecedorId", fornecedorA1);
        corpo.put("precoUnitario", 23.5);
        mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isOk());

        JsonNode material = consultar(tokenA, id);
        assertThat(material.get("fornecedores")).hasSize(1);
        assertThat(material.get("fornecedores").get(0).get("precoUnitario").decimalValue()).isEqualByComparingTo("23.5");
    }

    @Test
    void reenvioDaMesmaOperacaoNaoDuplicaOLancamento() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null).get("id").asLong();
        Map<String, Object> corpo = movimentoMapa(id, "SAIDA", 3, "Maria", null);
        corpo.put("chaveIdempotencia", UUID.randomUUID().toString());
        String conteudo = json.writeValueAsString(corpo);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + tokenA)
                    .contentType(MediaType.APPLICATION_JSON).content(conteudo)).andExpect(status().isOk());
        }

        assertThat(saldo(tokenA, id)).isEqualTo(7);
        assertThat(movimentacaoRepository.findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(id, empresaId("usuario.a")))
                .filteredOn(m -> m.getTipo() == TipoMovimentacao.SAIDA).hasSize(1);
    }

    @Test
    void duasRetiradasSimultaneasNuncaGastamOMesmoSaldo() throws Exception {
        long id = criarMaterial(tokenA, "Furadeira", "", "", "", 1, null).get("id").asLong();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final String conteudo = json.writeValueAsString(movimentoMapa(id, "SAIDA", 1, "Pessoa " + i, null));
            resultados.add(pool.submit(() -> {
                largada.await();
                return mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(conteudo)).andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();
        int sucessos = 0;
        for (Future<Integer> f : resultados) {
            if (f.get(30, TimeUnit.SECONDS) == 200) sucessos++;
        }
        pool.shutdown();

        assertThat(sucessos).isEqualTo(1);
        assertThat(saldo(tokenA, id)).isZero();
    }

    // ------------------------------------------------------------------ empréstimos

    @Test
    void emprestimoExigeDataPrevistaDeDevolucao() throws Exception {
        long id = criarMaterial(tokenA, "Furadeira", "", "", "", 2, null).get("id").asLong();

        Map<String, Object> corpo = emprestimoMapa(id, 1);
        corpo.remove("dataPrevistaDevolucao");
        mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest());
        assertThat(saldo(tokenA, id)).isEqualTo(2);
    }

    @Test
    void materialEmprestidoNaoFicaDisponivelEDevolucaoRestauraSaldoESituacao() throws Exception {
        long id = criarMaterial(tokenA, "Furadeira", "Bosch", "GSB", "", 2, null).get("id").asLong();

        JsonNode emp = ler(mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(emprestimoMapa(id, 2))))
                .andExpect(status().isOk()).andReturn());
        long empId = emp.get("id").asLong();

        JsonNode emprestado = consultar(tokenA, id);
        assertThat(emprestado.get("quantidadeDisponivel").asInt()).isZero();
        assertThat(emprestado.get("quantidadeEmprestada").asInt()).isEqualTo(2);
        assertThat(emprestado.get("quantidadeTotal").asInt()).isEqualTo(2);
        assertThat(emprestado.get("situacao").asText()).isEqualTo("EMPRESTADO");

        // não pode ser retirado nem emprestado de novo enquanto estiver fora
        movimentar(tokenA, id, "SAIDA", 1, "Outra pessoa", null).andExpect(status().isBadRequest());
        mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(emprestimoMapa(id, 1))))
                .andExpect(status().isBadRequest());

        // devolução parcial
        devolver(tokenA, empId, 1).andExpect(status().isOk());
        JsonNode parcial = consultar(tokenA, id);
        assertThat(parcial.get("quantidadeDisponivel").asInt()).isEqualTo(1);
        assertThat(parcial.get("quantidadeEmprestada").asInt()).isEqualTo(1);
        assertThat(ler(mvc.perform(get("/api/emprestimos/" + empId).header("Authorization", "Bearer " + tokenA))
                .andReturn()).get("situacao").asText()).isEqualTo("ATIVO");

        // devolver mais do que está emprestado é recusado
        devolver(tokenA, empId, 2).andExpect(status().isBadRequest());

        // devolução do restante
        devolver(tokenA, empId, 1).andExpect(status().isOk());
        JsonNode fim = consultar(tokenA, id);
        assertThat(fim.get("quantidadeDisponivel").asInt()).isEqualTo(2);
        assertThat(fim.get("quantidadeEmprestada").asInt()).isZero();
        assertThat(fim.get("situacao").asText()).isEqualTo("DISPONIVEL");

        JsonNode final_ = ler(mvc.perform(get("/api/emprestimos/" + empId).header("Authorization", "Bearer " + tokenA))
                .andReturn());
        assertThat(final_.get("situacao").asText()).isEqualTo("DEVOLVIDO");
        assertThat(final_.get("dataDevolucaoEfetiva").isNull()).isFalse();

        devolver(tokenA, empId, 1).andExpect(status().isBadRequest());
    }

    @Test
    void emprestimoEmAbertoApareceNaListagemComFiltroDeSituacao() throws Exception {
        long id = criarMaterial(tokenA, "Furadeira", "", "", "", 2, null).get("id").asLong();
        mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(emprestimoMapa(id, 1))))
                .andExpect(status().isOk());

        JsonNode abertos = ler(mvc.perform(get("/api/emprestimos?situacao=ABERTOS").header("Authorization", "Bearer " + tokenA))
                .andReturn());
        JsonNode devolvidos = ler(mvc.perform(get("/api/emprestimos?situacao=DEVOLVIDO").header("Authorization", "Bearer " + tokenA))
                .andReturn());
        assertThat(abertos).hasSize(1);
        assertThat(devolvidos).isEmpty();
    }

    @Test
    void emprestimoRepetidoComAMesmaChaveCriaApenasUm() throws Exception {
        long id = criarMaterial(tokenA, "Furadeira", "", "", "", 5, null).get("id").asLong();
        Map<String, Object> corpo = emprestimoMapa(id, 2);
        corpo.put("chaveIdempotencia", UUID.randomUUID().toString());
        String conteudo = json.writeValueAsString(corpo);

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenA)
                    .contentType(MediaType.APPLICATION_JSON).content(conteudo)).andExpect(status().isOk());
        }

        assertThat(saldo(tokenA, id)).isEqualTo(3);
        assertThat(emprestimoRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ filtros

    @Test
    void filtrosDeMateriaisSaoCombinaveis() throws Exception {
        criarMaterial(tokenA, "Caneta azul", "Marca A", "X1", "", 8, fornecedorA1);
        criarMaterial(tokenA, "Caneta azul", "Marca B", "Z9", "", 0, fornecedorA2);
        criarMaterial(tokenA, "Luva", "Marca A", "", "12345", 4, fornecedorA2);

        assertThat(listar(tokenA, "")).hasSize(3);
        assertThat(listar(tokenA, "?marca=Marca A")).hasSize(2);
        assertThat(listar(tokenA, "?modelo=Z9")).hasSize(1);
        assertThat(listar(tokenA, "?ca=12345")).hasSize(1);
        assertThat(listar(tokenA, "?fornecedorId=" + fornecedorA2)).hasSize(2);
        assertThat(listar(tokenA, "?marca=Marca A&fornecedorId=" + fornecedorA2)).hasSize(1);
        assertThat(listar(tokenA, "?situacao=SEM_ESTOQUE")).hasSize(1);
        assertThat(listar(tokenA, "?busca=ferragens")).hasSize(2);
        assertThat(listar(tokenA, "?nome=caneta&marca=Marca B")).hasSize(1);
    }

    @Test
    void historicoDeMovimentacoesPodeSerFiltradoPorTipo() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null).get("id").asLong();
        movimentar(tokenA, id, "SAIDA", 1, "Maria", null).andExpect(status().isOk());

        JsonNode saidas = ler(mvc.perform(get("/api/movimentacoes?tipo=SAIDA").header("Authorization", "Bearer " + tokenA))
                .andReturn());
        JsonNode todas = ler(mvc.perform(get("/api/movimentacoes").header("Authorization", "Bearer " + tokenA)).andReturn());
        assertThat(saidas).hasSize(1);
        assertThat(todas).hasSize(2); // saldo inicial + saída
    }

    // ------------------------------------------------------------------ exclusões seguras

    @Test
    void materialComHistoricoNaoEhExcluidoMasPodeSerInativado() throws Exception {
        JsonNode m = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null);
        long id = m.get("id").asLong();

        MvcResult r = mvc.perform(delete("/api/materiais/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("histórico");
        assertThat(materialRepository.existsById(id)).isTrue();

        Map<String, Object> corpo = corpoMaterialMapa("Papel A4", "", "", "", 0, null);
        corpo.put("ativo", false);
        mvc.perform(put("/api/materiais/" + id).header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isOk());

        JsonNode inativo = consultar(tokenA, id);
        assertThat(inativo.get("situacao").asText()).isEqualTo("INDISPONIVEL");
        assertThat(inativo.get("quantidadeAtual").asInt()).isEqualTo(10); // saldo não é alterado pela edição
        movimentar(tokenA, id, "ENTRADA", 1, null, null).andExpect(status().isBadRequest());
        assertThat(movimentacaoRepository.count()).isEqualTo(1); // histórico preservado
    }

    @Test
    void materialSemHistoricoPodeSerExcluidoComSeusVinculos() throws Exception {
        long id = criarMaterial(tokenA, "Papel A4", "", "", "", 0, fornecedorA1).get("id").asLong();

        mvc.perform(delete("/api/materiais/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        assertThat(materialRepository.existsById(id)).isFalse();
        assertThat(vinculoRepository.count()).isZero();
    }

    @Test
    void fornecedorVinculadoNaoEhExcluidoEMostraMensagemAmigavel() throws Exception {
        criarMaterial(tokenA, "Papel A4", "", "", "", 0, fornecedorA1);

        MvcResult r = mvc.perform(delete("/api/fornecedores/" + fornecedorA1).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("vinculado");
        assertThat(fornecedorRepository.existsById(fornecedorA1)).isTrue();

        mvc.perform(delete("/api/fornecedores/" + fornecedorA2).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ segurança e isolamento

    @Test
    void semTokenAApiRecusaOAcesso() throws Exception {
        mvc.perform(get("/api/materiais")).andExpect(status().isUnauthorized());
    }

    @Test
    void empresaNaoEnxergaNemAlteraDadosDeOutraEmpresa() throws Exception {
        long idA = criarMaterial(tokenA, "Papel A4", "", "", "", 10, null).get("id").asLong();

        assertThat(listar(tokenB, "")).isEmpty();
        mvc.perform(get("/api/materiais/" + idA).header("Authorization", "Bearer " + tokenB)).andExpect(status().isNotFound());
        movimentar(tokenB, idA, "SAIDA", 1, "Invasor", null).andExpect(status().isNotFound());
        mvc.perform(delete("/api/materiais/" + idA).header("Authorization", "Bearer " + tokenB)).andExpect(status().isNotFound());
        mvc.perform(get("/api/fornecedores/" + fornecedorA1).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        assertThat(saldo(tokenA, idA)).isEqualTo(10);
    }

    // ------------------------------------------------------------------ auxiliares

    private Empresa criarEmpresa(String nome) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setAtiva(true);
        return empresaRepository.save(e);
    }

    private void criarUsuario(String login, Empresa empresa) {
        Usuario u = new Usuario();
        u.setNome("Nome de " + login);
        u.setUsuario(login);
        u.setSenha(encoder.encode(SENHA));
        u.setPapel(Papel.ADMIN);
        u.setAtivo(true);
        u.setEmpresa(empresa);
        usuarioRepository.save(u);
    }

    private Long criarFornecedor(String nome, Empresa empresa) {
        Fornecedor f = new Fornecedor();
        f.setNome(nome);
        f.setEmpresa(empresa);
        return fornecedorRepository.save(f).getId();
    }

    private Long empresaId(String login) {
        return usuarioRepository.findByUsuario(login).orElseThrow().getEmpresa().getId();
    }

    private String login(String usuario) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", usuario, "senha", SENHA))))
                .andExpect(status().isOk()).andReturn();
        return ler(r).get("token").asText();
    }

    private JsonNode ler(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, Object> corpoMaterialMapa(String nome, String marca, String modelo, String ca, int saldo, Long fornecedorId) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nome", nome);
        m.put("marca", marca);
        m.put("modelo", modelo);
        m.put("ca", ca);
        m.put("unidade", "UNIDADE");
        m.put("quantidadeAtual", saldo);
        m.put("quantidadeMinima", 1);
        if (fornecedorId != null) {
            m.put("fornecedores", List.of(Map.of("fornecedorId", fornecedorId, "precoUnitario", 10.0)));
        }
        return m;
    }

    private String corpoMaterial(String nome, String marca, String modelo, String ca, int saldo, Long fornecedorId) throws Exception {
        return json.writeValueAsString(corpoMaterialMapa(nome, marca, modelo, ca, saldo, fornecedorId));
    }

    private JsonNode criarMaterial(String token, String nome, String marca, String modelo, String ca, int saldo, Long fornecedorId)
            throws Exception {
        return ler(mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoMaterial(nome, marca, modelo, ca, saldo, fornecedorId)))
                .andExpect(status().isOk()).andReturn());
    }

    private Map<String, Object> movimentoMapa(long materialId, String tipo, int quantidade, String destinatario, String observacao) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("tipo", tipo);
        m.put("quantidade", quantidade);
        m.put("responsavel", "Responsável Teste");
        if (destinatario != null) m.put("destinatario", destinatario);
        if (observacao != null) m.put("observacao", observacao);
        return m;
    }

    private org.springframework.test.web.servlet.ResultActions movimentar(String token, long materialId, String tipo, int quantidade,
                                                                           String destinatario, String observacao) throws Exception {
        return mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(movimentoMapa(materialId, tipo, quantidade, destinatario, observacao))));
    }

    private Map<String, Object> emprestimoMapa(long materialId, int quantidade) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("quantidade", quantidade);
        m.put("tomador", "Carlos Lima");
        m.put("setor", "Manutenção");
        m.put("responsavelEntrega", "Maria");
        m.put("dataPrevistaDevolucao", LocalDate.now().plusDays(7).toString());
        return m;
    }

    private org.springframework.test.web.servlet.ResultActions devolver(String token, long emprestimoId, int quantidade) throws Exception {
        return mvc.perform(post("/api/emprestimos/" + emprestimoId + "/devolucoes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("quantidade", quantidade, "responsavel", "Almoxarife"))));
    }

    private JsonNode consultar(String token, long id) throws Exception {
        return ler(mvc.perform(get("/api/materiais/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
    }

    private int saldo(String token, long id) throws Exception {
        return consultar(token, id).get("quantidadeAtual").asInt();
    }

    private JsonNode listar(String token, String query) throws Exception {
        return ler(mvc.perform(get("/api/materiais" + query).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
    }
}
