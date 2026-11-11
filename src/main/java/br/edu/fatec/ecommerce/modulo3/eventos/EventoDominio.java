package br.edu.fatec.ecommerce.modulo3.eventos;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface base para eventos de domínio emitidos no ciclo de vida do e-commerce.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Contrato base do padrão <b>Observer</b> (Publish/Subscribe) para comunicação desacoplada
 * entre o agregado raiz e subsistemas de notificação, auditoria e logística.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface EventoDominio {

    /**
     * Retorna o identificador único universal do evento.
     *
     * @return UUID do evento.
     */
    UUID getEventoId();

    /**
     * Retorna o momento exato em que o evento ocorreu.
     *
     * @return Instant do disparo.
     */
    Instant getOcorridoEm();
}
