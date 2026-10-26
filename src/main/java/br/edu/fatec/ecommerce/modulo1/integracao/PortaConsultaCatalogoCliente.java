package br.edu.fatec.ecommerce.modulo1.integracao;

import java.util.UUID;

/**
 * Porta de leitura fornecida pelo Módulo 1 (Integrante 1) para consumo desacoplado
 * pelos Módulos 2 (Carrinho/Promoções) e 3 (Checkout/Frete) (FR-013).
 */
public interface PortaConsultaCatalogoCliente {

    /**
     * Consulta os dados consolidados e polimórficos de um item do catálogo
     * (seja {@code ProdutoVariacaoLeaf} ou {@code ComboKitComposite}).
     *
     * <p>Objetivo Funcional: Fornecer preço vigente, peso total em gramas, dimensões físicas e
     * disponibilidade em estoque para o carrinho, promoções e cálculo de frete.</p>
     * <p>Papel no Design Pattern: Porta de fronteira cliente do padrão <b>Composite</b>.</p>
     *
     * @param sku código SKU da variação ou do combo
     * @return DTO {@link VisaoItemCatalogoDTO}
     */
    VisaoItemCatalogoDTO consultarItemPorSku(String sku);

    /**
     * Verifica se determinado SKU pertence à categoria alvo ou a qualquer uma de suas
     * subcategorias descendentes (via padrão <b>Composite</b> de categorias).
     *
     * <p>Objetivo Funcional: Permitir ao motor de promoções (Módulo 2) avaliar regras como
     * "15% de desconto em toda a categoria Vestuário (incluindo subcategorias)".</p>
     * <p>Papel no Design Pattern: Consome {@code CategoriaComp.obterIdsSubarvoreRecursivo()}.</p>
     *
     * @param sku código SKU do item ou combo
     * @param categoriaAlvoId UUID da categoria alvo da promoção
     * @return {@code true} se a categoria principal do item pertencer à subárvore da categoria alvo
     */
    boolean itemPertenceACategoriaOuSubcategoria(String sku, UUID categoriaAlvoId);

    /**
     * Retorna os dados de elegibilidade do cliente e seu endereço selecionado
     * (ou representação segura via {@code EnderecoAusenteNullObject}).
     *
     * <p>Objetivo Funcional: Informar ao Módulo 2 (promoção de 1ª compra ou por região/UF) e ao
     * Módulo 3 (frete por CEP/UF) os dados do cliente sem risco de {@code NullPointerException}.</p>
     * <p>Papel no Design Pattern: Porta de fronteira apoiada pelo padrão <b>Null Object</b>.</p>
     *
     * @param clienteId UUID do cliente
     * @return DTO {@link VisaoClienteElegibilidadeDTO}
     */
    VisaoClienteElegibilidadeDTO consultarClienteParaCompra(UUID clienteId);
}

