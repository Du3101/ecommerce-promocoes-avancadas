package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import org.springframework.stereotype.Component;

/**
 * Especialização do checkout para meios de liquidação assíncrona (PIX e boleto).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Class</i> do Template Method. A cobrança só foi
 * emitida; o pedido passa para {@code AGUARDANDO_PAGAMENTO} e a reserva de estoque continua ativa até o webhook.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class ProcessadorCheckoutPagamentoAssincrono extends ProcessadorCheckoutTemplate {

    /**
     * Construtor com os recursos comuns.
     *
     * @param recursos colaboradores do checkout
     */
    public ProcessadorCheckoutPagamentoAssincrono(RecursosCheckout recursos) {
        super(recursos);
    }

    /**
     * Atende PIX e boleto.
     *
     * @return {@link TipoLiquidacao#ASSINCRONA}
     */
    @Override
    public TipoLiquidacao tipoLiquidacaoAtendida() {
        return TipoLiquidacao.ASSINCRONA;
    }

    /**
     * Coloca o pedido em espera pela confirmação do gateway.
     *
     * <p>Papel no Design Pattern: implementação da etapa abstrata do Template Method.</p>
     *
     * @param ctx contexto
     */
    @Override
    protected void tratarResultadoCobranca(ContextoCheckout ctx) {
        ctx.pedido.aguardarConfirmacaoPagamento();
    }
}
