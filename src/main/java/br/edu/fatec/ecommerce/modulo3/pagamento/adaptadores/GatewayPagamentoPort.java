package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;

/**
 * Porta de integração com provedores de liquidação financeira.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Interface <i>Target</i> do padrão <b>Adapter</b> / Camada Anticorrupção (ACL).
 * Permite que o sistema efetue cobranças e solicite estornos junto a gateways externos
 * sem que o domínio conheça estruturas proprietárias de terceiros.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface GatewayPagamentoPort {

    /**
     * Verifica se este adaptador processa o método solicitado.
     *
     * @param metodo Método de pagamento.
     * @return {@code true} se o adaptador processa a modalidade.
     */
    boolean suporta(MetodoPagamento metodo);

    /**
     * Efetua a autorização ou registro inicial de cobrança no provedor financeiro.
     *
     * @param requisicao Dados da cobrança.
     * @return Resposta padronizada do gateway.
     */
    RespostaPagamentoDTO processarCobranca(RequisicaoCobrancaDTO requisicao);

    /**
     * Solicita o estorno financeiro de uma transação liquidada.
     *
     * @param identificadorGateway Código retornado originalmente pelo gateway.
     * @param valor Valor monetário a ser estornado.
     */
    void solicitarEstorno(String identificadorGateway, BigDecimal valor);
}
