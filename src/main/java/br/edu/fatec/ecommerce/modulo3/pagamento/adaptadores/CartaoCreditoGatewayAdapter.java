package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PagamentoInvalidoException;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para pagamentos com cartão de crédito fictício.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Adapter do padrão <b>Adapter</b> / Camada Anticorrupção (ACL).
 * Simula autorização online da adquirente com verificação de limite fictício.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CartaoCreditoGatewayAdapter implements GatewayPagamentoPort {

    @Override
    public boolean suporta(MetodoPagamento metodo) {
        return metodo == MetodoPagamento.CARTAO_CREDITO;
    }

    @Override
    public RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao) {
        // Validação de cartão fictício para teste: número terminando em '0000' simula recusa
        if (requisicao.getNumeroCartaoFicticio() != null && requisicao.getNumeroCartaoFicticio().endsWith("0000")) {
            throw new PagamentoInvalidoException("Transação com cartão de crédito recusada pela operadora.");
        }
        String idGateway = "CC-AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return RespostaPagamentoDTO.sucessoCartao(idGateway);
    }

    @Override
    public void solicitarEstorno(String identificadorGateway, BigDecimal valor) {
        // Simulação de cancelamento/estorno de transação na adquirente
    }
}
