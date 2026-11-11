package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para pagamentos instantâneos via PIX.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Adapter do padrão <b>Adapter</b> / Camada Anticorrupção (ACL).
 * Converte a chamada de checkout em cobrança PIX com chave aleatória copia-e-cola e payload QR Code.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class PixGatewayAdapter implements GatewayPagamentoPort {

    @Override
    public boolean suporta(MetodoPagamento metodo) {
        return metodo == MetodoPagamento.PIX;
    }

    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        String idGateway = "PIX-GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String chaveCopiaECola = String.format("00020126580014br.gov.bcb.pix0136%s520400005303986540%s5802BR5910FATEC6009SAOPAULO62070503***6304E2D1",
                idGateway, requisicao.getValor());
        String qrCode = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        return RespostaPagamentoDTO.sucessoPix(idGateway, chaveCopiaECola, qrCode);
    }

    @Override
    public void solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        // Simulação de estorno instantâneo PIX via devolução BACEN
    }
}
