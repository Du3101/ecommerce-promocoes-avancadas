package br.edu.fatec.ecommerce.modulo3.notificacao;

import br.edu.fatec.ecommerce.modulo3.eventos.EventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.ObservadorEventoDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de notificação por e-mail (simulado em log).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Observer</i>. Recebe todos os eventos do pedido e
 * "envia" um e-mail ao cliente com a saudação e o resumo do evento. Em produção, este seria o único ponto a trocar
 * por um cliente SMTP real.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class NotificacaoEmailListener implements ObservadorEventoDominio {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoEmailListener.class);

    /**
     * Nome do canal.
     *
     * @return {@code "E-MAIL"}
     */
    @Override
    public String nomeCanal() {
        return "E-MAIL";
    }

    /**
     * O e-mail acompanha todos os marcos do pedido.
     *
     * @param evento evento publicado
     * @return sempre {@code true}
     */
    @Override
    public boolean interessadoEm(EventoDominio evento) {
        return true;
    }

    /**
     * Simula o envio do e-mail.
     *
     * <p>Papel no Design Pattern: {@code update()} do Observer.</p>
     *
     * @param evento evento publicado
     */
    @Override
    public void notificar(EventoDominio evento) {
        LOG.info("[E-MAIL] Para: {} | Assunto: Pedido {} - {} | Olá, {}! {}",
                evento.emailCliente(), evento.numeroPedido(), evento.tipoEvento(),
                evento.nomeCliente(), evento.resumo());
    }
}
