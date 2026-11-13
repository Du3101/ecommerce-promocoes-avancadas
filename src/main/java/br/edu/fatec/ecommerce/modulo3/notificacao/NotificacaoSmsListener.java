package br.edu.fatec.ecommerce.modulo3.notificacao;

import br.edu.fatec.ecommerce.modulo3.eventos.EventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.ObservadorEventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.PagamentoConfirmadoEvent;
import br.edu.fatec.ecommerce.modulo3.eventos.PedidoEnviadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de notificação por SMS (simulado em log).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Observer</i> com assinatura seletiva: SMS custa
 * dinheiro, então só é enviado nos dois momentos que o cliente mais espera — pagamento confirmado e pedido enviado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class NotificacaoSmsListener implements ObservadorEventoDominio {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoSmsListener.class);

    /**
     * Nome do canal.
     *
     * @return {@code "SMS"}
     */
    @Override
    public String nomeCanal() {
        return "SMS";
    }

    /**
     * Interessa-se apenas por pagamento confirmado e pedido enviado.
     *
     * @param evento evento publicado
     * @return {@code true} para {@link PagamentoConfirmadoEvent} e {@link PedidoEnviadoEvent}
     */
    @Override
    public boolean interessadoEm(EventoDominio evento) {
        return evento instanceof PagamentoConfirmadoEvent || evento instanceof PedidoEnviadoEvent;
    }

    /**
     * Simula o envio do SMS com texto curto.
     *
     * <p>Papel no Design Pattern: {@code update()} do Observer.</p>
     *
     * @param evento evento publicado
     */
    @Override
    public void notificar(EventoDominio evento) {
        LOG.info("[SMS] Cliente {} | FATEC Store: {}", evento.emailCliente(), evento.resumo());
    }
}
