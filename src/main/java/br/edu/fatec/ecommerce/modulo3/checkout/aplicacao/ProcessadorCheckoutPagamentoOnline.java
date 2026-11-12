package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.mediador.CoordenadorCancelamentoMediator;
import br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao.ServicoProcessamentoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import org.springframework.stereotype.Component;

/**
 * Especialização do checkout para liquidação online (cartão de crédito).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Class</i> do Template Method. Implementa a etapa
 * abstrata e sobrescreve os dois hooks:
 * <ul>
 *   <li>exige o número do cartão fictício antes de qualquer efeito colateral;</li>
 *   <li>aprovado: aplica a confirmação na hora (baixa de estoque, cupom, evento);</li>
 *   <li>recusado: o Mediator libera a reserva, e o carrinho <b>não</b> é esvaziado (o cliente pode tentar outro
 *       meio de pagamento).</li>
 * </ul>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class ProcessadorCheckoutPagamentoOnline extends ProcessadorCheckoutTemplate {

    private final ServicoProcessamentoPagamento servicoPagamento;
    private final CoordenadorCancelamentoMediator mediador;

    /**
     * Construtor com os recursos comuns e os colaboradores da liquidação online.
     *
     * @param recursos colaboradores do checkout
     * @param servicoPagamento serviço que aplica a confirmação de pagamento
     * @param mediador mediador de compensações (recusa)
     */
    public ProcessadorCheckoutPagamentoOnline(RecursosCheckout recursos,
                                              ServicoProcessamentoPagamento servicoPagamento,
                                              CoordenadorCancelamentoMediator mediador) {
        super(recursos);
        this.servicoPagamento = servicoPagamento;
        this.mediador = mediador;
    }

    /**
     * Atende cartão de crédito.
     *
     * @return {@link TipoLiquidacao#ONLINE}
     */
    @Override
    public TipoLiquidacao tipoLiquidacaoAtendida() {
        return TipoLiquidacao.ONLINE;
    }

    /**
     * Hook: exige o número do cartão fictício.
     *
     * @param ctx contexto
     * @throws RegraNegocioModulo3Exception (HTTP 400) se o número não for informado
     */
    @Override
    protected void validarDadosPagamento(ContextoCheckout ctx) {
        String numero = ctx.requisicao.numeroCartaoFicticio();
        if (numero == null || numero.isBlank()) {
            throw new RegraNegocioModulo3Exception("Informe o número do cartão (fictício) para pagar com cartão de crédito.");
        }
    }

    /**
     * Confirma ou recusa o pagamento imediatamente, conforme a resposta da operadora.
     *
     * <p>Papel no Design Pattern: implementação da etapa abstrata do Template Method.</p>
     *
     * @param ctx contexto
     */
    @Override
    protected void tratarResultadoCobranca(ContextoCheckout ctx) {
        if (ctx.respostaCobranca.sucesso()) {
            servicoPagamento.aplicarConfirmacaoPagamento(ctx.pedido, ctx.respostaCobranca.identificadorGateway());
            // Webhook tardio da operadora sobre este pedido será respondido como repetição idempotente
            servicoPagamento.registrarConfirmacaoOnline(ctx.pedido);
        } else {
            mediador.coordenarRecusaPagamento(ctx.pedido, ctx.respostaCobranca.mensagem());
        }
    }

    /**
     * Hook: só esvazia o carrinho se o cartão foi aprovado.
     *
     * @param ctx contexto
     * @return {@code true} se a cobrança foi aprovada
     */
    @Override
    protected boolean deveEsvaziarCarrinho(ContextoCheckout ctx) {
        return ctx.respostaCobranca.sucesso();
    }
}
