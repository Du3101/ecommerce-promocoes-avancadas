package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

/**
 * Contrato polimórfico que representa uma opção de endereço de entrega do cliente.
 *
 * <p><b>Design Pattern Aplicado: Null Object / Missing Object (Interface Polimórfica)</b></p>
 * <p>Permite que clientes com endereço válido selecionado ({@link Endereco}) e clientes que ainda
 * não selecionaram endereço ({@link EnderecoAusenteNullObject}) respondam à mesma interface sem
 * retornar {@code null} nem exigir verificações defensivas {@code if (endereco != null)}.</p>
 */
public interface OpcaoEnderecoEntrega {

    /**
     * Indica se esta opção representa um endereço físico real e completo para envio da mercadoria.
     *
     * <p>Objetivo Funcional: Informar ao carrinho e ao checkout se o cliente já escolheu onde receber o pedido.</p>
     * <p>Papel no Design Pattern: Método discriminador polimórfico do padrão <b>Null Object</b>.</p>
     *
     * @return {@code true} para {@link Endereco} real; {@code false} para {@link EnderecoAusenteNullObject}
     */
    boolean possuiEnderecoValido();

    /**
     * Obtém o CEP de destino (8 dígitos numéricos) ou string vazia caso ausente.
     *
     * <p>Objetivo Funcional: Fornecer o CEP para cálculo de frete no Módulo 3.</p>
     * <p>Papel no Design Pattern: Operação polimórfica segura contra nulidade no <b>Null Object</b>.</p>
     *
     * @return CEP de 8 dígitos ou string vazia {@code ""}
     */
    String obterCep();

    /**
     * Obtém a sigla da Unidade Federativa (UF/Estado) ou string vazia caso ausente.
     *
     * <p>Objetivo Funcional: Fornecer a região para condições de elegibilidade de promoções (Módulo 2)
     * e cálculo de frete regional (Módulo 3).</p>
     * <p>Papel no Design Pattern: Operação polimórfica segura no <b>Null Object</b>.</p>
     *
     * @return sigla da UF de 2 letras (ex.: "SP") ou string vazia {@code ""}
     */
    String obterUfRegiao();

    /**
     * Obtém um resumo textual formatado do endereço para exibição ao cliente ou gravação no pedido.
     *
     * <p>Objetivo Funcional: Apresentar o endereço completo ou a mensagem neutra de ausência.</p>
     * <p>Papel no Design Pattern: Comportamento padrão neutro no <b>Null Object</b>.</p>
     *
     * @return descrição formatada do endereço
     */
    String obterResumoFormatado();

    /**
     * Obtém o CEP de destino (alias semântico para {@link #obterCep()}).
     *
     * <p>Objetivo Funcional: Fornecer o CEP de destino para frete e contratos de integração.</p>
     * <p>Papel no Design Pattern: Método polimórfico do padrão <b>Null Object</b>.</p>
     *
     * @return CEP de 8 dígitos ou string vazia
     */
    default String obterCepDestino() {
        return obterCep();
    }

    /**
     * Obtém a UF de destino (alias semântico para {@link #obterUfRegiao()}).
     *
     * <p>Objetivo Funcional: Fornecer a sigla da UF para elegibilidade regional e frete.</p>
     * <p>Papel no Design Pattern: Método polimórfico do padrão <b>Null Object</b>.</p>
     *
     * @return sigla da UF ou string vazia
     */
    default String obterUfDestino() {
        return obterUfRegiao();
    }
}

