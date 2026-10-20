package com.almoxarifado.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de erros da API.
 *
 * Todas as respostas de erro seguem o mesmo formato JSON:
 * { "timestamp", "status", "erro": "mensagem em português para o usuário", "campos": {campo: mensagem} (opcional) }
 *
 * Detalhes técnicos (SQL, Hibernate, stack traces) vão apenas para o log do servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MSG_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente e, se o problema persistir, entre em contato com o suporte.";

    /** Como cada cadastro é descrito nas mensagens (de acordo com os relacionamentos reais do sistema). */
    private record Recurso(String artigoNome, String dependentes, String pronome,
                           String duplicidade) { }

    private static final Map<String, Recurso> RECURSOS = Map.of(
            "fornecedores", new Recurso("este fornecedor", "materiais", "ele",
                    "Já existe um fornecedor com estes dados."),
            "categorias", new Recurso("esta categoria", "materiais", "ela",
                    "Já existe uma categoria com este nome."),
            "materiais", new Recurso("este material", "movimentações de estoque", "ele",
                    "Já existe um material com este código."),
            "usuarios", new Recurso("este usuário", "registros", "ele",
                    "Já existe um usuário com este nome de usuário.")
    );

    // ------------------------------------------------------------------
    // Erros de negócio e de requisição
    // ------------------------------------------------------------------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return montarResposta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                campos.putIfAbsent(erro.getField(), mensagemOuPadrao(erro.getDefaultMessage(), erro.getField())));
        return montarResposta(HttpStatus.BAD_REQUEST, resumir(campos), campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            campos.putIfAbsent(v.getPropertyPath().toString(), mensagemOuPadrao(v.getMessage(), v.getPropertyPath().toString()));
        }
        return montarResposta(HttpStatus.BAD_REQUEST, resumir(campos), campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição inválido: {}", ex.getMessage());
        return montarResposta(HttpStatus.BAD_REQUEST,
                "Os dados enviados são inválidos. Verifique se todos os campos foram preenchidos corretamente.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        log.warn("Parâmetro inválido '{}': {}", ex.getName(), ex.getMessage());
        return montarResposta(HttpStatus.BAD_REQUEST, "O valor informado é inválido. Verifique e tente novamente.", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroAusente(MissingServletRequestParameterException ex) {
        return montarResposta(HttpStatus.BAD_REQUEST, "Informe todos os dados necessários para continuar.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return montarResposta(HttpStatus.METHOD_NOT_ALLOWED, "Esta operação não está disponível.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return montarResposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de dados não suportado.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRotaInexistente(NoResourceFoundException ex) {
        return montarResposta(HttpStatus.NOT_FOUND, "O recurso solicitado não foi encontrado.", null);
    }

    // ------------------------------------------------------------------
    // Segurança (antes caíam no handler genérico e viravam erro 500)
    // ------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcessoNegado(AccessDeniedException ex) {
        return montarResposta(HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAutenticacao(AuthenticationException ex) {
        return montarResposta(HttpStatus.UNAUTHORIZED, "Não autenticado. Faça login novamente.", null);
    }

    // ------------------------------------------------------------------
    // Banco de dados
    // ------------------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        // O detalhe técnico fica só no log.
        log.warn("Violação de integridade em {} {}: {}", request.getMethod(), request.getRequestURI(), causaRaiz(ex));

        String causa = causaRaiz(ex).toLowerCase(Locale.ROOT);
        Recurso recurso = recursoDaRequisicao(request);
        boolean exclusao = HttpMethod.DELETE.matches(request.getMethod());

        if (causa.contains("duplicate entry") || causa.contains("unique")) {
            String msg = recurso != null ? recurso.duplicidade() : "Já existe um registro com estes dados.";
            return montarResposta(HttpStatus.CONFLICT, msg, null);
        }

        if (causa.contains("foreign key") || causa.contains("referential integrity")) {
            if (exclusao || causa.contains("cannot delete or update a parent row")) {
                return montarResposta(HttpStatus.CONFLICT, mensagemExclusaoBloqueada(recurso), null);
            }
            return montarResposta(HttpStatus.CONFLICT,
                    "Um dos itens selecionados (como categoria ou fornecedor) não existe mais. Atualize a página e tente novamente.", null);
        }

        if (causa.contains("data too long") || causa.contains("value too long")) {
            return montarResposta(HttpStatus.BAD_REQUEST,
                    "Algum campo excede o tamanho máximo permitido. Reduza o texto e tente novamente.", null);
        }

        if (causa.contains("cannot be null") || causa.contains("doesn't have a default value") || causa.contains("null not allowed")) {
            return montarResposta(HttpStatus.BAD_REQUEST, "Preencha todos os campos obrigatórios.", null);
        }

        if (causa.contains("out of range")) {
            return montarResposta(HttpStatus.BAD_REQUEST, "Algum valor numérico informado está fora do limite permitido.", null);
        }

        String msg = exclusao
                ? mensagemExclusaoBloqueada(recurso)
                : "Não foi possível salvar: os dados informados entram em conflito com registros existentes.";
        return montarResposta(HttpStatus.CONFLICT, msg, null);
    }

    @ExceptionHandler({CannotCreateTransactionException.class, CannotGetJdbcConnectionException.class,
            DataAccessResourceFailureException.class})
    public ResponseEntity<Map<String, Object>> handleBancoIndisponivel(Exception ex) {
        log.error("Banco de dados indisponível", ex);
        return montarResposta(HttpStatus.SERVICE_UNAVAILABLE,
                "O sistema não conseguiu acessar o banco de dados no momento. Tente novamente em instantes.", null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleErroDeDados(DataAccessException ex) {
        log.error("Erro de acesso a dados", ex);
        return montarResposta(HttpStatus.INTERNAL_SERVER_ERROR, MSG_ERRO_INESPERADO, null);
    }

    // ------------------------------------------------------------------
    // Qualquer outra falha inesperada: detalhes no log, mensagem genérica para o usuário
    // ------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return montarResposta(HttpStatus.INTERNAL_SERVER_ERROR, MSG_ERRO_INESPERADO, null);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private String mensagemExclusaoBloqueada(Recurso recurso) {
        if (recurso == null) {
            return "Não foi possível excluir este registro porque ele está vinculado a outros registros do sistema.";
        }
        return "Não foi possível excluir " + recurso.artigoNome() + " porque existem "
                + recurso.dependentes() + " vinculados a " + recurso.pronome() + ".";
    }

    /** Identifica o cadastro pela URL: /api/{recurso}/... */
    private Recurso recursoDaRequisicao(HttpServletRequest request) {
        String[] partes = request.getRequestURI().split("/");
        for (int i = 0; i < partes.length - 1; i++) {
            if ("api".equals(partes[i])) {
                return RECURSOS.get(partes[i + 1]);
            }
        }
        return null;
    }

    private String causaRaiz(Throwable ex) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(ex);
        return causa.getMessage() != null ? causa.getMessage() : ex.getClass().getSimpleName();
    }

    private String mensagemOuPadrao(String mensagem, String campo) {
        // Mensagens padrão do Bean Validation podem vir em inglês; evita expô-las.
        if (mensagem == null || mensagem.matches("(?i)^(must|should|size must|numeric value).*")) {
            return "Valor inválido no campo '" + campo + "'.";
        }
        return mensagem;
    }

    private String resumir(Map<String, String> campos) {
        if (campos.isEmpty()) {
            return "Verifique os campos informados.";
        }
        return campos.values().stream().distinct().collect(Collectors.joining(" "));
    }

    private ResponseEntity<Map<String, Object>> montarResposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now().toString());
        corpo.put("status", status.value());
        corpo.put("erro", mensagem);
        if (campos != null && !campos.isEmpty()) {
            corpo.put("campos", campos);
        }
        return ResponseEntity.status(status).body(corpo);
    }
}
