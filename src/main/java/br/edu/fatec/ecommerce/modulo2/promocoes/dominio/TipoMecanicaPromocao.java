package br.edu.fatec.ecommerce.modulo2.promocoes.dominio;

/**
 * Enumeração das quatro modalidades comerciais distintas de promoção suportadas pelo sistema.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atende ao requisito da Constituição de suportar no mínimo 4 naturezas distintas de promoções
 * e serve de chave de identificação no padrão <i>Dynamic Factory</i>
 * ({@code FabricaDinamicaEstrategiaPromocao}), permitindo selecionar a estratégia polimórfica
 * adequada em tempo de execução sem switches engessados.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public enum TipoMecanicaPromocao {

    /**
     * Modalidade 1: Leve X unidades e Pague apenas Y unidades de produtos elegíveis.
     * Exemplo: Leve 3 Camisetas e Pague 2 (a de menor valor sai grátis).
     */
    LEVE_X_PAGUE_Y("Leve X e Pague Y"),

    /**
     * Modalidade 2: Desconto Percentual Condicional.
     * Exemplo: 15% de desconto para primeira compra de clientes ou em determinada categoria.
     */
    PERCENTUAL_CONDICIONAL("Desconto Percentual Condicional"),

    /**
     * Modalidade 3: Desconto em Valor Monetário Fixo por Meta de Carrinho.
     * Exemplo: R$ 50,00 de desconto caso o subtotal atinja ou supere R$ 300,00.
     */
    VALOR_FIXO_META_CARRINHO("Desconto Fixo por Meta de Carrinho"),

    /**
     * Modalidade 4: Desconto em Combo Cruzado / Venda Casada.
     * Exemplo: Na compra conjunta do Produto A e Produto B, concede 20% de desconto no par casado.
     */
    COMBO_CRUZADO_CASADO("Combo Cruzado / Venda Casada");

    private final String descricaoExibicao;

    /**
     * Construtor do enum.
     *
     * <p>Objetivo funcional: Inicializa o enum com o rótulo legível para relatórios e exibição.
     * <p>Papel no padrão: Metadado semântico para a estratégia promocional.
     *
     * @param descricaoExibicao Rótulo legível para humanos.
     */
    TipoMecanicaPromocao(String descricaoExibicao) {
        this.descricaoExibicao = descricaoExibicao;
    }

    /**
     * Retorna a descrição legível da mecânica promocional.
     *
     * <p>Objetivo funcional: Obter nome legível para relatórios e exibição no painel administrativo.
     * <p>Papel no padrão: Exposição de metadados da modalidade.
     *
     * @return String contendo a descrição legível da promoção.
     */
    public String getDescricaoExibicao() {
        return descricaoExibicao;
    }
}

