package com.almoxarifado.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ajustes de banco que o Hibernate (ddl-auto=update) não faz sozinho ao evoluir um banco já existente.
 * Todas as etapas são idempotentes: rodar de novo não altera nada que já foi migrado.
 * Nenhuma restrição de integridade é removida além da antiga UNIQUE(codigo, empresa_id), que impedia
 * cadastrar o mesmo produto com marcas diferentes e foi substituída por UNIQUE(codigo, marca, modelo, ca, empresa).
 */
/** Só roda no MySQL real; os testes automatizados (perfil "test") usam um banco novo em memória. */
@Profile("!test")
@Component
public class MigracaoBanco implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoBanco.class);

    private final JdbcTemplate jdbc;

    public MigracaoBanco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            removerUnicidadeAntigaDoCodigo();
            converterTipoMovimentacaoParaTexto();
            ampliarObservacaoDasMovimentacoes();
            migrarFornecedorUnicoParaVinculos();
            jdbc.update("UPDATE materiais SET marca = '' WHERE marca IS NULL");
            jdbc.update("UPDATE materiais SET modelo = '' WHERE modelo IS NULL");
            jdbc.update("UPDATE materiais SET ca = '' WHERE ca IS NULL");
        } catch (Exception e) {
            log.error("Falha ao migrar o banco de dados. Verifique o log e a conexão com o MySQL.", e);
        }
    }

    /** Remove UNIQUE(codigo, empresa_id): o código agora é do produto e se repete entre suas variações. */
    private void removerUnicidadeAntigaDoCodigo() {
        List<String> indices = jdbc.queryForList(
                "SELECT index_name FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'materiais' AND non_unique = 0 "
                + "AND index_name <> 'PRIMARY' GROUP BY index_name "
                + "HAVING COUNT(*) = 2 AND SUM(column_name IN ('codigo', 'empresa_id')) = 2",
                String.class);
        for (String indice : indices) {
            jdbc.execute("ALTER TABLE materiais DROP INDEX `" + indice.replace("`", "") + "`");
            log.info("Índice único antigo de código removido: {}", indice);
        }
    }

    /** O MySQL pode ter criado 'tipo' como ENUM; texto permite os novos tipos (EMPRESTIMO, DEVOLUCAO). */
    private void converterTipoMovimentacaoParaTexto() {
        List<String> tipos = jdbc.queryForList(
                "SELECT column_type FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'movimentacoes' AND column_name = 'tipo'",
                String.class);
        if (!tipos.isEmpty() && tipos.get(0).toLowerCase().startsWith("enum")) {
            jdbc.execute("ALTER TABLE movimentacoes MODIFY COLUMN tipo VARCHAR(20)");
            log.info("Coluna movimentacoes.tipo convertida de ENUM para VARCHAR(20)");
        }
    }

    /** Observações passaram de 255 para 1000 caracteres (justificativas de perda/avaria são mais longas). */
    private void ampliarObservacaoDasMovimentacoes() {
        List<Long> tamanhos = jdbc.queryForList(
                "SELECT character_maximum_length FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'movimentacoes' AND column_name = 'observacao'",
                Long.class);
        if (!tamanhos.isEmpty() && tamanhos.get(0) != null && tamanhos.get(0) < 1000) {
            jdbc.execute("ALTER TABLE movimentacoes MODIFY COLUMN observacao VARCHAR(1000)");
            log.info("Coluna movimentacoes.observacao ampliada para 1000 caracteres");
        }
    }

    /** Copia o antigo fornecedor único de cada material para a tabela de vínculos (com o preço) e libera a coluna. */
    private void migrarFornecedorUnicoParaVinculos() {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'materiais' AND column_name = 'fornecedor_id'",
                Integer.class);
        if (existe == null || existe == 0) return;
        int copiados = jdbc.update(
                "INSERT INTO material_fornecedores (material_id, fornecedor_id, preco_unitario, data_atualizacao, empresa_id) "
                + "SELECT m.id, m.fornecedor_id, COALESCE(m.preco_unitario, 0), NOW(), m.empresa_id FROM materiais m "
                + "WHERE m.fornecedor_id IS NOT NULL AND NOT EXISTS ("
                + "SELECT 1 FROM material_fornecedores mf WHERE mf.material_id = m.id AND mf.fornecedor_id = m.fornecedor_id)");
        jdbc.update("UPDATE materiais SET fornecedor_id = NULL WHERE fornecedor_id IS NOT NULL");
        if (copiados > 0) log.info("{} vínculo(s) material-fornecedor migrado(s)", copiados);
    }
}
