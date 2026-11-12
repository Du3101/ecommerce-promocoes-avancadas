package br.edu.fatec.ecommerce.modulo3.mediador;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaComandoReservaEstoque;
import br.edu.fatec.ecommerce.modulo3.eventos.BarramentoEventosDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.PagamentoRecusadoEvent;
import br.edu.fatec.ecommerce.modulo3.eventos.PedidoCanceladoEvent;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.fabrica.GatewayPagamentoServiceLocator;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Coordenador central das compensações pós-venda: cancelamento de pedido e recusa de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Mediator</i>. Os participantes — o {@link Pedido}
 * (máquina de estados), o gateway de pagamento (estorno), a transação financeira, a porta de estoque do Módulo 1
 * (liberar reserva ou devolver unidades) e o barramento de eventos (notificações) — <b>não se conhecem</b>. Cada um
 * só fala com o mediador, que decide a ordem e o que chamar.
 *
 * <p><b>Problema resolvido:</b> sem o mediador, o pedido precisaria chamar o gateway, o gateway precisaria avisar
 * o estoque, o estoque precisaria avisar o pedido... uma teia de referências cruzadas difícil de testar. Aqui toda
 * a regra de compensação está em um único lugar.
 *
 * <p><b>Consistência:</b> os métodos rodam dentro da transação do serviço chamador. Primeiro o estado do pedido é
 * validado ({@code pedido.cancelar} lança exceção em estados proibidos, antes de qualquer efeito colateral); se uma
 * compensação falhar, a transação inteira é desfeita, inclusive a mudança de estado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CoordenadorCancelamentoMediator {

    private final PortaComandoReservaEstoque portaEstoque;
    private final GatewayPagamentoServiceLocator localizadorPagamento;
    private final TransacaoPagamentoJpaRepository transacaoRepository;
    private final BarramentoEventosDominio barramentoEventos;

    /**
     * Construtor com os participantes coordenados.
     *
     * @param portaEstoque porta de comando de estoque do Módulo 1
     * @param localizadorPagamento localizador das famílias de pagamento (para obter o gateway de estorno)
     * @param transacaoRepository repositório das transações financeiras
     * @param barramentoEventos barramento de eventos (Observer)
     */
    public CoordenadorCancelamentoMediator(PortaComandoReservaEstoque portaEstoque,
                                           GatewayPagamentoServiceLocator localizadorPagamento,
                                           TransacaoPagamentoJpaRepository transacaoRepository,
                                           BarramentoEventosDominio barramentoEventos) {
        this.portaEstoque = portaEstoque;
        this.localizadorPagamento = localizadorPagamento;
        this.transacaoRepository = transacaoRepository;
        this.barramentoEventos = barramentoEventos;
    }

    /**
     * Resultado das compensações executadas em um cancelamento.
     *
     * @param estornoRealizado se houve devolução financeira
     * @param protocoloEstorno protocolo do estorno no provedor (ou {@code null})
     * @param acaoEstoque {@code "RESERVA_LIBERADA"} ou {@code "ESTOQUE_DEVOLVIDO"}
     */
    public record ResultadoCompensacao(boolean estornoRealizado, String protocoloEstorno, String acaoEstoque) {
    }

    /**
     * Coordena o cancelamento de um pedido e todas as compensações necessárias.
     *
     * <p>Passos: (1) consulta polimórfica ao State se o pagamento já foi capturado; (2) transição de estado (valida
     * se o cancelamento é permitido); (3a) pedido pago: estorno no gateway + devolução das unidades já baixadas;
     * (3b) pedido não pago: cancela a cobrança pendente + libera a reserva; (4) publica {@link PedidoCanceladoEvent}.</p>
     *
     * @param pedido pedido carregado com lock pessimista
     * @param motivo motivo do cancelamento
     * @return resultado das compensações
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir cancelamento (ex.: pedido enviado)
     */
    public ResultadoCompensacao coordenarCancelamento(Pedido pedido, String motivo) {
        // 1. Pergunta ao State (antes da transição) se o dinheiro já foi capturado
        boolean pagamentoCapturado = pedido.pagamentoJaCapturado();

        // 2. Transição de estado: lança exceção e interrompe tudo se o cancelamento for proibido
        pedido.cancelar(motivo);

        Optional<TransacaoPagamento> transacao = this.transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId());
        ResultadoCompensacao resultado;
        if (pagamentoCapturado) {
            // 3a. Pedido pago: devolve o dinheiro e as unidades que já tinham saído do estoque
            String protocolo = estornarPagamento(pedido, transacao);
            this.portaEstoque.estornarBaixaDefinitivaPorCancelamento(pedido.getChaveOperacaoEstoque(), motivo);
            resultado = new ResultadoCompensacao(true, protocolo, "ESTOQUE_DEVOLVIDO");
        } else {
            // 3b. Pedido ainda não pago: invalida a cobrança pendente e libera a reserva
            transacao.ifPresent(TransacaoPagamento::cancelar);
            this.portaEstoque.liberarReservaPorCancelamentoOuRecusa(pedido.getChaveOperacaoEstoque(), motivo);
            resultado = new ResultadoCompensacao(false, null, "RESERVA_LIBERADA");
        }

        // 4. Notifica todos os interessados (e-mail, auditoria...) sem conhecê-los
        this.barramentoEventos.publicar(PedidoCanceladoEvent.de(pedido, pagamentoCapturado));
        return resultado;
    }

    /**
     * Coordena a recusa de um pagamento: muda o estado, marca a transação e libera a reserva de estoque.
     *
     * @param pedido pedido aguardando pagamento (ou recém-criado, no caso do cartão)
     * @param motivo motivo informado pelo gateway
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir recusa
     */
    public void coordenarRecusaPagamento(Pedido pedido, String motivo) {
        String motivoEfetivo = motivo != null && !motivo.isBlank() ? motivo : "Pagamento recusado pelo gateway";
        // 1. Transição de estado (valida antes de qualquer efeito colateral)
        pedido.recusarPagamento(motivoEfetivo);
        // 2. Marca a transação financeira como recusada
        this.transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId())
                .ifPresent(TransacaoPagamento::recusar);
        // 3. Devolve imediatamente as unidades reservadas ao estoque disponível (Princípio IV)
        this.portaEstoque.liberarReservaPorCancelamentoOuRecusa(pedido.getChaveOperacaoEstoque(), motivoEfetivo);
        // 4. Notifica o cliente
        this.barramentoEventos.publicar(PagamentoRecusadoEvent.de(pedido, motivoEfetivo));
    }

    /**
     * Solicita o estorno no gateway da família de pagamento do pedido e marca a transação como estornada.
     *
     * @param pedido pedido cancelado
     * @param transacao transação financeira do pedido (se existir)
     * @return protocolo do estorno no provedor
     */
    private String estornarPagamento(Pedido pedido, Optional<TransacaoPagamento> transacao) {
        String idGateway = transacao.map(TransacaoPagamento::getIdentificadorGateway).orElse(pedido.getNumeroPedido());
        String protocolo = this.localizadorPagamento.obterGateway(pedido.getMetodoPagamento())
                .solicitarEstorno(idGateway, pedido.getFinanceiro().getValorTotalLiquido());
        transacao.ifPresent(TransacaoPagamento::estornar);
        return protocolo;
    }
}
