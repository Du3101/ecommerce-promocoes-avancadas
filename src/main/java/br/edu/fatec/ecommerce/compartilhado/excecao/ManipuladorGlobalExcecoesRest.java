package br.edu.fatec.ecommerce.compartilhado.excecao;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Interceptador global de exceções da camada HTTP REST.
 *
 * <p>Responsabilidade Comercial: Traduzir exceções de domínio do Módulo 1 em respostas
 * JSON padronizadas com códigos HTTP semânticos (400, 403, 404, 409).</p>
 */
@RestControllerAdvice
public class ManipuladorGlobalExcecoesRest {

    /**
     * Trata recursos não encontrados no domínio e retorna HTTP 404 Not Found.
     *
     * <p>Objetivo Funcional: Informar claramente ao consumidor da API qual recurso não existe.</p>
     * <p>Papel no Design Pattern: Tradução de exceção na camada de fronteira HTTP.</p>
     *
     * @param ex exceção de recurso não encontrado
     * @return resposta estruturada com status 404
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return construirRespostaErro(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", ex.getMessage());
    }

    /**
     * Trata falhas de credenciais de login e retorna HTTP 401 Unauthorized.
     *
     * <p>Objetivo Funcional: Negar autenticação quando e-mail ou senha estiverem incorretos.</p>
     * <p>Papel no Design Pattern: Resposta HTTP padronizada para falhas de autenticação.</p>
     *
     * @param ex exceção de autenticação do Spring Security
     * @return resposta estruturada com status 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> tratarFalhaAutenticacao(AuthenticationException ex) {
        return construirRespostaErro(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", ex.getMessage());
    }

    /**
     * Trata violações de permissão de perfil (RBAC) e retorna HTTP 403 Forbidden.
     *
     * <p>Objetivo Funcional: Negar ações não autorizadas capturadas pelos Proxies de proteção.</p>
     * <p>Papel no Design Pattern: Resposta HTTP para bloqueios disparados pelo padrão <b>Proxy</b>.</p>
     *
     * @param ex exceção de acesso negado
     * @return resposta estruturada com status 403
     */
    @ExceptionHandler(AcessoNegadoModulo1Exception.class)
    public ResponseEntity<Map<String, Object>> tratarAcessoNegado(AcessoNegadoModulo1Exception ex) {
        return construirRespostaErro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", ex.getMessage());
    }

    /**
     * Trata conflitos de estoque insuficiente, duplicidade de chaves ou transição inválida com HTTP 409 Conflict.
     *
     * <p>Objetivo Funcional: Sinalizar conflitos de estado ou falta de estoque no checkout.</p>
     * <p>Papel no Design Pattern: Tratamento de falha concorrente ou violação de unicidade.</p>
     *
     * @param ex exceção de conflito de estado ou duplicidade
     * @return resposta estruturada com status 409
     */
    @ExceptionHandler(ConflitoDeEstadoOuDuplicidadeException.class)
    public ResponseEntity<Map<String, Object>> tratarConflitoOuDuplicidade(ConflitoDeEstadoOuDuplicidadeException ex) {
        return construirRespostaErro(HttpStatus.CONFLICT, "CONFLITO_REGRA_NEGOCIO", ex.getMessage());
    }

    /**
     * Trata demais violações de regra de negócio (ciclos recursivos, valores inválidos) com HTTP 400 Bad Request.
     *
     * <p>Objetivo Funcional: Rejeitar dados inválidos antes de persistir no catálogo ou cadastro.</p>
     * <p>Papel no Design Pattern: Resposta para falhas de validação dos <b>Builders</b>,
     * <b>Static Factory Methods</b> e <b>Composite</b>.</p>
     *
     * @param ex exceção de regra de negócio ou argumento inválido
     * @return resposta estruturada com status 400
     */
    @ExceptionHandler({RegraNegocioModulo1Exception.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> tratarRegraNegocioInvalida(RuntimeException ex) {
        return construirRespostaErro(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", ex.getMessage());
    }

    /**
     * Trata erros de validação declarativa (Bean Validation) nos DTOs de requisição HTTP.
     *
     * <p>Objetivo Funcional: Consolidar mensagens de campos obrigatórios ausentes em HTTP 400.</p>
     * <p>Papel no Design Pattern: Validação sintática de entrada na borda da API REST.</p>
     *
     * @param ex exceção lançada pelo Spring Validation
     * @return resposta estruturada com status 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacaoDto(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .findFirst()
            .orElse("Dados de entrada inválidos.");
        return construirRespostaErro(HttpStatus.BAD_REQUEST, "VALIDACAO_CAMPOS_FALHOU", mensagem);
    }

    /**
     * Método auxiliar explícito que monta o corpo padronizado de erro JSON.
     *
     * <p>Objetivo Funcional: Garantir formato consistente com timestamp, status, código e mensagem.</p>
     * <p>Papel no Design Pattern: Método privado fábrica de payload de erro HTTP.</p>
     *
     * @param status código HTTP correspondente
     * @param codigo identificador textual do erro
     * @param mensagem mensagem explicativa
     * @return entidade HTTP pronta para serialização JSON
     */
    private ResponseEntity<Map<String, Object>> construirRespostaErro(
        HttpStatus status,
        String codigo,
        String mensagem
    ) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
