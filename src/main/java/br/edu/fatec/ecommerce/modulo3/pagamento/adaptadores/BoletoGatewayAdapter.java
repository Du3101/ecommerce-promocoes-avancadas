package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para pagamentos via boleto bancário.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Adapter do padrão <b>Adapter</b> / Camada Anticorrupção (ACL).
 * Gera linha digitável e código de barras padrão FEBRABAN simulados.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class BoletoGatewayAdapter implements GatewayPagamentoPort {

    @Override
    public boolean suporta(MetodoPagamento metodo) {
        return metodo == MetodoPagamento.BOLETO;
    }

    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        String idGateway = "BOL-GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String linhaDigitavel = "34191.79001 01043.510047 91020.150008 4 99120000052000";
        return RespostaPagamentoDTO.sucessoBoleto(idGateway, linhaDigitavel);
    }

    @Override
    public void solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        // Simulação de estorno/cancelamento de boleto bancário
    }
}
