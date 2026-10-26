package br.edu.fatec.ecommerce.modulo1.estoque.aplicacao;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;

/**
 * Interface de porta para operações administrativas de ajuste manual e reposição de estoque (FR-009, FR-012).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papel <i>Subject</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Implementada tanto pelo {@link ServicoAdminEstoqueReal} quanto pelo
 * {@code EstoqueAdminSegurancaProxy}, permitindo interceptar ajustes manuais de estoque para
 * validação estrita de perfil ({@code ADMIN} ou {@code RESPONSAVEL_ESTOQUE}) e gravação automática
 * de trilha de auditoria.</p>
 */
public interface ServicoAdminEstoquePort {

    /**
     * Realiza entrada de mercadoria ou ajuste manual de inventário sobre o saldo disponível de um SKU.
     *
     * <p>Objetivo Funcional: Atualizar saldo disponível registrando movimentação e auditoria (FR-009, FR-012).</p>
     * <p>Papel no Design Pattern: Operação do <i>Subject</i> interceptada pelo <b>Proxy</b>.</p>
     *
     * @param sku código SKU da variação física
     * @param deltaQuantidade variação de unidades (positiva para entrada, negativa para baixa manual)
     * @param motivo justificativa obrigatória do ajuste
     * @return posição de {@link ItemEstoque} atualizada
     */
    ItemEstoque ajustarSaldoDisponivelManual(String sku, int deltaQuantidade, String motivo);

    /**
     * Aciona administrativamente a varredura e expiração de reservas vencidas por TTL.
     *
     * <p>Objetivo Funcional: Liberar saldos retidos em reservas vencidas (FR-011).</p>
     * <p>Papel no Design Pattern: Operação administrativa protegida pelo <b>Proxy</b>.</p>
     *
     * @return número de reservas vencidas que foram expiradas
     */
    int acionarExpiracaoDeReservasVencidas();
}

