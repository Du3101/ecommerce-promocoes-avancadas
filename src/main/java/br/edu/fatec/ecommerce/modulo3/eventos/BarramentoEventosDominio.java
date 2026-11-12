package br.edu.fatec.ecommerce.modulo3.eventos;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Barramento de eventos de domínio do Módulo 3.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Subject</i> do padrão GoF <b>Observer</b>. Mantém a lista
 * de observadores, permite inscrever/cancelar inscrição em tempo de execução e notifica todos os interessados
 * quando um evento é publicado.
 *
 * <p><b>Problema resolvido:</b> sem o barramento, o serviço de pagamento teria de chamar diretamente o serviço
 * de e-mail, o de SMS e o de auditoria — e cada novo canal exigiria alterar o fluxo de pagamento.
 *
 * <p><b>Decisões de projeto:</b>
 * <ul>
 *   <li><b>Resiliência</b>: a falha de um observador é registrada em log e não impede os demais nem desfaz a
 *       transação de negócio (uma notificação que falhou não pode cancelar um pagamento confirmado).</li>
 *   <li><b>Entrega síncrona</b>: os observadores rodam na mesma thread, o que torna a ordem previsível e os
 *       testes determinísticos. Para produção, bastaria anotar os observadores com {@code @Async}.</li>
 * </ul>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class BarramentoEventosDominio {

    private static final Logger LOG = LoggerFactory.getLogger(BarramentoEventosDominio.class);

    private final List<ObservadorEventoDominio> observadores = new CopyOnWriteArrayList<>();

    /**
     * Cria o barramento já inscrevendo todos os observadores registrados no Spring.
     *
     * <p>Papel no Design Pattern: injeção dinâmica — o Spring entrega todos os beans que implementam
     * {@link ObservadorEventoDominio}, sem que o barramento conheça as classes concretas.</p>
     *
     * @param observadoresRegistrados observadores descobertos pelo Spring
     */
    public BarramentoEventosDominio(List<ObservadorEventoDominio> observadoresRegistrados) {
        if (observadoresRegistrados != null) {
            this.observadores.addAll(observadoresRegistrados);
        }
    }

    /**
     * Inscreve um novo observador em tempo de execução.
     *
     * <p>Papel no Design Pattern: operação {@code attach()} do Subject.</p>
     *
     * @param observador observador a inscrever (ignorado se já estiver inscrito)
     */
    public void inscrever(ObservadorEventoDominio observador) {
        Objects.requireNonNull(observador, "Observador não pode ser nulo.");
        if (!this.observadores.contains(observador)) {
            this.observadores.add(observador);
        }
    }

    /**
     * Remove um observador.
     *
     * <p>Papel no Design Pattern: operação {@code detach()} do Subject.</p>
     *
     * @param observador observador a remover
     */
    public void cancelarInscricao(ObservadorEventoDominio observador) {
        this.observadores.remove(observador);
    }

    /**
     * Publica um evento para todos os observadores interessados.
     *
     * <p>Papel no Design Pattern: operação {@code notifyObservers()} do Subject.</p>
     *
     * @param evento evento de domínio imutável
     * @return quantidade de observadores notificados com sucesso
     */
    public int publicar(EventoDominio evento) {
        Objects.requireNonNull(evento, "Evento não pode ser nulo.");
        LOG.info("[BARRAMENTO] Publicando {} do pedido {}", evento.tipoEvento(), evento.numeroPedido());

        int notificadosComSucesso = 0;
        for (ObservadorEventoDominio observador : this.observadores) {
            // 1. Assinatura seletiva: cada observador decide se quer o evento
            if (!observador.interessadoEm(evento)) {
                continue;
            }
            // 2. Isolamento de falhas: um observador com erro não afeta os demais
            try {
                observador.notificar(evento);
                notificadosComSucesso++;
            } catch (RuntimeException erro) {
                LOG.error("[BARRAMENTO] Falha no canal {} ao processar {} do pedido {}: {}",
                        observador.nomeCanal(), evento.tipoEvento(), evento.numeroPedido(), erro.getMessage());
            }
        }
        return notificadosComSucesso;
    }

    /**
     * Lista os observadores inscritos (somente leitura).
     *
     * @return cópia imutável da lista de observadores
     */
    public List<ObservadorEventoDominio> listarObservadores() {
        return List.copyOf(this.observadores);
    }
}
