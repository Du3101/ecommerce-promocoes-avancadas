package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import java.math.BigDecimal;

/**
 * Porta interna de cobrança e estorno junto a um provedor de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> interface <i>Target</i> do padrão <b>Adapter</b> /
 * Anti-Corruption Layer. Cada provedor (PIX, cartão, boleto) tem seu adaptador; o checkout e o cancelamento
 * só conhecem esta interface. A escolha do adaptador é feita pela família de pagamento (Abstract Factory) e pelo
 * {@code GatewayPagamentoServiceLocator}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface GatewayPagamentoPort {

    /**
     * Emite a cobrança no provedor.
     *
     * <p>Recusas são devolvidas como {@link RespostaPagamentoDTO#recusa(String, String)} (resultado de negócio),
     * nunca como exceção, para que o checkout possa liberar o estoque e registrar a recusa.</p>
     *
     * @param requisicao dados da cobrança no modelo interno
     * @return resposta traduzida para o modelo interno
     */
    RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao);

    /**
     * Solicita a devolução do valor ao cliente (cancelamento de pedido pago).
     *
     * @param identificadorGateway identificador da transação original no provedor
     * @param valor valor a devolver
     * @return protocolo do estorno no provedor
     */
    String solicitarEstorno(String identificadorGateway, BigDecimal valor);
}
