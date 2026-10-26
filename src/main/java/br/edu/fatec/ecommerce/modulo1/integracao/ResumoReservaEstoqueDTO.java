package br.edu.fatec.ecommerce.modulo1.integracao;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de fronteira imutável que representa o resultado de uma operação de reserva, confirmação
 * ou liberação de estoque para o Módulo 3 (Checkout/Pedidos/Pagamentos) (FR-010, FR-011, FR-013).
 *
 * @param reservaId UUID da reserva
 * @param chaveOperacaoPedido chave única da operação de pedido/checkout
 * @param clienteId UUID do cliente comprador
 * @param estado estado atual da reserva ({@link EstadoReservaEstoque})
 * @param criadaEm instante UTC de criação
 * @param expiraEm instante UTC limite de validade (TTL)
 * @param finalizadaEm instante UTC de confirmação/liberação/expiração (se finalizada)
 * @param quantidadesFisicasReservadasPorSku mapa de SKUs físicos folha e quantidades reservadas
 */
public record ResumoReservaEstoqueDTO(
    UUID reservaId,
    String chaveOperacaoPedido,
    UUID clienteId,
    EstadoReservaEstoque estado,
    Instant criadaEm,
    Instant expiraEm,
    Instant finalizadaEm,
    Map<String, Integer> quantidadesFisicasReservadasPorSku
) {
}

