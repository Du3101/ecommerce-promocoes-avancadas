package br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemComponenteKit;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.CategoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.ComboKitJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.ProdutoJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.VariacaoProdutoJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação dedicado às consultas públicas e internas do Catálogo de Produtos,
 * Categorias Recursivas e Combos/Kits Promocionais (FR-005, FR-006, FR-007, FR-008).
 *
 * <p>Consome de forma polimórfica os dois usos do padrão <b>Composite</b> no módulo:
 * {@link CategoriaComp} (árvore mercadológica) e {@link ItemCatalogoComponent}
 * ({@link ProdutoVariacaoLeaf} e {@link ComboKitComposite}).</p>
 */
@Service
public class ServicoConsultaCatalogo {

    private final CategoriaJpaRepository categoriaRepository;
    private final ProdutoJpaRepository produtoRepository;
    private final VariacaoProdutoJpaRepository variacaoRepository;
    private final ComboKitJpaRepository comboRepository;
    private final ItemEstoqueJpaRepository estoqueRepository;

    /**
     * Construtor com injeção explícita dos repositórios de catálogo e estoque.
     *
     * <p>Objetivo Funcional: Inicializar o serviço de leitura do catálogo.</p>
     * <p>Papel no Design Pattern: Camada de aplicação cliente dos padrões <b>Composite</b>,
     * <b>Null Object</b> e <b>Flyweight</b>.</p>
     *
     * @param categoriaRepository repositório de categorias
     * @param produtoRepository repositório de produtos
     * @param variacaoRepository repositório de variações folha
     * @param comboRepository repositório de combos/kits compostos
     * @param estoqueRepository repositório de saldos de estoque
     */
    public ServicoConsultaCatalogo(
        CategoriaJpaRepository categoriaRepository,
        ProdutoJpaRepository produtoRepository,
        VariacaoProdutoJpaRepository variacaoRepository,
        ComboKitJpaRepository comboRepository,
        ItemEstoqueJpaRepository estoqueRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.variacaoRepository = variacaoRepository;
        this.comboRepository = comboRepository;
        this.estoqueRepository = estoqueRepository;
    }

    /**
     * Lista todas as categorias de nível raiz com suas subárvores de subcategorias recursivas (FR-007).
     *
     * <p>Objetivo Funcional: Expor a árvore completa de categorias na vitrine da loja.</p>
     * <p>Papel no Design Pattern: Retorna os nós de topo do padrão <b>Composite</b> ({@link CategoriaComp}).</p>
     *
     * @return lista de categorias raiz
     */
    @Transactional(readOnly = true)
    public List<CategoriaComp> listarArvoreCategorias() {
        return this.categoriaRepository.findByCategoriaPaiPersistenciaIsNull();
    }

