package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.fabrica;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import org.springframework.stereotype.Component;

/**
 * Criador concreto especializado na fabricação de produtos físicos padrão e suas folhas comercializáveis
 * ({@link ProdutoVariacaoLeaf}) (FR-005).
 *
 * <p>Aplicação do Padrão GoF: <b>Factory Method</b> — papel <i>ConcreteCreator</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Encapsula a transformação de um {@link ProdutoBuilder} em instâncias validadas de
 * {@link Produto} e {@link ProdutoVariacaoLeaf}, aplicando as invariantes universais de
 * {@link CriadorItemCatalogo} sobre todas as variações do produto.</p>
 */
@Component
public class CriadorProdutoPadrao extends CriadorItemCatalogo<ProdutoBuilder> {

    /**
     * Construtor padrão explícito do criador concreto de produtos padrão.
     *
     * <p>Objetivo Funcional: Disponibilizar o componente Spring de fabricação de produtos.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>ConcreteCreator</i> do padrão <b>Factory Method</b>.</p>
     */
    public CriadorProdutoPadrao() {
        super();
    }

    /**
     * Implementação concreta do Factory Method que constrói o produto e retorna sua variação principal
     * como {@link ItemCatalogoComponent}.
     *
     * <p>Objetivo Funcional: Atender ao contrato polimórfico de {@link CriadorItemCatalogo} validando
     * todas as variações do produto construído.</p>
     * <p>Papel no Design Pattern: Sobrescrita concreta do <b>Factory Method</b>.</p>
     *
     * @param builder instância configurada de {@link ProdutoBuilder}
     * @return primeira variação {@link ProdutoVariacaoLeaf} do produto como {@link ItemCatalogoComponent}
     */
    @Override
    protected ItemCatalogoComponent criarItemCatalogo(ProdutoBuilder builder) {
        Produto produto = fabricarProdutoCompleto(builder);
        return produto.obterVariacoes().get(0);
    }

    /**
     * Fabrica o agregado {@link Produto} completo e valida cada uma de suas folhas {@link ProdutoVariacaoLeaf}
     * contra as invariantes comerciais do {@link CriadorItemCatalogo}.
     *
     * <p>Objetivo Funcional: Entregar o agregado {@link Produto} pronto para persistência no catálogo.</p>
     * <p>Papel no Design Pattern: Operação especializada do <i>ConcreteCreator</i> que combina o
     * <b>Builder</b> ({@link ProdutoBuilder}) com as pós-condições do <b>Factory Method</b>.</p>
     *
     * @param builder builder configurado com dados do produto e variações
     * @return instância validada de {@link Produto}
     */
    public Produto fabricarProdutoCompleto(ProdutoBuilder builder) {
        Produto produto = builder.construir();
        for (ProdutoVariacaoLeaf variacao : produto.obterVariacoes()) {
            validarInvariantesComerciaisDoItem(variacao);
        }
        return produto;
    }
}

