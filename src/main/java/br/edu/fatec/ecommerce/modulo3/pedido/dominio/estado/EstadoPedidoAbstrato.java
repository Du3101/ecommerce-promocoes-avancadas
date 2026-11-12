package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;

/**
 * Classe base abstrata para as classes de estado do Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Base do padrão <i>State</i> com política "negar por padrão": toda operação lança
 * {@link TransicaoEstadoInvalidaException}. Cada estado concreto sobrescreve somente as transições válidas a partir
 * dele. Assim, uma transição esquecida falha de forma segura (HTTP 400) em vez de alterar o pedido indevidamente.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public abstract class EstadoPedidoAbstrato implements EstadoPedido {

    /**
     * Bloqueia por padrão a entrada em {@code AGUARDANDO_PAGAMENTO}.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void aguardarPagamento(Pedido pedido) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Aguardar Pagamento");
    }

    /**
     * Bloqueia por padrão a confirmação de pagamento.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @param transacaoId identificador da transação (ignorado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void pagar(Pedido pedido, String transacaoId) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Confirmar Pagamento");
    }

    /**
     * Bloqueia por padrão a recusa de pagamento.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @param motivo motivo da recusa (ignorado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void recusarPagamento(Pedido pedido, String motivo) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Recusar Pagamento");
    }

    /**
     * Bloqueia por padrão o início da preparação.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void preparar(Pedido pedido) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Iniciar Preparação");
    }

    /**
     * Bloqueia por padrão o despacho.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @param codigoRastreio código de rastreio (ignorado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void despachar(Pedido pedido, String codigoRastreio) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Despachar Pedido");
    }

    /**
     * Bloqueia por padrão a confirmação de entrega.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void entregar(Pedido pedido) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Confirmar Entrega");
    }

    /**
     * Bloqueia por padrão o cancelamento.
     *
     * <p>Papel no Design Pattern: comportamento padrão "transição proibida" do State.</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @param motivo motivo do cancelamento (ignorado)
     * @throws TransicaoEstadoInvalidaException sempre, salvo sobrescrita no estado concreto
     */
    @Override
    public void cancelar(Pedido pedido, String motivo) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Cancelar Pedido");
    }

    /**
     * Por padrão, considera que o pagamento ainda não foi capturado.
     *
     * <p>Papel no Design Pattern: consulta polimórfica do State; sobrescrita pelos estados pós-pagamento.</p>
     *
     * @return {@code false}
     */
    @Override
    public boolean pagamentoJaCapturado() {
        return false;
    }
}