    /**
     * Busca uma categoria específica pelo seu identificador único.
     *
     * <p>Objetivo Funcional: Recuperar uma categoria e sua subárvore para navegação ou validação.</p>
     * <p>Papel no Design Pattern: Consulta de nó do padrão <b>Composite</b>.</p>
     *
     * @param categoriaId UUID da categoria
     * @return entidade {@link CategoriaComp}
     * @throws RecursoNaoEncontradoException se a categoria não existir
     */
    @Transactional(readOnly = true)
    public CategoriaComp obterCategoriaPorId(UUID categoriaId) {
        return this.categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada com o ID: " + categoriaId
            ));
    }

    /**
     * Coleta recursivamente os UUIDs de uma categoria e de todas as suas subcategorias descendentes ativas.
     *
     * <p>Objetivo Funcional: Permitir filtro recursivo de produtos e elegibilidade de promoções por
     * categoria pai (FR-007, FR-013).</p>
     * <p>Papel no Design Pattern: Invoca a operação recursiva {@link CategoriaComp#obterIdsSubarvoreRecursivo()}
     * do padrão <b>Composite</b>.</p>
     *
     * @param categoriaId UUID da categoria raiz da busca
     * @return conjunto de UUIDs da subárvore
     */
    @Transactional(readOnly = true)
    public Set<UUID> obterIdsSubarvoreCategoria(UUID categoriaId) {
        CategoriaComp categoria = obterCategoriaPorId(categoriaId);
        return categoria.obterIdsSubarvoreRecursivo();
    }

    /**
     * Busca polimorficamente qualquer item comercializável ({@link ProdutoVariacaoLeaf} ou
     * {@link ComboKitComposite}) pelo seu código SKU único.
     *
     * <p>Objetivo Funcional: Fornecer visão uniforme de preço, peso, dimensões e estoque sem que o
     * chamador precise saber se o SKU é uma variação física ou um combo promocional.</p>
     * <p>Papel no Design Pattern: Ponto de resolução polimórfica do <i>Component</i>
     * ({@link ItemCatalogoComponent}) no padrão <b>Composite</b>.</p>
     *
     * @param sku código SKU do item ou combo
     * @return instância resolvida de {@link ItemCatalogoComponent}
     * @throws RecursoNaoEncontradoException se nenhum item ou combo possuir o SKU informado
     */
    @Transactional(readOnly = true)
    public ItemCatalogoComponent buscarItemComercializavelPorSku(String sku) {
        return buscarItemComercializavelPorSkuOpcional(sku)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Nenhum item comercializável (variação ou combo) encontrado para o SKU: " + sku
            ));
    }

    /**
     * Busca opcionalmente um item comercializável (variação ou combo) pelo SKU sem lançar exceção
     * quando ausente, evitando marcação de rollback em transações chamadoras.
     *
     * <p>Objetivo Funcional: Resolver polimorficamente um SKU retornando {@link Optional}.</p>
     * <p>Papel no Design Pattern: Consulta segura do <i>Component</i> no padrão <b>Composite</b>.</p>
     *
     * @param sku código SKU pesquisado
     * @return {@link Optional} contendo o {@link ItemCatalogoComponent} se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<ItemCatalogoComponent> buscarItemComercializavelPorSkuOpcional(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        String skuNormalizado = sku.trim();

        Optional<ProdutoVariacaoLeaf> variacao = this.variacaoRepository.findBySkuIgnoreCase(skuNormalizado);
        if (variacao.isPresent()) {
            return Optional.of(variacao.get());
        }

        Optional<ComboKitComposite> combo = this.comboRepository.findBySkuIgnoreCase(skuNormalizado);
        if (combo.isPresent()) {
            ComboKitComposite comboHidratado = combo.get();
            hidratarComponentesDoComboRecursivamente(comboHidratado);
            return Optional.of(comboHidratado);
        }

        return Optional.empty();
    }

    /**
     * Lista todos os itens comercializáveis ativos do catálogo, filtrando opcionalmente por uma
     * categoria pai (incluindo recursivamente os itens de todas as suas subcategorias filhas).
     *
     * <p>Objetivo Funcional: Atender à vitrine pública unificando produtos simples e combos e
     * respeitando a hierarquia recursiva de categorias (FR-007, FR-008).</p>
     * <p>Papel no Design Pattern: Combina a navegação recursiva de {@link CategoriaComp} com a
     * lista polimórfica de {@link ItemCatalogoComponent} (ambos <b>Composite</b>).</p>
     *
     * @param categoriaIdOpcional UUID opcional da categoria para filtro recursivo
     * @return lista polimórfica de {@link ItemCatalogoComponent} ativos
     */
    @Transactional(readOnly = true)
    public List<ItemCatalogoComponent> listarItensComercializaveisAtivos(UUID categoriaIdOpcional) {
        List<Produto> produtos;
        List<ComboKitComposite> combos;

        if (categoriaIdOpcional != null) {
            Set<UUID> idsSubarvore = obterIdsSubarvoreCategoria(categoriaIdOpcional);
            produtos = this.produtoRepository.findByCategoriaPrincipalIdIn(idsSubarvore);
            combos = this.comboRepository.findByCategoriaPrincipalIdIn(idsSubarvore);
        } else {
            produtos = this.produtoRepository.findAll();
            combos = this.comboRepository.findAll();
        }

        List<ItemCatalogoComponent> resultado = new ArrayList<>();
        for (Produto produto : produtos) {
            if (produto.estaAtivo()) {
                for (ProdutoVariacaoLeaf variacao : produto.obterVariacoes()) {
                    if (variacao.estaAtivo()) {
                        resultado.add(variacao);
                    }
                }
            }
        }

        for (ComboKitComposite combo : combos) {
            hidratarComponentesDoComboRecursivamente(combo);
            if (combo.estaAtivo()) {
                resultado.add(combo);
            }
        }

        return resultado;
    }

    /**
     * Calcula a disponibilidade máxima para venda imediata de qualquer {@link ItemCatalogoComponent}.
     *
     * <p>Objetivo Funcional: Retornar o saldo disponível direto para folhas ou o cálculo recursivo
     * pelo item gargalo ($\min(\lfloor \text{disponivel}_i / \text{qtdNoKit}_i \rfloor)$) para combos.</p>
     * <p>Papel no Design Pattern: Invoca {@link ItemCatalogoComponent#calcularDisponibilidadeMaxima}
     * injetando a função de consulta ao repositório de estoque.</p>
     *
     * @param item item comercializável (folha ou combo)
     * @return quantidade máxima disponível ({@code >= 0})
     */
    @Transactional(readOnly = true)
    public int calcularDisponibilidadeEstoque(ItemCatalogoComponent item) {
        return item.calcularDisponibilidadeMaxima(this::obterSaldoDisponivelFolhaPorSku);
    }

    /**
     * Consulta o saldo físico disponível de um único SKU folha no repositório de estoque.
     *
     * <p>Objetivo Funcional: Fornecer o caso base de saldo para variações e combos.</p>
     * <p>Papel no Design Pattern: Função provedora de saldo para o cálculo recursivo do <b>Composite</b>.</p>
     *
     * @param skuFolha código SKU de uma variação física
     * @return saldo disponível ({@code 0} se ainda não houver registro de estoque)
     */
    @Transactional(readOnly = true)
    public int obterSaldoDisponivelFolhaPorSku(String skuFolha) {
        return this.estoqueRepository.findBySkuIgnoreCase(skuFolha)
            .map(ItemEstoque::obterQuantidadeDisponivel)
            .orElse(0);
    }

    /**
     * Hidrata recursivamente as referências em memória dos componentes filhos de um {@link ComboKitComposite}
     * carregado a partir do banco de dados JPA.
     *
     * <p>Objetivo Funcional: Garantir que todos os nós filhos (folhas ou sub-combos) estejam conectados
     * em memória antes da execução dos cálculos recursivos.</p>
     * <p>Papel no Design Pattern: Reconstrução da árvore de objetos do padrão <b>Composite</b>.</p>
     *
     * @param combo instância de {@link ComboKitComposite} carregada do banco
     */
    public void hidratarComponentesDoComboRecursivamente(ComboKitComposite combo) {
        for (ItemComponenteKit vinculo : combo.obterComponentes()) {
            ItemCatalogoComponent filho = buscarItemComercializavelPorSku(vinculo.obterComponenteSku());
            vinculo.vincularComponenteResolvido(filho);
        }
    }
}

