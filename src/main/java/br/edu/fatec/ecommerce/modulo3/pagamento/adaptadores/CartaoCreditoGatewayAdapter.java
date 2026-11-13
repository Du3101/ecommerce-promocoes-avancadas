package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador da operadora de cartão de crédito (simulada, apenas cartões fictícios).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i> com autorização <b>online</b>: a
 * resposta é definitiva no próprio checkout.
 *
 * <p><b>Regras determinísticas da simulação</b> (para testes e demonstração):
 * <ul>
 *   <li>número com menos de 13 ou mais de 19 dígitos → recusado ("dados do cartão inválidos");</li>
 *   <li>número terminado em {@code 0000} → recusado ("saldo/limite insuficiente");</li>
 *   <li>qualquer outro número → aprovado (ex.: {@code 4111111111111111}).</li>
 * </ul>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CartaoCreditoGatewayAdapter implements GatewayPagamentoPort {

    private static final Logger LOG = LoggerFactory.getLogger(CartaoCreditoGatewayAdapter.class);

    /**
     * Solicita a autorização online do cartão.
     *
     * @param requisicao dados da cobrança (inclui cartão fictício)
     * @return resposta {@code CONFIRMADA} ou {@code RECUSADA}
     */
    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        String idTentativa = "CC-AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String digitos = requisicao.numeroCartaoFicticio() == null
                ? ""
                : requisicao.numeroCartaoFicticio().replaceAll("\\D", "");

        // 1. Validação de formato feita pela operadora
        if (digitos.length() < 13 || digitos.length() > 19) {
            return RespostaPagamentoDTO.recusa(idTentativa, "Dados do cartão inválidos.");
        }
        // 2. Análise de crédito simulada
        if (digitos.endsWith("0000")) {
            return RespostaPagamentoDTO.recusa(idTentativa, "Transação recusada pela operadora: saldo ou limite insuficiente.");
        }
        // 3. Autorização aprovada
        return RespostaPagamentoDTO.cartaoAprovado(idTentativa, requisicao.finalCartao());
    }

    /**
     * Simula o cancelamento da compra na fatura do cartão.
     *
     * @param identificadorGateway autorização original
     * @param valor valor estornado
     * @return protocolo do estorno
     */
    @Override
    public String solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        String protocolo = "CC-EST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOG.info("[CARTAO] Estorno de R$ {} da autorização {} solicitado (protocolo {}).", valor, identificadorGateway, protocolo);
        return protocolo;
    }
}
