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
 * Adaptador do provedor PIX (simulado).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i>. Gera a chave copia-e-cola no padrão
 * BR Code (EMV) e um QR Code ilustrativo, com validade de 30 minutos. A liquidação chega depois pelo webhook.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class PixGatewayAdapter implements GatewayPagamentoPort {

    private static final Logger LOG = LoggerFactory.getLogger(PixGatewayAdapter.class);

    /** Validade da cobrança PIX. */
    private static final Duration VALIDADE_PIX = Duration.ofMinutes(30);

    /** Imagem PNG 1x1 em Base64, usada apenas para ilustrar o campo de QR Code. */
    private static final String QR_CODE_ILUSTRATIVO =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    /**
     * Emite a cobrança PIX.
     *
     * @param requisicao dados da cobrança
     * @return resposta pendente com chave copia-e-cola, QR Code e validade
     */
    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        // 1. Identificador da cobrança no provedor
        String idGateway = "PIX-GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // 2. Chave copia-e-cola no formato BR Code (campos EMV simplificados)
        String chaveCopiaECola = String.format(
                "00020126580014br.gov.bcb.pix0136%s520400005303986540%s5802BR5911FATEC STORE6009SAO PAULO62070503***6304E2D1",
                idGateway, requisicao.valor().toPlainString());
        return RespostaPagamentoDTO.pixEmitido(idGateway, chaveCopiaECola, QR_CODE_ILUSTRATIVO,
                Instant.now().plus(VALIDADE_PIX));
    }

    /**
     * Simula a devolução PIX ao pagador.
     *
     * @param identificadorGateway transação original
     * @param valor valor devolvido
     * @return protocolo da devolução
     */
    @Override
    public String solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        String protocolo = "PIX-DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOG.info("[PIX] Devolução de R$ {} da transação {} solicitada (protocolo {}).", valor, identificadorGateway, protocolo);
        return protocolo;
    }
}
