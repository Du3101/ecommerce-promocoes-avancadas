package br.edu.fatec.ecommerce.compartilhado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Ponto de entrada de autenticação HTTP para requisições REST sem emissão do cabeçalho
 * {@code WWW-Authenticate}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como adaptador de resposta de erro de segurança na fronteira HTTP (<b>Proxy / Interceptor</b>
 * da cadeia {@code SecurityFilterChain}), substituindo o desafio {@code Basic realm} padrão que
 * disparava caixas de diálogo nativas no navegador em chamadas {@code fetch()} da interface web.</p>
 */
public class EntradaAutenticacaoRestSemPopup implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Construtor explícito recebendo o serializador JSON do contexto Spring.
     *
     * <p>Objetivo Funcional: Garantir formatação JSON consistente ao responder falhas 401.</p>
     * <p>Papel no Design Pattern: Injeção de dependência de serializador para a borda de segurança.</p>
     *
     * @param objectMapper instância configurada de {@link ObjectMapper}
     */
    public EntradaAutenticacaoRestSemPopup(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Intercepta tentativas não autenticadas ou com credenciais inválidas devolvendo HTTP 401 JSON
     * sem o cabeçalho {@code WWW-Authenticate}.
     *
     * <p>Objetivo Funcional: Permitir que o portal web trate falhas 401 diretamente no formulário
     * de login sem provocar popups nativos do navegador.</p>
     * <p>Papel no Design Pattern: Gancho de resposta não autorizada da cadeia de segurança do Spring.</p>
     *
     * @param request requisição HTTP recebida
     * @param response resposta HTTP a ser preenchida
     * @param authException exceção de autenticação capturada pelo filtro de segurança
     * @throws IOException caso ocorra falha ao escrever o corpo JSON na resposta
     * @throws ServletException caso ocorra erro no processamento servlet
     */
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> corpoErro = construirPayloadErroNaoAutenticado(authException);
        response.getWriter().write(this.objectMapper.writeValueAsString(corpoErro));
    }

    /**
     * Monta o mapa JSON padronizado compatível com o {@code ManipuladorGlobalExcecoesRest}.
     *
     * <p>Objetivo Funcional: Assegurar estrutura uniforme com timestamp, status 401, código
     * {@code NAO_AUTENTICADO} e mensagem legível ao cliente web.</p>
     * <p>Papel no Design Pattern: Método auxiliar fábrica de payload de resposta HTTP 401.</p>
     *
     * @param authException exceção original opcional contendo detalhe da falha
     * @return mapa ordenado pronto para serialização JSON
     */
    private Map<String, Object> construirPayloadErroNaoAutenticado(AuthenticationException authException) {
        String mensagemPadrao = "Autenticação obrigatória para acessar este recurso.";
        String detalhe = (authException != null && authException.getMessage() != null
            && !authException.getMessage().isBlank())
            ? authException.getMessage()
            : mensagemPadrao;

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", HttpStatus.UNAUTHORIZED.value());
        corpo.put("codigo", "NAO_AUTENTICADO");
        corpo.put("mensagem", detalhe);
        return corpo;
    }
}
