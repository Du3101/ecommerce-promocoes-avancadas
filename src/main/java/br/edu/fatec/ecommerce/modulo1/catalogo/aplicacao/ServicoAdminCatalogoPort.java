package br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Interface de porta para todas as operações administrativas de escrita sobre o Catálogo
 * (Categorias, Produtos, Variações, Preços, Status e Combos/Kits) (FR-005 a FR-008, FR-012).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papel <i>Subject</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Tanto o serviço real ({@link ServicoAdminCatalogoReal}) quanto o proxy de
 * proteção e auditoria ({@code CatalogoAdminAuditoriaProxy}) implementam esta interface, permitindo
 * que controllers e serviços consumidores interajam com o Proxy de maneira transparente.</p>
 */
public interface ServicoAdminCatalogoPort {

    /**
     * Cria uma nova categoria raiz ou subcategoria vinculada a uma categoria pai existente.
     *
     * <p>Objetivo Funcional: Estruturar a árvore mercadológica de categorias (FR-007).</p>
     * <p>Papel no Design Pattern: Operação do <i>Subject</i> interceptada pelo <b>Proxy</b> para
     * controle de acesso e auditoria.</p>
     *
     * @param nome nome comercial da categoria
     * @param slugOpcional slug opcional
     * @param categoriaPaiIdOpcional UUID da categoria pai (ou {@code null} se for raiz)
     * @return categoria criada e persistida
     */
    CategoriaComp criarCategoria(String nome, String slugOpcional, UUID categoriaPaiIdOpcional);

    /**
     * Move ou vincula uma subcategoria existente para dentro de uma categoria pai, validando ausência de ciclos.
     *
     * <p>Objetivo Funcional: Reorganizar a hierarquia de categorias com proteção anti-ciclo (FR-007).</p>
     * <p>Papel no Design Pattern: Operação do <i>Subject</i> interceptada pelo <b>Proxy</b>.</p>
     *
     * @param categoriaPaiId UUID da categoria pai destino
     * @param subcategoriaId UUID da subcategoria filha
     * @return categoria pai atualizada
     */
    CategoriaComp vincularSubcategoria(UUID categoriaPaiId, UUID subcategoriaId);

    /**
     * Cadastra um novo produto e suas variações comercializáveis inicializando seus registros de estoque.
     *
     * <p>Objetivo Funcional: Incluir produto físico completo no catálogo (FR-005, FR-006).</p>
     * <p>Papel no Design Pattern: Operação do <i>Subject</i> que utiliza {@link ProdutoBuilder} (<b>Builder</b>)
     * e {@code CriadorProdutoPadrao} (<b>Factory Method</b>), interceptada pelo <b>Proxy</b>.</p>
     *
     * @param builder builder configurado com o produto e suas variações
     * @param estoqueInicialPorSku mapa opcional com saldo inicial de estoque para cada SKU
     * @return produto persistido
     */
    Produto cadastrarProdutoComVariacoes(ProdutoBuilder builder, Map<String, Integer> estoqueInicialPorSku);

    /**
     * Cadastra um novo Combo/Kit promocional composto por itens existentes no catálogo.
     *
     * <p>Objetivo Funcional: Criar pacote comercial composto recursivamente (FR-008).</p>
     * <p>Papel no Design Pattern: Operação do <i>Subject</i> que utiliza {@code ComboKitBuilder}
     * (<b>Builder</b>), {@code CriadorComboKitPromocional} (<b>Factory Method</b>) e
     * {@link ComboKitComposite} (<b>Composite</b>), interceptada pelo <b>Proxy</b>.</p>
     *
     * @param skuCombo código SKU único do combo
     * @param nomeCombo nome comercial do combo
     * @param descricao descrição comercial
     * @param categoriaId UUID da categoria principal
     * @param modoPreco modo de precificação ({@link ModoPrecoCombo})
     * @param percentualDescontoKit percentual de desconto (para soma recursiva)
     * @param precoFixoOpcional preço fixo dedicado (para preço fixo)
     * @param quantidadesPorSkuComponente mapa de SKUs filhos e suas quantidades por kit
     * @return combo criado e persistido
     */
    ComboKitComposite cadastrarComboKitPromocional(
        String skuCombo,
        String nomeCombo,
        String descricao,
        UUID categoriaId,
        ModoPrecoCombo modoPreco,
        BigDecimal percentualDescontoKit,
        BigDecimal precoFixoOpcional,
        Map<String, Integer> quantidadesPorSkuComponente
    );

    /**
     * Altera o preço vigente de uma variação unitária ou o parâmetro de preço de um combo promocional.
     *
     * <p>Objetivo Funcional: Atualizar preços no catálogo com captura automática de valor anterior e
     * valor novo na trilha de auditoria (FR-005, FR-008, FR-012).</p>
     * <p>Papel no Design Pattern: Operação crítica do <i>Subject</i> auditada pelo <b>Proxy</b>.</p>
     *
     * @param sku código SKU da variação ou do combo
     * @param novoValor novo preço final (para variação/combo fixo) ou novo percentual de desconto (para combo recursivo)
     * @return item comercializável atualizado
     */
    ItemCatalogoComponent alterarPrecoItemCatalogo(String sku, BigDecimal novoValor);

    /**
     * Ativa ou inativa logicamente um item comercializável (variação ou combo) no catálogo.
     *
     * <p>Objetivo Funcional: Controlar disponibilidade comercial sem exclusão física de registros (FR-005, FR-012).</p>
     * <p>Papel no Design Pattern: Operação administrativa do <i>Subject</i> auditada pelo <b>Proxy</b>.</p>
     *
     * @param sku código SKU da variação ou combo
     * @param ativo {@code true} para ativar; {@code false} para inativar
     * @return item comercializável atualizado
     */
    ItemCatalogoComponent alterarStatusAtivacaoItemCatalogo(String sku, boolean ativo);
}

