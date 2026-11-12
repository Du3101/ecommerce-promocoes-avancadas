package br.edu.fatec.ecommerce.modulo3.pagamento.api;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.AcessoNegadoModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoRequestDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao.ServicoProcessamentoPagamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook chamado pelo gateway de pagamento para informar aprovação ou recusa.
 *
 * <p>A rota não exige login de usuário (quem chama é o gateway), mas exige o cabeçalho {@code X-Gateway-Token}
 * com o segredo compartilhado configurado em {@code ecommerce.modulo3.webhook-token} — sem ele, qualquer pessoa
 * poderia "aprovar" o próprio pedido.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/pagamentos")
@Tag(name = "Pagamentos", description = "Módulo 3 — Webhook de confirmação de pagamento com idempotência estrita")
public class WebhookPagamentoController {

    /** Nome do cabeçalho com o segredo compartilhado do gateway. */
    public static final String CABECALHO_TOKEN = "X-Gateway-Token";

    private final ServicoProcessamentoPagamento servicoPagamento;
    private final String tokenEsperado;

    /**
     * Construtor com dependências injetadas.
     *
     * @param servicoPagamento serviço de processamento de pagamento
     * @param tokenEsperado segredo compartilhado com o gateway
     */
    public WebhookPagamentoController(ServicoProcessamentoPagamento servicoPagamento,
                                      @Value("${ecommerce.modulo3.webhook-token:gateway-simulado-fatec}") String tokenEsperado) {
        this.servicoPagamento = servicoPagamento;
        this.tokenEsperado = tokenEsperado;
    }

    /**
     * Recebe a confirmação (ou recusa) do pagamento.
     *
     * @param token segredo compartilhado enviado pelo gateway
     * @param requisicao notificação do gateway
     * @return resultado; repetições retornam {@code idempotenteReexecutado = true}
     */
    @PostMapping("/confirmacao")
    @Operation(summary = "Webhook idempotente de confirmação/recusa de pagamento (header X-Gateway-Token obrigatório)")
    public ConfirmacaoPagamentoResponseDTO confirmar(
            @RequestHeader(value = CABECALHO_TOKEN, required = false) String token,
            @Valid @RequestBody ConfirmacaoPagamentoRequestDTO requisicao) {
        validarToken(token);
        return servicoPagamento.processarNotificacaoGateway(requisicao);
    }

    /**
     * Compara o token recebido com o esperado em tempo constante (evita ataque de temporização).
     *
     * @param token token recebido
     * @throws AcessoNegadoModulo3Exception se o token estiver ausente ou incorreto
     */
    private void validarToken(String token) {
        boolean valido = token != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), tokenEsperado.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            throw new AcessoNegadoModulo3Exception("Webhook recusado: token do gateway ausente ou inválido.");
        }
    }
}
