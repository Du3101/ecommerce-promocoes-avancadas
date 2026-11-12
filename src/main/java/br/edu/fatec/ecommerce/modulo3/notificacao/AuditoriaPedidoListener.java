package br.edu.fatec.ecommerce.modulo3.notificacao;

import br.edu.fatec.ecommerce.modulo3.eventos.EventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.ObservadorEventoDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro técnico (log estruturado) de todos os eventos do ciclo de vida do pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Observer</i>. Mostra que o mesmo evento pode ter
 * consumidores com finalidades completamente diferentes (comunicação com o cliente vs. rastreabilidade técnica)
 * sem que o emissor saiba disso. A auditoria formal de cancelamentos (quem cancelou) é gravada pela porta de
 * auditoria do Módulo 1 dentro do próprio fluxo de cancelamento.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class AuditoriaPedidoListener implements ObservadorEventoDominio {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaPedidoListener.class);

    /**
     * Nome do canal.
     *
     * @return {@code "AUDITORIA"}
     */
    @Override
    public String nomeCanal() {
        return "AUDITORIA";
    }

    /**
     * A trilha técnica registra todos os eventos.
     *
     * @param evento evento publicado
     * @return sempre {@code true}
     */
    @Override
    public boolean interessadoEm(EventoDominio evento) {
        return true;
    }

    /**
     * Grava uma linha estruturada (chave=valor) com os dados do evento.
     *
     * <p>Papel no Design Pattern: {@code update()} do Observer.</p>
     *
     * @param evento evento publicado
     */
    @Override
    public void notificar(EventoDominio evento) {
        LOG.info("[AUDITORIA] evento={} eventoId={} pedido={} pedidoId={} ocorridoEm={}",
                evento.tipoEvento(), evento.eventoId(), evento.numeroPedido(), evento.pedidoId(), evento.ocorridoEm());
    }
}
