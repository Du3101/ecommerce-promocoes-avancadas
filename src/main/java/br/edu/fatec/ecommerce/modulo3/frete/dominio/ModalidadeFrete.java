package br.edu.fatec.ecommerce.modulo3.frete.dominio;

/**
 * Modalidades de transporte e entrega suportadas no cálculo logístico.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Chave discriminadora utilizada pelo adaptador (GoF Adapter) do subsistema de frete
 * para selecionar a calculadora logística específica (Correios PAC, Correios SEDEX ou Transportadora).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum ModalidadeFrete {

    /**
     * Encomenda econômica padrão dos Correios simulados.
     */
    CORREIOS_PAC("Correios Econômico (PAC)", "Correios"),

    /**
     * Encomenda expressa prioritária dos Correios simulados.
     */
    CORREIOS_SEDEX("Correios Expresso (SEDEX)", "Correios"),

    /**
     * Transporte rodoviário privado com seguro de carga e entrega expressa.
     */
    TRANSPORTADORA_EXPRESSA("Transportadora Privada Expressa", "Transportadora Privada");

    private final String nomeExibicao;
    private final String transportadora;

    /**
     * Construtor explícito do enum com nome legível e empresa prestadora.
     *
     * @param nomeExibicao Nome descritivo da modalidade.
     * @param transportadora Nome da empresa responsável pelo frete.
     */
    ModalidadeFrete(String nomeExibicao, String transportadora) {
        this.nomeExibicao = nomeExibicao;
        this.transportadora = transportadora;
    }

    /**
     * Retorna o nome amigável para exibição ao cliente.
     *
     * @return Nome descritivo da opção de frete.
     */
    public String getNomeExibicao() {
        return nomeExibicao;
    }

    /**
     * Retorna a transportadora responsável.
     *
     * @return Nome da transportadora.
     */
    public String getTransportadora() {
        return transportadora;
    }
}
