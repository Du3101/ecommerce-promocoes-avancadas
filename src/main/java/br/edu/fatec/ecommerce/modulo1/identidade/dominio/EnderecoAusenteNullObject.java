package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

/**
 * Implementação polimórfica que representa a ausência de um endereço de entrega selecionado (FR-004).
 *
 * <p><b>Design Pattern Aplicado: Null Object / Missing Object</b></p>
 * <ul>
 *   <li><b>1. Qual problema de projeto existia?</b> Um cliente recém-cadastrado (ou que removeu seu
 *   endereço anterior) ainda não possui endereço selecionado para entrega. Retornar {@code null} em
 *   {@code cliente.obterEnderecoSelecionado()} obrigaria todos os módulos (Perfil, Carrinho,
 *   Elegibilidade Regional de Promoções e Cálculo de Frete) a espalhar {@code if (endereco != null)},
 *   sob risco constante de {@link NullPointerException}.</li>
 *   <li><b>2. Por que este padrão foi escolhido?</b> O padrão Null Object / Missing Object fornece um
 *   objeto substituto imutável que implementa {@link OpcaoEnderecoEntrega} com comportamento neutro,
 *   seguro e previsível.</li>
 *   <li><b>3. O que ficou mais simples de modificar?</b> Os consumidores nos Módulos 1, 2 e 3 podem
 *   invocar {@code obterResumoFormatado()}, {@code obterUfRegiao()} e {@code possuiEnderecoValido()}
 *   diretamente sem checagens de nulidade.</li>
 * </ul>
 */
public final class EnderecoAusenteNullObject implements OpcaoEnderecoEntrega {

    /**
     * Instância única imutável e compartilhada do objeto nulo de endereço.
     */
    private static final EnderecoAusenteNullObject INSTANCIA = new EnderecoAusenteNullObject();

    /**
     * Construtor privado explícito para evitar criação repetida de objetos sem estado.
     *
     * <p>Objetivo Funcional: Garantir reuso da instância neutra.</p>
     * <p>Papel no Design Pattern: Construtor encapsulado do <b>Null Object</b>.</p>
     */
    private EnderecoAusenteNullObject() {
        // Sem estado mutável
    }

    /**
     * Obtém a instância compartilhada que representa um endereço ausente/não selecionado.
     *
     * <p>Objetivo Funcional: Retornar o objeto neutro sempre que o cliente não tiver endereço ativo.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> de acesso ao <b>Null Object</b>.</p>
     *
     * @return instância imutável de {@link EnderecoAusenteNullObject}
     */
    public static EnderecoAusenteNullObject obterInstancia() {
        return INSTANCIA;
    }

    /**
     * Informa explicitamente que não há endereço físico válido para envio.
     *
     * <p>Objetivo Funcional: Sinalizar ao checkout que um endereço real ainda precisa ser selecionado.</p>
     * <p>Papel no Design Pattern: Comportamento neutro falso do <b>Null Object</b>.</p>
     *
     * @return sempre {@code false}
     */
    @Override
    public boolean possuiEnderecoValido() {
        return false;
    }

    /**
     * Retorna CEP vazio de forma segura sem referência nula.
     *
     * <p>Objetivo Funcional: Evitar {@link NullPointerException} em consultas de CEP.</p>
     * <p>Papel no Design Pattern: Retorno neutro do <b>Null Object</b>.</p>
     *
     * @return string vazia {@code ""}
     */
    @Override
    public String obterCep() {
        return "";
    }

    /**
     * Retorna UF/Região vazia de forma segura sem referência nula.
     *
     * <p>Objetivo Funcional: Permitir que avaliadores de elegibilidade regional consultem a UF com segurança.</p>
     * <p>Papel no Design Pattern: Retorno neutro do <b>Null Object</b>.</p>
     *
     * @return string vazia {@code ""}
     */
    @Override
    public String obterUfRegiao() {
        return "";
    }

    /**
     * Retorna mensagem padrão amigável indicando que nenhum endereço foi selecionado.
     *
     * <p>Objetivo Funcional: Exibir mensagem clara na consulta do perfil do cliente.</p>
     * <p>Papel no Design Pattern: Comportamento padrão descritivo do <b>Null Object</b>.</p>
     *
     * @return mensagem {@code "Nenhum endereço de entrega selecionado"}
     */
    @Override
    public String obterResumoFormatado() {
        return "Nenhum endereço de entrega selecionado";
    }
}

