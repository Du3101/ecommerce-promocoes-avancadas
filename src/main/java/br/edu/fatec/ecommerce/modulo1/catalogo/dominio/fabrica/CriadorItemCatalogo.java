package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.fabrica;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import java.math.BigDecimal;

/**
 * Classe abstrata criadora que define o contrato para instanciação polimórfica de itens
 * comercializáveis do catálogo ({@link ItemCatalogoComponent}) (FR-005, FR-008).
 *
 * <p>Aplicação do Padrão GoF: <b>Factory Method</b> — papel <i>Creator</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Desacopla o fluxo de validação pós-criação de itens comercializáveis da classe
 * concreta instanciada ({@code ProdutoVariacaoLeaf} via {@link CriadorProdutoPadrao} ou
 * {@code ComboKitComposite} via {@code CriadorComboKitPromocional}), permitindo introduzir novos
 * tipos de itens no catálogo sem modificar o código cliente.</p>
 *
 * @param <E> tipo da especificação/builder utilizado pela subclasse concreta
 */
public abstract class CriadorItemCatalogo<E> {

    /**
     * Construtor padrão explícito do Creator abstrato.
     *
     * <p>Objetivo Funcional: Permitir herança pelas fábricas concretas de itens do catálogo.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>Creator</i> abstrato do padrão <b>Factory Method</b>.</p>
     */
    protected CriadorItemCatalogo() {
        // Construtor base do Creator
    }

    /**
     * Operação de alto nível do Creator que delega a criação ao Factory Method {@link #criarItemCatalogo(Object)}
     * e aplica as invariantes universais de qualquer {@link ItemCatalogoComponent}.
     *
     * <p>Objetivo Funcional: Garantir que qualquer item fabricado (folha ou combo) possua SKU válido,
     * categoria vinculada, preço vigente positivo e peso físico positivo antes de ingressar no catálogo.</p>
     * <p>Papel no Design Pattern: Operação cliente interna do <i>Creator</i> que invoca o
     * <b>Factory Method</b> polimórfico.</p>
     *
     * @param especificacao objeto de especificação/builder da família do item
     * @return instância validada de {@link ItemCatalogoComponent}
     */
    public final ItemCatalogoComponent fabricarItemValidado(E especificacao) {
        if (especificacao == null) {
            throw new RegraNegocioModulo1Exception(
                "A especificação para criação do item de catálogo não pode ser nula."
            );
        }
        ItemCatalogoComponent itemCriado = criarItemCatalogo(especificacao);
        validarInvariantesComerciaisDoItem(itemCriado);
        return itemCriado;
    }

    /**
     * Factory Method abstrato sobrescrito pelas subclasses concretas para instanciar o tipo específico
     * de {@link ItemCatalogoComponent}.
     *
     * <p>Objetivo Funcional: Delegar a decisão de qual classe concreta instanciar para as subclasses.</p>
     * <p>Papel no Design Pattern: O próprio <b>Factory Method</b> ({@code factoryMethod()}) GoF.</p>
     *
     * @param especificacao especificação ou builder concreto
     * @return nova instância de {@link ItemCatalogoComponent}
     */
    protected abstract ItemCatalogoComponent criarItemCatalogo(E especificacao);

    /**
     * Verifica as invariantes comerciais comuns a todos os itens comercializáveis do catálogo.
     *
     * <p>Objetivo Funcional: Impedir que qualquer fábrica concreta retorne item sem SKU, sem categoria,
     * com preço menor/igual a zero ou peso menor/igual a zero.</p>
     * <p>Papel no Design Pattern: Pós-condição garantida pelo <i>Creator</i> do <b>Factory Method</b>.</p>
     *
     * @param item item recém-criado pelo Factory Method
     * @throws RegraNegocioModulo1Exception se alguma invariante universal for violada
     */
    protected void validarInvariantesComerciaisDoItem(ItemCatalogoComponent item) {
        if (item == null) {
            throw new RegraNegocioModulo1Exception("O Factory Method não pode retornar um item nulo.");
        }
        if (item.obterSku() == null || item.obterSku().isBlank()) {
            throw new RegraNegocioModulo1Exception("Todo item do catálogo deve possuir um SKU válido.");
        }
        if (item.obterCategoriaPrincipalId() == null) {
            throw new RegraNegocioModulo1Exception(
                "O item '" + item.obterSku() + "' deve possuir uma categoria principal vinculada."
            );
        }
        if (item.calcularPrecoVigente() == null
            || item.calcularPrecoVigente().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O preço vigente do item '" + item.obterSku() + "' deve ser estritamente maior que zero."
            );
        }
        if (item.obterPesoTotalGramas() <= 0 || item.obterDimensoesFisicas() == null) {
            throw new RegraNegocioModulo1Exception(
                "O item '" + item.obterSku() + "' deve possuir peso e dimensões físicas maiores que zero."
            );
        }
    }
}

