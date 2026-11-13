package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

/**
 * Enumeração dos estados permitidos no ciclo de vida de um Pedido de compra.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como identificador tipado do estado ativo gerenciado pelo padrão <i>State</i>
 * (GoF State Pattern). Cada constante corresponde biunivocamente a uma classe polimórfica
 * concreta que implementa a interface {@code EstadoPedido}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum StatusPedido {

    /**
     * Pedido recém-criado no checkout, aguardando início da liquidação financeira.
     */
    CRIADO("Criado"),

    /**
     * Pedido com chave de pagamento gerada, aguardando webhook ou liquidação do gateway.
     */
    AGUARDANDO_PAGAMENTO("Aguardando Pagamento"),

    /**
     * Pagamento confirmado com sucesso; reserva convertida em baixa definitiva no estoque.
     */
    PAGO("Pago"),

    /**
     * Pedido em processo físico de separação e embalagem no centro de distribuição.
     */
    EM_PREPARACAO("Em Preparação"),

    /**
     * Pedido despachado para a transportadora com código de rastreamento gerado.
     */
    ENVIADO("Enviado"),

    /**
     * Pedido entregue ao destinatário final (estado terminal com sucesso).
     */
    ENTREGUE("Entregue"),

    /**
     * Pedido cancelado pelo cliente ou administrador, com liberação de estoque e estorno.
     */
    CANCELADO("Cancelado"),

    /**
     * Pagamento recusado pelo gateway financeiro; reserva de estoque liberada imediatamente.
     */
    PAGAMENTO_RECUSADO("Pagamento Recusado");

    private final String descricaoAmigavel;

    /**
     * Construtor explícito do enum com sua descrição formatada para exibição em relatórios e interfaces.
     *
     * @param descricaoAmigavel Rótulo legível para visualização na API e relatórios.
     */
    StatusPedido(String descricaoAmigavel) {
        this.descricaoAmigavel = descricaoAmigavel;
    }

    /**
     * Retorna a descrição legível para exibição ao usuário final.
     *
     * @return Descrição em português do status.
     */
    public String getDescricaoAmigavel() {
        return descricaoAmigavel;
    }

    /**
     * Indica se o estado atual representa um estado terminal definitivo do ciclo de vida.
     *
     * @return {@code true} se o pedido estiver em estado que não permite transições subsequentes.
     */
    public boolean isEstadoTerminal() {
        return this == ENTREGUE || this == CANCELADO || this == PAGAMENTO_RECUSADO;
    }
}
