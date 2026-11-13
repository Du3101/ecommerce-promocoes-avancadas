package br.edu.fatec.ecommerce.modulo3.eventos;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato comum de todos os eventos de domínio do ciclo de vida do pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * É a "mensagem" do padrão <b>Observer</b>. O emissor (serviços de checkout, pagamento, cancelamento e
 * expedição) publica um {@code EventoDominio} no {@link BarramentoEventosDominio}; os observadores
 * ({@link ObservadorEventoDominio}) recebem o evento sem que o emissor saiba quem são eles.
 *
 * <p>Todos os eventos são {@code record}s imutáveis: depois de publicados, nenhum ouvinte consegue alterá-los.
 *
 * <p><b>Guia de extensão:</b> para um novo marco do pedido (ex.: {@code PedidoEntregueEvent}), crie um
 * {@code record} que implemente esta interface e publique-o no ponto do fluxo correspondente.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface EventoDominio {

    /**
     * Identificador único da ocorrência do evento.
     *
     * @return UUID do evento
     */
    UUID eventoId();

    /**
     * Instante em que o fato de negócio aconteceu.
     *
     * @return instante da ocorrência
     */
    Instant ocorridoEm();

    /**
     * Pedido ao qual o evento se refere.
     *
     * @return UUID do pedido
     */
    UUID pedidoId();

    /**
     * Número comercial do pedido (exibido ao cliente).
     *
     * @return número do pedido
     */
    String numeroPedido();

    /**
     * E-mail do cliente, destinatário das notificações.
     *
     * @return e-mail do cliente
     */
    String emailCliente();

    /**
     * Nome do cliente, usado na saudação das notificações.
     *
     * @return nome do cliente
     */
    String nomeCliente();

    /**
     * Texto curto e legível descrevendo o fato, usado em logs e notificações.
     *
     * <p>Papel no Design Pattern: cada evento concreto define sua própria descrição (polimorfismo), então os
     * observadores não precisam de {@code instanceof} para montar uma mensagem genérica.</p>
     *
     * @return resumo do evento
     */
    String resumo();

    /**
     * Nome do tipo do evento (nome simples da classe).
     *
     * @return tipo do evento, ex.: {@code PedidoCriadoEvent}
     */
    default String tipoEvento() {
        return getClass().getSimpleName();
    }
}
