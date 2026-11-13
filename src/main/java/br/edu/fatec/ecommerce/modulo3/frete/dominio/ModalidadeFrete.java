package br.edu.fatec.ecommerce.modulo3.frete.dominio;

/**
 * Modalidades de entrega oferecidas pela loja.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> chave de resolução dos adaptadores de frete (cada
 * {@code CalculadoraFretePort} declara qual modalidade atende) e metadado da política de frete grátis
 * (só a modalidade marcada como {@link #isEconomica() econômica} pode ser zerada).
 *
 * <p><b>Guia de extensão:</b> para uma nova transportadora (ex.: Loggi), adicione a constante aqui e crie o
 * adaptador correspondente; o {@code ServicoCalculoFrete} passa a cotá-la automaticamente.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum ModalidadeFrete {

    /** Correios PAC: menor custo, prazo maior. Única modalidade elegível ao frete grátis. */
    CORREIOS_PAC("Correios Econômico (PAC)", "Correios", true),

    /** Correios SEDEX: custo intermediário, prazo rápido. */
    CORREIOS_SEDEX("Correios Expresso (SEDEX)", "Correios", false),

    /** Transportadora privada expressa com seguro de carga. */
    TRANSPORTADORA_EXPRESSA("Transportadora Privada Expressa", "Transportadora Privada", false);

    private final String nomeExibicao;
    private final String transportadora;
    private final boolean economica;

    /**
     * Construtor do enum.
     *
     * @param nomeExibicao nome amigável exibido ao cliente
     * @param transportadora empresa responsável pela entrega
     * @param economica se é a modalidade econômica (alvo da política de frete grátis)
     */
    ModalidadeFrete(String nomeExibicao, String transportadora, boolean economica) {
        this.nomeExibicao = nomeExibicao;
        this.transportadora = transportadora;
        this.economica = economica;
    }

    /**
     * Retorna o nome amigável da modalidade.
     *
     * @return nome de exibição
     */
    public String getNomeExibicao() {
        return nomeExibicao;
    }

    /**
     * Retorna a transportadora responsável.
     *
     * @return nome da transportadora
     */
    public String getTransportadora() {
        return transportadora;
    }

    /**
     * Indica se é a modalidade econômica (a única que a política de frete grátis pode zerar).
     *
     * @return {@code true} para a modalidade econômica
     */
    public boolean isEconomica() {
        return economica;
    }
}
