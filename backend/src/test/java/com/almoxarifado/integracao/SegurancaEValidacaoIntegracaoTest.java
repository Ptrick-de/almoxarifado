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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, permissões, mensagens de erro amigáveis e empréstimos atrasados, pela API real contra H2 em memória.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaEValidacaoIntegracaoTest {

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

    private String tokenAdminA;
    private String tokenOperadorA;
    private String tokenAdminB;

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
        criarUsuario("admin.a", a, Papel.ADMIN, true);
        criarUsuario("operador.a", a, Papel.OPERADOR, true);
        criarUsuario("admin.b", b, Papel.ADMIN, true);
        criarUsuario("desativado.a", a, Papel.OPERADOR, false);

        tokenAdminA = login("admin.a");
        tokenOperadorA = login("operador.a");
        tokenAdminB = login("admin.b");
    }

    // ------------------------------------------------------------------ login e permissões

    @Test
    void loginComSenhaErradaMostraMensagemAmigavel() throws Exception {
        MvcResult r = tentarLogin("admin.a", "senha-errada").andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).isEqualTo("Usuário ou senha inválidos");
    }

    @Test
    void usuarioDesativadoNaoConsegueEntrar() throws Exception {
        MvcResult r = tentarLogin("desativado.a", SENHA).andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("desativado");
    }

    @Test
    void loginRetornaOsDadosDaEmpresaDoUsuario() throws Exception {
        MvcResult r = tentarLogin("admin.b", SENHA).andExpect(status().isOk()).andReturn();
        JsonNode corpo = ler(r);
        assertThat(corpo.get("empresaNome").asText()).isEqualTo("Empresa B");
        assertThat(corpo.get("papel").asText()).isEqualTo("ADMIN");
    }

    @Test
    void tokenInvalidoEhRecusadoComMensagemAmigavel() throws Exception {
        MvcResult r = mvc.perform(get("/api/materiais").header("Authorization", "Bearer isto.nao.e.um.token"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("login");
    }

    @Test
    void operadorNaoAcessaGestaoDeUsuariosMasAdminAcessa() throws Exception {
        MvcResult r = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenOperadorA))
                .andExpect(status().isForbidden()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("permissão");

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenAdminA)).andExpect(status().isOk());
    }

    @Test
    void operadorPodeRegistrarMovimentacoes() throws Exception {
        long id = criarMaterial(tokenOperadorA, "Papel A4", 10);
        movimentar(tokenOperadorA, id, "SAIDA", 2, "Maria").andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ validações e mensagens

    @Test
    void materialSemNomeRetornaMensagemEmPortuguesPorCampo() throws Exception {
        Map<String, Object> corpo = corpoMaterial("", 5);
        MvcResult r = mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest()).andReturn();

        JsonNode resposta = ler(r);
        assertThat(resposta.get("campos").get("nome").asText()).isEqualTo("Nome é obrigatório");
        assertThat(resposta.get("erro").asText()).contains("Nome é obrigatório");
    }

    @Test
    void quantidadeZeroOuNegativaEhRecusadaNaMovimentacao() throws Exception {
        long id = criarMaterial(tokenAdminA, "Papel A4", 10);

        MvcResult r = movimentar(tokenAdminA, id, "ENTRADA", 0, null).andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("campos").get("quantidade").asText()).isEqualTo("Quantidade deve ser maior que zero");
        movimentar(tokenAdminA, id, "ENTRADA", -3, null).andExpect(status().isBadRequest());
        assertThat(saldo(tokenAdminA, id)).isEqualTo(10);
    }

    @Test
    void jsonMalformadoNaoExpoeDetalhesTecnicos() throws Exception {
        MvcResult r = mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content("{ isto nao eh json"))
                .andExpect(status().isBadRequest()).andReturn();
        String mensagem = ler(r).get("erro").asText();
        assertThat(mensagem).contains("dados enviados são inválidos");
        assertThat(mensagem).doesNotContain("Exception").doesNotContain("JSON parse");
    }

    @Test
    void tipoDeMovimentacaoInexistenteRetornaErroAmigavel() throws Exception {
        long id = criarMaterial(tokenAdminA, "Papel A4", 10);
        Map<String, Object> corpo = movimentoMapa(id, "TIPO_QUE_NAO_EXISTE", 1, null);
        MvcResult r = mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).doesNotContain("Exception");
    }

    // ------------------------------------------------------------------ empréstimos atrasados e isolamento

    @Test
    void emprestimoComPrazoVencidoApareceComoAtrasadoNaListaENoPainel() throws Exception {
        long id = criarMaterial(tokenAdminA, "Furadeira", 3);

        Map<String, Object> vencido = emprestimoMapa(id, 1);
        vencido.put("dataEmprestimo", LocalDate.now().minusDays(10).toString());
        vencido.put("dataPrevistaDevolucao", LocalDate.now().minusDays(3).toString());
        JsonNode criado = ler(mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(vencido)))
                .andExpect(status().isOk()).andReturn());
        assertThat(criado.get("situacao").asText()).isEqualTo("ATRASADO");

        Map<String, Object> noPrazo = emprestimoMapa(id, 1);
        mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenAdminA)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(noPrazo))).andExpect(status().isOk());

        assertThat(listarEmprestimos(tokenAdminA, "?situacao=ATRASADO")).hasSize(1);
        assertThat(listarEmprestimos(tokenAdminA, "?situacao=ATIVO")).hasSize(1);
        assertThat(listarEmprestimos(tokenAdminA, "?situacao=ABERTOS")).hasSize(2);
        assertThat(listarEmprestimos(tokenAdminA, "?situacao=DEVOLVIDO")).isEmpty();

        JsonNode painel = ler(mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk()).andReturn());
        assertThat(painel.get("emprestimosAbertos").asInt()).isEqualTo(2);
        assertThat(painel.get("emprestimosAtrasados").asInt()).isEqualTo(1);
    }

    @Test
    void emprestimoNaoPodeTerDataPrevistaAnteriorAoEmprestimo() throws Exception {
        long id = criarMaterial(tokenAdminA, "Furadeira", 3);
        Map<String, Object> corpo = emprestimoMapa(id, 1);
        corpo.put("dataPrevistaDevolucao", LocalDate.now().minusDays(1).toString());

        MvcResult r = mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(ler(r).get("erro").asText()).contains("anterior");
        assertThat(saldo(tokenAdminA, id)).isEqualTo(3);
    }

    @Test
    void outraEmpresaNaoConsegueConsultarNemDevolverEmprestimoAlheio() throws Exception {
        long id = criarMaterial(tokenAdminA, "Furadeira", 3);
        JsonNode emp = ler(mvc.perform(post("/api/emprestimos").header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(emprestimoMapa(id, 1))))
                .andExpect(status().isOk()).andReturn());
        long empId = emp.get("id").asLong();

        mvc.perform(get("/api/emprestimos/" + empId).header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/emprestimos/" + empId + "/devolucoes").header("Authorization", "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("quantidade", 1))))
                .andExpect(status().isNotFound());
        assertThat(listarEmprestimos(tokenAdminB, "")).isEmpty();
        assertThat(saldo(tokenAdminA, id)).isEqualTo(2);
    }

    @Test
    void codigosSaoSequenciaisPorEmpresaEIndependentes() throws Exception {
        criarMaterial(tokenAdminA, "Papel A4", 1);
        criarMaterial(tokenAdminA, "Furadeira", 1);

        MvcResult r = mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpoMaterial("Mouse", 1))))
                .andExpect(status().isOk()).andReturn();
        assertThat(ler(r).get("codigo").asText()).isEqualTo("MAT-001");
    }

    // ------------------------------------------------------------------ auxiliares

    private Empresa criarEmpresa(String nome) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setAtiva(true);
        return empresaRepository.save(e);
    }

    private void criarUsuario(String login, Empresa empresa, Papel papel, boolean ativo) {
        Usuario u = new Usuario();
        u.setNome("Nome de " + login);
        u.setUsuario(login);
        u.setSenha(encoder.encode(SENHA));
        u.setPapel(papel);
        u.setAtivo(ativo);
        u.setEmpresa(empresa);
        usuarioRepository.save(u);
    }

    private ResultActions tentarLogin(String usuario, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("usuario", usuario, "senha", senha))));
    }

    private String login(String usuario) throws Exception {
        return ler(tentarLogin(usuario, SENHA).andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    private JsonNode ler(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, Object> corpoMaterial(String nome, int saldo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", nome);
        m.put("unidade", "UNIDADE");
        m.put("quantidadeAtual", saldo);
        m.put("quantidadeMinima", 1);
        return m;
    }

    private long criarMaterial(String token, String nome, int saldo) throws Exception {
        return ler(mvc.perform(post("/api/materiais").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpoMaterial(nome, saldo))))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
    }

    private Map<String, Object> movimentoMapa(long materialId, String tipo, int quantidade, String destinatario) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("tipo", tipo);
        m.put("quantidade", quantidade);
        m.put("responsavel", "Responsável Teste");
        if (destinatario != null) m.put("destinatario", destinatario);
        return m;
    }

    private ResultActions movimentar(String token, long materialId, String tipo, int quantidade, String destinatario) throws Exception {
        return mvc.perform(post("/api/movimentacoes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(movimentoMapa(materialId, tipo, quantidade, destinatario))));
    }

    private Map<String, Object> emprestimoMapa(long materialId, int quantidade) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("quantidade", quantidade);
        m.put("tomador", "Carlos Lima");
        m.put("setor", "Manutenção");
        m.put("responsavelEntrega", "Maria");
        m.put("dataPrevistaDevolucao", LocalDate.now().plusDays(7).toString());
        return m;
    }

    private int saldo(String token, long id) throws Exception {
        return ler(mvc.perform(get("/api/materiais/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn()).get("quantidadeAtual").asInt();
    }

    private JsonNode listarEmprestimos(String token, String query) throws Exception {
        return ler(mvc.perform(get("/api/emprestimos" + query).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
    }
}
