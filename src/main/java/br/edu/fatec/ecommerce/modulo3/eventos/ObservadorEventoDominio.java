package br.edu.fatec.ecommerce.modulo3.eventos;

/**
 * Contrato de um ouvinte (observador) de eventos de domínio do pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> interface <i>Observer</i> do padrão GoF Observer.
 * Cada canal de reação (e-mail, SMS, auditoria, futuramente WhatsApp ou ERP) é uma implementação independente.
 *
 * <p><b>Guia de extensão:</b> para um novo canal, crie uma classe {@code @Component} que implemente esta
 * interface. O Spring injeta todas as implementações no {@link BarramentoEventosDominio} automaticamente;
 * nenhuma classe existente precisa mudar (Princípio Aberto-Fechado).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface ObservadorEventoDominio {

    /**
     * Nome do canal, usado em logs (ex.: "E-MAIL", "SMS").
     *
     * @return nome do canal
     */
    String nomeCanal();

    /**
     * Indica se este observador quer receber o evento.
     *
     * <p>Papel no Design Pattern: filtro de interesse do Observer (assinatura seletiva por tipo de evento).</p>
     *
     * @param evento evento publicado
     * @return {@code true} se o observador deve ser notificado
     */
    boolean interessadoEm(EventoDominio evento);

    /**
     * Reage ao evento.
     *
     * <p>Papel no Design Pattern: método {@code update()} do Observer.</p>
     *
     * @param evento evento publicado
     */
    void notificar(EventoDominio evento);
}
