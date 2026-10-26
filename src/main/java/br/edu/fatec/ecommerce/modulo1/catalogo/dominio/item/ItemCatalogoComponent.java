package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Interface comum para qualquer item comercializável no catálogo (seja uma variação unitária física
 * {@link ProdutoVariacaoLeaf} ou um pacote composto {@code ComboKitComposite}) (FR-005, FR-008).
 *
 * <p>Aplicação do Padrão GoF: <b>Composite</b> — papel <i>Component</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Permite que o catálogo público, o carrinho de compras, o motor de promoções (Módulo 2)
 * e o checkout/frete (Módulo 3) consultem preço vigente, peso total em gramas, dimensões físicas agregadas
 * e disponibilidade máxima em estoque de maneira uniforme, sem diferenciar item unitário de combo.</p>
 */
public interface ItemCatalogoComponent {

    /**
     * Obtém o identificador único do item comercializável.
     *
     * <p>Objetivo Funcional: Identificar o item internamente no domínio.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i> no padrão <b>Composite</b>.</p>
     *
     * @return UUID do item
     */
    UUID obterId();

    /**
     * Obtém o código SKU único do item comercializável ou do combo.
     *
     * <p>Objetivo Funcional: Identificar univocamente o item em buscas, carrinhos e reservas.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return código SKU em maiúsculas
     */
    String obterSku();

    /**
     * Obtém o nome comercial completo de exibição do item ou kit.
     *
     * <p>Objetivo Funcional: Apresentar o nome do item na vitrine e nos pedidos.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return nome de exibição
     */
    String obterNomeExibicao();

    /**
     * Calcula o preço unitário vigente de venda do item ou combo em reais (BRL).
     *
     * <p>Objetivo Funcional: Retornar o preço final da variação ({@code precoBase + adicionalPreco})
     * ou o preço calculado/fixo do combo promocional.</p>
     * <p>Papel no Design Pattern: Operação polimórfica central do padrão <b>Composite</b>.</p>
     *
     * @return preço vigente com 2 casas decimais ({@code > 0.00})
     */
    BigDecimal calcularPrecoVigente();

    /**
     * Obtém o peso físico total em gramas do item unitário ou a soma recursiva ponderada do combo.
     *
     * <p>Objetivo Funcional: Fornecer o peso total exato para cálculo de frete no Módulo 3.</p>
     * <p>Papel no Design Pattern: Operação recursiva do padrão <b>Composite</b>.</p>
     *
     * @return peso total em gramas ({@code > 0})
     */
    int obterPesoTotalGramas();

    /**
     * Obtém as dimensões físicas do item unitário ou as dimensões consolidadas do combo.
     *
     * <p>Objetivo Funcional: Fornecer altura, largura, profundidade e peso para cubagem de entrega.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return Value Object {@link DimensoesFisicas}
     */
    DimensoesFisicas obterDimensoesFisicas();

    /**
     * Obtém o identificador da categoria principal à qual o item ou combo está vinculado.
     *
     * <p>Objetivo Funcional: Permitir avaliação de elegibilidade de promoções por categoria.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return UUID da categoria principal
     */
    UUID obterCategoriaPrincipalId();

    /**
     * Indica se o item comercializável (e todos os seus componentes obrigatórios, no caso de combo) está ativo.
     *
     * <p>Objetivo Funcional: Impedir venda de itens ou combos inativados.</p>
     * <p>Papel no Design Pattern: Predicado recursivo de estado no padrão <b>Composite</b>.</p>
     *
     * @return {@code true} se ativo para comercialização; {@code false} caso contrário
     */
    boolean estaAtivo();

    /**
     * Indica se este item é um pacote composto ({@code ComboKitComposite}) ou uma folha ({@link ProdutoVariacaoLeaf}).
     *
     * <p>Objetivo Funcional: Informar aos módulos consumidores a natureza estrutural do item.</p>
     * <p>Papel no Design Pattern: Discriminador explícito entre <i>Leaf</i> e <i>Composite</i>.</p>
     *
     * @return {@code true} se for um combo/kit; {@code false} se for variação física unitária
     */
    boolean eComboKit();

    /**
     * Calcula a quantidade máxima disponível para compra imediata consultando os saldos físicos por SKU.
     *
     * <p>Objetivo Funcional: Para folhas, retorna o saldo disponível do próprio SKU; para combos,
     * calcula recursivamente $\min(\lfloor \text{disponivel}_i / \text{qtdNoKit}_i \rfloor)$ (FR-008).</p>
     * <p>Papel no Design Pattern: Operação recursiva de disponibilidade do padrão <b>Composite</b>.</p>
     *
     * @param consultorSaldoDisponivelPorSku função que retorna o saldo disponível para um dado SKU folha
     * @return quantidade inteira máxima disponível ({@code >= 0})
     */
    int calcularDisponibilidadeMaxima(Function<String, Integer> consultorSaldoDisponivelPorSku);

    /**
     * Expande recursivamente os SKUs físicos folha necessários para compor 1 unidade deste item do catálogo.
     *
     * <p>Objetivo Funcional: Permitir que a reserva atômica de estoque baixe proporcionalmente todos
     * os SKUs físicos que integram um combo/kit (inclusive combos aninhados).</p>
     * <p>Papel no Design Pattern: Decomposição recursiva de folhas do padrão <b>Composite</b>.</p>
     *
     * @return mapa associando cada SKU físico folha à quantidade necessária por unidade deste item
     */
    Map<String, Integer> expandirSkusFisicosComMultiplicador();

    /**
     * Decompõe recursivamente os SKUs físicos folha necessários para atender uma quantidade solicitada deste item.
     *
     * <p>Objetivo Funcional: Calcular o total exato de unidades físicas por SKU folha para reservar no estoque.</p>
     * <p>Papel no Design Pattern: Operação recursiva parametrizada do padrão <b>Composite</b>.</p>
     *
     * @param quantidadeSolicitada quantidade de unidades deste item ou combo desejada ({@code >= 1})
     * @return mapa associando cada SKU folha à quantidade física total a reservar
     */
    Map<String, Integer> decomporSkusFisicosParaReserva(int quantidadeSolicitada);
}
