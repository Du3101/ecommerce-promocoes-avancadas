package br.edu.fatec.ecommerce.modulo3.pagamento.dominio;

/**
 * Enumeração dos métodos de pagamento suportados pelo subsistema financeiro.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Serve como chave discriminadora utilizada pelo <i>Service Locator</i> e pela
 * <i>Abstract Factory</i> para resolver dinamicamente o adaptador (GoF Adapter) correspondente
 * à modalidade escolhida no momento do checkout.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum MetodoPagamento {

    /**
     * Pagamento instantâneo via arranjo PIX com chave e QR Code copia-e-cola.
     */
    PIX("PIX Instantâneo"),

    /**
     * Pagamento via cartão de crédito fictício com autorização online simulada.
     */
    CARTAO_CREDITO("Cartão de Crédito"),

    /**
     * Pagamento via boleto bancário simulado com código de barras e linha digitável.
     */
    BOLETO("Boleto Bancário");

    private final String descricaoExibicao;

    /**
     * Construtor explícito do enum com sua descrição formatada para exibição.
     *
     * @param descricaoExibicao Rótulo legível para visualização na API e relatórios.
     */
    MetodoPagamento(String descricaoExibicao) {
        this.descricaoExibicao = descricaoExibicao;
    }

    /**
     * Retorna a descrição legível para exibição ao usuário final.
     *
     * @return Descrição amigável do meio de pagamento.
     */
    public String getDescricaoExibicao() {
        return descricaoExibicao;
    }
}
