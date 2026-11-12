package br.edu.fatec.ecommerce.modulo2.integracao;

import java.util.UUID;

/**
 * Porta Java desacoplada exposta pelo Módulo 2 (Eduardo Juncker Toras)
 * para consumo pelo Módulo 3 (Checkout, Ciclo de Vida de Pedidos e Frete).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a <i>Interface Segregation</i> e a fronteira explícita entre módulos (Constituição, Princípio V).
 * Permite que o fluxo de fechamento de pedido no Módulo 3 consulte o carrinho com seu preço
 * promocional congelado, confirme o consumo de cupons e solicite a limpeza do carrinho sem
 * conhecer as tabelas internas nem os repositórios JPA do Módulo 2.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface PortaConsultaCarrinhoPromocoes {

    /**
     * Obtém o resumo consolidado do carrinho de um cliente ou sessão para início do checkout.
     *
     * @param clienteId UUID do cliente (se autenticado).
     * @param sessaoId String da sessão (se visitante).
     * @return ResumoCarrinhoCheckoutDTO contendo SKUs para reserva, peso e valores monetários.
     */
    ResumoCarrinhoCheckoutDTO obterResumoCarrinhoParaCheckout(UUID clienteId, String sessaoId);

    /**
     * Esvazia o carrinho de compras após a conclusão bem-sucedida do checkout no Módulo 3.
     *
     * @param clienteId UUID do cliente.
     * @param sessaoId String da sessão.
     */
    void esvaziarCarrinhoAposCheckout(UUID clienteId, String sessaoId);

    /**
     * Confirma definitivamente o consumo da cota de uso de um cupom aplicado após aprovação do pedido.
     *
     * @param codigoCupom Código do cupom utilizado.
     */
    void confirmarConsumoDeCupom(String codigoCupom);

    /**
     * Resolve o identificador de dono de carrinho usado pelo Módulo 2 para um usuário autenticado.
     *
     * <p>Objetivo Funcional: O carrinho de um usuário logado é indexado por um UUID derivado do seu e-mail
     * (regra interna do Módulo 2). O Módulo 3 recebe apenas o e-mail do Spring Security e usa este método
     * para obter a chave correta antes de chamar {@link #obterResumoCarrinhoParaCheckout(UUID, String)} e
     * {@link #esvaziarCarrinhoAposCheckout(UUID, String)}, sem replicar a regra de derivação.</p>
     * <p>Papel no Design Pattern: Porta de fronteira (Anti-Corruption) que encapsula a política de
     * identificação de carrinhos do Módulo 2.</p>
     *
     * @param emailUsuario e-mail do usuário autenticado.
     * @return UUID usado como {@code clienteId} do carrinho no Módulo 2.
     */
    UUID resolverIdentificadorCarrinhoPorEmail(String emailUsuario);
}

