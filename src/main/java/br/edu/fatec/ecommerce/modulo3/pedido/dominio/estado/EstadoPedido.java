package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Interface polimórfica que define as operações e transições válidas no ciclo de vida de um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a interface <i>State</i> no padrão GoF (GoF State Pattern).
 * Encapsula os comportamentos específicos de cada status do pedido e garante
 * que transições ilegais sejam bloqueadas com exceções explícitas de domínio.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface EstadoPedido {

    /**
     * Retorna o enum associado a esta implementação de estado.
     *
     * @return Constante de {@link StatusPedido}.
     */
    StatusPedido getStatus();

    /**
     * Confirma o pagamento financeiro do pedido.
     *
     * @param pedido Agregado raiz do pedido.
     * @param transacaoId Identificador da liquidação no gateway.
     */
    void pagar(Pedido pedido, String transacaoId);

    /**
     * Registra a recusa definitiva do pagamento pelo gateway.
     *
     * @param pedido Agregado raiz do pedido.
     * @param motivo Justificativa da recusa.
     */
    void recusarPagamento(Pedido pedido, String motivo);

    /**
     * Inicia a preparação e separação física do pedido na expedição.
     *
     * @param pedido Agregado raiz do pedido.
     */
    void preparar(Pedido pedido);

    /**
     * Despacha o pedido para o transporte com emissão de rastreio.
     *
     * @param pedido Agregado raiz do pedido.
     * @param codigoRastreio Código de rastreamento emitido pela transportadora.
     */
    void despachar(Pedido pedido, String codigoRastreio);

    /**
     * Registra a entrega concluída ao cliente.
     *
     * @param pedido Agregado raiz do pedido.
     */
    void entregar(Pedido pedido);

    /**
     * Executa o cancelamento do pedido.
     *
     * @param pedido Agregado raiz do pedido.
     * @param motivo Motivo do cancelamento.
     */
    void cancelar(Pedido pedido, String motivo);
}
