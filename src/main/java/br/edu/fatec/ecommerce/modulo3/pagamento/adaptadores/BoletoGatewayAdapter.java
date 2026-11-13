package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador do emissor de boletos bancários (simulado).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i>. Gera a linha digitável com o valor
 * codificado e vencimento em 3 dias; a liquidação chega depois pelo webhook.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class BoletoGatewayAdapter implements GatewayPagamentoPort {

    private static final Logger LOG = LoggerFactory.getLogger(BoletoGatewayAdapter.class);

    /** Prazo de vencimento do boleto. */
    private static final Duration PRAZO_VENCIMENTO = Duration.ofDays(3);

    /**
     * Emite o boleto.
     *
     * @param requisicao dados da cobrança
     * @return resposta pendente com linha digitável e vencimento
     */
    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        // 1. Identificador do boleto no emissor
        String idGateway = "BOL-GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // 2. Valor em centavos com 10 dígitos no final da linha digitável (como no padrão FEBRABAN)
        long centavos = requisicao.valor().movePointRight(2).longValue();
        String linhaDigitavel = String.format("34191.79001 01043.510047 91020.150008 4 9912%010d", centavos);
        return RespostaPagamentoDTO.boletoEmitido(idGateway, linhaDigitavel, Instant.now().plus(PRAZO_VENCIMENTO));
    }

    /**
     * Simula a devolução de um boleto pago (transferência ao cliente).
     *
     * @param identificadorGateway boleto original
     * @param valor valor devolvido
     * @return protocolo da devolução
     */
    @Override
    public String solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        String protocolo = "BOL-DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOG.info("[BOLETO] Devolução de R$ {} do boleto {} solicitada (protocolo {}).", valor, identificadorGateway, protocolo);
        return protocolo;
    }
}
