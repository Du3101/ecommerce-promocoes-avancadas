package br.edu.fatec.ecommerce.modulo1.integracao;

import java.util.Map;
import java.util.UUID;

/**
 * Porta de comando transacional fornecida pelo Módulo 1 (Integrante 1) para o Módulo 3
 * (Checkout, Pagamentos e Cancelamento de Pedidos) (FR-010, FR-011, FR-013).
 */
public interface PortaComandoReservaEstoque {

    /**
     * Reserva atomicamente os itens solicitados (decompondo Combos/Kits em SKUs físicos
     * e aplicando lock contra condição de corrida na última unidade).
     *
     * <p>Objetivo Funcional: Bloquear saldo em estoque durante o checkout com semântica tudo-ou-nada
     * e idempotência por {@code chaveOperacaoPedido}.</p>
     * <p>Papel no Design Pattern: Porta de comando integrada aos padrões <b>Composite</b>,
     * <b>Static Factory Method</b> e <b>Singleton</b>.</p>
     *
     * @param chaveOperacaoPedido identificador único da operação de pedido/checkout
     * @param clienteId UUID do cliente comprador
     * @param quantidadePorSku mapa de SKUs do catálogo e quantidades solicitadas
     * @return DTO {@link ResumoReservaEstoqueDTO} no estado {@code ATIVA}
     */
    ResumoReservaEstoqueDTO reservarItensParaCheckout(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadePorSku
    );

    /**
     * Converte a reserva em baixa definitiva de forma idempotente após confirmação do pagamento.
     *
     * <p>Objetivo Funcional: Concluir a saída física definitiva das unidades reservadas.</p>
     * <p>Papel no Design Pattern: Comando de transição idempotente {@code ATIVA -> CONFIRMADA}.</p>
     *
     * @param chaveOperacaoPedido chave única da operação de pedido/reserva
     * @return DTO {@link ResumoReservaEstoqueDTO} no estado {@code CONFIRMADA}
     */
    ResumoReservaEstoqueDTO confirmarBaixaDefinitiva(String chaveOperacaoPedido);

    /**
     * Libera integralmente o estoque reservado de forma idempotente após pagamento recusado
     * ou cancelamento do pedido.
     *
     * <p>Objetivo Funcional: Devolver imediatamente as unidades ao saldo disponível.</p>
     * <p>Papel no Design Pattern: Comando de transição compensatória idempotente {@code ATIVA -> LIBERADA}.</p>
     *
     * @param chaveOperacaoPedido chave única da operação de pedido/reserva
     * @param motivo motivo do cancelamento ou recusa
     * @return DTO {@link ResumoReservaEstoqueDTO} no estado {@code LIBERADA}
     */
    ResumoReservaEstoqueDTO liberarReservaPorCancelamentoOuRecusa(
        String chaveOperacaoPedido,
        String motivo
    );

    /**
     * Desfaz de forma idempotente a baixa definitiva de uma reserva já {@code CONFIRMADA}, devolvendo as
     * unidades ao saldo disponível.
     *
     * <p>Objetivo Funcional: Atender ao cancelamento de um pedido já pago e ainda não enviado (Módulo 3).
     * {@link #liberarReservaPorCancelamentoOuRecusa(String, String)} só vale para reservas {@code ATIVA};
     * depois do pagamento as unidades já saíram do estoque e precisam ser devolvidas por esta operação.</p>
     * <p>Papel no Design Pattern: Comando de compensação idempotente {@code CONFIRMADA -> ESTORNADA_POS_BAIXA}.</p>
     *
     * @param chaveOperacaoPedido chave única da operação de pedido/reserva
     * @param motivo motivo do cancelamento
     * @return DTO {@link ResumoReservaEstoqueDTO} no estado {@code ESTORNADA_POS_BAIXA}
     */
    ResumoReservaEstoqueDTO estornarBaixaDefinitivaPorCancelamento(
        String chaveOperacaoPedido,
        String motivo
    );
}

