package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pedido de cobrança no modelo interno, entregue a um {@code GatewayPagamentoPort}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> DTO do lado "domínio" da Anti-Corruption Layer. Cada adaptador
 * de gateway traduz estes dados para o formato do seu provedor. Dados de cartão são estritamente fictícios
 * (Princípio V da Constituição) e nunca são persistidos.
 *
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param metodo método de pagamento
 * @param valor valor a cobrar
 * @param chaveIdempotencia chave idempotente da cobrança (evita cobrança dupla no provedor)
 * @param nomeCliente nome do pagador
 * @param cpfCliente CPF do pagador (exigido por PIX e boleto)
 * @param numeroCartaoFicticio número de cartão fictício (somente cartão)
 * @param titularCartao nome impresso no cartão fictício (somente cartão)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record RequisicaoCobrancaDTO(
        UUID pedidoId,
        String numeroPedido,
        MetodoPagamento metodo,
        BigDecimal valor,
        String chaveIdempotencia,
        String nomeCliente,
        String cpfCliente,
        String numeroCartaoFicticio,
        String titularCartao
) {

    /**
     * Retorna os 4 últimos dígitos do cartão (para exibição segura).
     *
     * @return últimos 4 dígitos ou {@code "****"} se não houver cartão
     */
    public String finalCartao() {
        if (numeroCartaoFicticio == null) {
            return "****";
        }
        String digitos = numeroCartaoFicticio.replaceAll("\\D", "");
        return digitos.length() >= 4 ? digitos.substring(digitos.length() - 4) : "****";
    }
}
