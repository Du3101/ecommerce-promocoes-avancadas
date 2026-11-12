package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Interface polimórfica que define as operações e transições válidas no ciclo de vida de um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a interface <i>State</i> no padrão GoF State. Cada status do pedido é uma classe concreta
 * que decide sozinha quais operações aceita. O {@link Pedido} (o <i>Context</i>) nunca faz
 * {@code switch(status)}: ele apenas delega para o estado atual.
 *
 * <p><b>Problema resolvido:</b> sem o padrão, cada operação do pedido teria um bloco
 * {@code if (status == ...)} repetido, e esquecer uma combinação permitiria, por exemplo, pagar um pedido cancelado.
 *
 * <p><b>Guia de extensão:</b> para criar um novo estado (ex.: {@code DEVOLUCAO_SOLICITADA}), crie uma classe que
 * estenda {@link EstadoPedidoAbstrato}, sobrescreva apenas as transições permitidas a partir dele, adicione a
 * constante em {@link StatusPedido} e ajuste somente os estados vizinhos que transitam para ele.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface EstadoPedido {

    /**
     * Retorna o enum persistido associado a esta implementação de estado.
     *
     * <p>Papel no Design Pattern: liga o objeto polimórfico (memória) à coluna {@code status} (banco).</p>
     *
     * @return constante de {@link StatusPedido}
     */
    StatusPedido getStatus();

    /**
     * Coloca o pedido recém-criado na fila de espera da confirmação do pagamento (PIX, boleto).
     *
     * <p>Papel no Design Pattern: transição {@code CRIADO -> AGUARDANDO_PAGAMENTO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void aguardarPagamento(Pedido pedido);

    /**
     * Confirma o pagamento financeiro do pedido.
     *
     * <p>Papel no Design Pattern: transição para {@code PAGO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @param transacaoId identificador da liquidação no gateway
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void pagar(Pedido pedido, String transacaoId);

    /**
     * Registra a recusa definitiva do pagamento pelo gateway.
     *
     * <p>Papel no Design Pattern: transição para {@code PAGAMENTO_RECUSADO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @param motivo justificativa da recusa
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void recusarPagamento(Pedido pedido, String motivo);

    /**
     * Inicia a preparação e separação física do pedido na expedição.
     *
     * <p>Papel no Design Pattern: transição {@code PAGO -> EM_PREPARACAO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void preparar(Pedido pedido);

    /**
     * Despacha o pedido para o transporte com emissão de rastreio.
     *
     * <p>Papel no Design Pattern: transição {@code EM_PREPARACAO -> ENVIADO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @param codigoRastreio código de rastreamento emitido pela transportadora
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void despachar(Pedido pedido, String codigoRastreio);

    /**
     * Registra a entrega concluída ao cliente.
     *
     * <p>Papel no Design Pattern: transição {@code ENVIADO -> ENTREGUE} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação
     */
    void entregar(Pedido pedido);

    /**
     * Executa o cancelamento do pedido.
     *
     * <p>Papel no Design Pattern: transição para {@code CANCELADO} controlada pelo estado.</p>
     *
     * @param pedido agregado raiz do pedido (Context)
     * @param motivo motivo do cancelamento
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir a operação (ex.: pedido já enviado)
     */
    void cancelar(Pedido pedido, String motivo);

    /**
     * Informa se, neste estado, o dinheiro do cliente já foi capturado e o estoque já teve baixa definitiva.
     *
     * <p>Objetivo Funcional: o cancelamento precisa saber se deve apenas liberar a reserva (pedido não pago) ou
     * solicitar estorno financeiro e devolver unidades já baixadas (pedido pago).</p>
     * <p>Papel no Design Pattern: consulta polimórfica do <i>State</i> usada pelo <i>Mediator</i> de cancelamento,
     * evitando um {@code switch(status)} fora da máquina de estados.</p>
     *
     * @return {@code true} se o pagamento já foi confirmado neste estado
     */
    boolean pagamentoJaCapturado();
}
