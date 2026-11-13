package br.edu.fatec.ecommerce.modulo3.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.modulo3.notificacao.AuditoriaPedidoListener;
import br.edu.fatec.ecommerce.modulo3.notificacao.NotificacaoEmailListener;
import br.edu.fatec.ecommerce.modulo3.notificacao.NotificacaoSmsListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários do barramento de eventos (padrão Observer) (T033).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class BarramentoEventosObserverTest {

    /**
     * Observador de teste que grava os eventos recebidos.
     */
    private static final class ObservadorGravador implements ObservadorEventoDominio {
        private final List<EventoDominio> recebidos = new ArrayList<>();

        @Override
        public String nomeCanal() {
            return "GRAVADOR";
        }

        @Override
        public boolean interessadoEm(EventoDominio evento) {
            return true;
        }

        @Override
        public void notificar(EventoDominio evento) {
            recebidos.add(evento);
        }
    }

    /**
     * Observador que sempre falha, para provar o isolamento de falhas.
     */
    private static final class ObservadorDefeituoso implements ObservadorEventoDominio {
        @Override
        public String nomeCanal() {
            return "DEFEITUOSO";
        }

        @Override
        public boolean interessadoEm(EventoDominio evento) {
            return true;
        }

        @Override
        public void notificar(EventoDominio evento) {
            throw new IllegalStateException("Servidor de e-mail fora do ar");
        }
    }

    private static PedidoCriadoEvent eventoCriado() {
        return new PedidoCriadoEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "PED-1",
                "c@t.com", "Cliente", "PIX", new BigDecimal("520.00"), "Pague com PIX");
    }

    private static PagamentoConfirmadoEvent eventoPago() {
        return new PagamentoConfirmadoEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "PED-1",
                "c@t.com", "Cliente", "PIX-1", new BigDecimal("520.00"));
    }

    @Test
    @DisplayName("Publica para todos os observadores inscritos (e-mail, SMS seletivo, auditoria e um inscrito em tempo de execução)")
    void devePublicarParaTodosOsObservadoresInteressados() {
        BarramentoEventosDominio barramento = new BarramentoEventosDominio(List.of(
                new NotificacaoEmailListener(), new NotificacaoSmsListener(), new AuditoriaPedidoListener()));
        ObservadorGravador gravador = new ObservadorGravador();
        barramento.inscrever(gravador);

        int notificadosCriado = barramento.publicar(eventoCriado());
        int notificadosPago = barramento.publicar(eventoPago());

        // PedidoCriado: e-mail + auditoria + gravador (SMS não se interessa)
        assertEquals(3, notificadosCriado);
        // PagamentoConfirmado: e-mail + SMS + auditoria + gravador
        assertEquals(4, notificadosPago);
        assertEquals(2, gravador.recebidos.size());
        assertEquals("PagamentoConfirmadoEvent", gravador.recebidos.get(1).tipoEvento());
    }

    @Test
    @DisplayName("SMS só se interessa por pagamento confirmado e pedido enviado")
    void smsDeveSerSeletivo() {
        NotificacaoSmsListener sms = new NotificacaoSmsListener();
        assertFalse(sms.interessadoEm(eventoCriado()));
        assertTrue(sms.interessadoEm(eventoPago()));
    }

    @Test
    @DisplayName("Falha de um observador não impede os demais (resiliência)")
    void falhaDeUmObservadorNaoDeveAfetarOsOutros() {
        ObservadorGravador gravador = new ObservadorGravador();
        BarramentoEventosDominio barramento = new BarramentoEventosDominio(List.of(new ObservadorDefeituoso(), gravador));

        int notificados = barramento.publicar(eventoCriado());

        assertEquals(1, notificados);
        assertEquals(1, gravador.recebidos.size());
    }

    @Test
    @DisplayName("Observador com inscrição cancelada não recebe mais eventos")
    void observadorRemovidoNaoDeveReceberEventos() {
        ObservadorGravador gravador = new ObservadorGravador();
        BarramentoEventosDominio barramento = new BarramentoEventosDominio(List.of());
        barramento.inscrever(gravador);
        barramento.cancelarInscricao(gravador);

        barramento.publicar(eventoCriado());

        assertTrue(gravador.recebidos.isEmpty());
        assertTrue(barramento.listarObservadores().isEmpty());
    }
}
