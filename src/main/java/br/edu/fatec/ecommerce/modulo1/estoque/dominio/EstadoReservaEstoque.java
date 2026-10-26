package br.edu.fatec.ecommerce.modulo1.estoque.dominio;

/**
 * Ciclo de vida de uma {@link ReservaEstoque} (Clarificação Q3 / FR-010, FR-011).
 *
 * <p>Regras de transição:</p>
 * <ul>
 *   <li>{@code ATIVA -> CONFIRMADA} (baixa definitiva após pagamento aprovado; idempotente se já {@code CONFIRMADA})</li>
 *   <li>{@code ATIVA -> LIBERADA} (devolução ao disponível por cancelamento/recusa; idempotente se já {@code LIBERADA} ou {@code EXPIRADA})</li>
 *   <li>{@code ATIVA -> EXPIRADA} (devolução automática ao disponível por vencimento de TTL)</li>
 * </ul>
 */
public enum EstadoReservaEstoque {

    /** Reserva ativa bloqueando saldo até confirmação do pagamento ou vencimento do TTL. */
    ATIVA,

    /** Reserva convertida em baixa definitiva após confirmação de pagamento. */
    CONFIRMADA,

    /** Reserva cancelada/estornada com devolução integral do saldo ao estoque disponível. */
    LIBERADA,

    /** Reserva vencida por decurso do tempo limite (TTL), com saldo devolvido ao estoque disponível. */
    EXPIRADA
}

