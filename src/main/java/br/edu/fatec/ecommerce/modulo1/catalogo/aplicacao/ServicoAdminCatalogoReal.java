package br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.ConflitoDeEstadoOuDuplicidadeException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.fabrica.CriadorComboKitPromocional;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.fabrica.CriadorProdutoPadrao;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.CategoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.ComboKitJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.ProdutoJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.VariacaoProdutoJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação real das operações administrativas do Catálogo (FR-005 a FR-008).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papel <i>RealSubject</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Concentra exclusivamente a lógica de negócio e persistência de categorias,
 * produtos, variações e combos, enquanto a verificação de permissões RBAC e a gravação da trilha
 * de auditoria ficam desacopladas no {@code CatalogoAdminAuditoriaProxy}.</p>
 */
@Service("servicoAdminCatalogoReal")
public class ServicoAdminCatalogoReal implements ServicoAdminCatalogoPort {

    private final CategoriaJpaRepository categoriaRepository;
    private final ProdutoJpaRepository produtoRepository;
    private final VariacaoProdutoJpaRepository variacaoRepository;
    private final ComboKitJpaRepository comboRepository;
    private final ItemEstoqueJpaRepository estoqueRepository;
    private final CriadorProdutoPadrao criadorProdutoPadrao;
    private final CriadorComboKitPromocional criadorComboKitPromocional;
    private final ServicoConsultaCatalogo servicoConsultaCatalogo;

    /**
     * Construtor com injeção explícita das dependências de domínio e infraestrutura.
     *
     * <p>Objetivo Funcional: Inicializar o sujeito real de administração do catálogo.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>RealSubject</i> do padrão <b>Proxy</b>.</p>
     */
    public ServicoAdminCatalogoReal(
        CategoriaJpaRepository categoriaRepository,
        ProdutoJpaRepository produtoRepository,
        VariacaoProdutoJpaRepository variacaoRepository,
        ComboKitJpaRepository comboRepository,
        ItemEstoqueJpaRepository estoqueRepository,
        CriadorProdutoPadrao criadorProdutoPadrao,
        CriadorComboKitPromocional criadorComboKitPromocional,
        ServicoConsultaCatalogo servicoConsultaCatalogo
    ) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.variacaoRepository = variacaoRepository;
        this.comboRepository = comboRepository;
        this.estoqueRepository = estoqueRepository;
        this.criadorProdutoPadrao = criadorProdutoPadrao;
        this.criadorComboKitPromocional = criadorComboKitPromocional;
        this.servicoConsultaCatalogo = servicoConsultaCatalogo;
    }

    /**
     * Cria uma nova categoria ou subcategoria validando unicidade de slug e ausência de ciclos.
     *
     * <p>Objetivo Funcional: Persistir nó na árvore {@link CategoriaComp} (FR-007).</p>
     * <p>Papel no Design Pattern: Execução real no <i>RealSubject</i> delegada pelo <b>Proxy</b>.</p>
     *
     * @param nome nome comercial da categoria
     * @param slugOpcional slug opcional
     * @param categoriaPaiIdOpcional UUID da categoria pai (se for subcategoria)
     * @return categoria criada
     */
    @Override
    @Transactional
    public CategoriaComp criarCategoria(String nome, String slugOpcional, UUID categoriaPaiIdOpcional) {
        CategoriaComp novaCategoria = new CategoriaComp(nome, slugOpcional);
        if (this.categoriaRepository.existsBySlugIgnoreCase(novaCategoria.obterSlug())) {
            throw new ConflitoDeEstadoOuDuplicidadeException(
                "Já existe uma categoria cadastrada com o slug: " + novaCategoria.obterSlug()
            );
        }

        if (categoriaPaiIdOpcional != null) {
            CategoriaComp pai = this.categoriaRepository.findById(categoriaPaiIdOpcional)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Categoria pai não encontrada com ID: " + categoriaPaiIdOpcional
                ));
            pai.adicionarSubcategoria(novaCategoria);
            this.categoriaRepository.save(pai);
            return novaCategoria;
        }

        return this.categoriaRepository.save(novaCategoria);
    }

    /**
     * Vincula uma subcategoria existente a uma categoria pai existente, validando que não há ciclo hierárquico.
     *
     * <p>Objetivo Funcional: Permitir reorganização da árvore de categorias bloqueando ciclos.</p>
     * <p>Papel no Design Pattern: Invoca {@link CategoriaComp#adicionarSubcategoria} (<b>Composite</b>).</p>
     *
     * @param categoriaPaiId UUID da categoria pai
     * @param subcategoriaId UUID da subcategoria
     * @return categoria pai atualizada
     */
    @Override
    @Transactional
    public CategoriaComp vincularSubcategoria(UUID categoriaPaiId, UUID subcategoriaId) {
        CategoriaComp pai = this.categoriaRepository.findById(categoriaPaiId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria pai não encontrada: " + categoriaPaiId
            ));
        CategoriaComp filha = this.categoriaRepository.findById(subcategoriaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Subcategoria não encontrada: " + subcategoriaId
            ));

        pai.adicionarSubcategoria(filha);
        return this.categoriaRepository.save(pai);
    }

    /**
     * Fabrica um novo {@link Produto} via {@link CriadorProdutoPadrao} e inicializa os saldos de estoque
     * de suas variações {@link ProdutoVariacaoLeaf}.
     *
     * <p>Objetivo Funcional: Cadastrar produto e SKUs físicos garantindo unicidade global de SKU.</p>
     * <p>Papel no Design Pattern: Execução do <i>RealSubject</i> combinando <b>Builder</b> e
     * <b>Factory Method</b>.</p>
     *
     * @param builder builder configurado do produto
     * @param estoqueInicialPorSku mapa opcional de saldo inicial por SKU
     * @return produto persistido
     */
    @Override
    @Transactional
    public Produto cadastrarProdutoComVariacoes(
        ProdutoBuilder builder,
        Map<String, Integer> estoqueInicialPorSku
    ) {
        Produto produto = this.criadorProdutoPadrao.fabricarProdutoCompleto(builder);

        if (!this.categoriaRepository.existsById(produto.obterCategoriaPrincipalId())) {
            throw new RecursoNaoEncontradoException(
                "A categoria informada para o produto não existe: " + produto.obterCategoriaPrincipalId()
            );
        }

        for (ProdutoVariacaoLeaf variacao : produto.obterVariacoes()) {
            validarSkuGlobalmenteInedito(variacao.obterSku());
        }

        Produto produtoSalvo = this.produtoRepository.save(produto);

        for (ProdutoVariacaoLeaf variacao : produtoSalvo.obterVariacoes()) {
            int saldoInicial = (estoqueInicialPorSku != null
                && estoqueInicialPorSku.containsKey(variacao.obterSku()))
                ? estoqueInicialPorSku.get(variacao.obterSku())
                : 0;
            ItemEstoque posicaoEstoque = new ItemEstoque(variacao.obterSku(), saldoInicial);
            this.estoqueRepository.save(posicaoEstoque);
        }

        return produtoSalvo;
    }

    /**
     * Cadastra um novo {@link ComboKitComposite} resolvendo seus componentes filhos e validando
     * ausência de ciclos recursivos.
     *
     * <p>Objetivo Funcional: Criar pacote comercial composto por variações e/ou outros combos (FR-008).</p>
     * <p>Papel no Design Pattern: Execução do <i>RealSubject</i> integrando <b>Composite</b>,
     * <b>Builder</b> ({@link ComboKitBuilder}) e <b>Factory Method</b> ({@link CriadorComboKitPromocional}).</p>
     *
     * @param skuCombo código SKU único do combo
     * @param nomeCombo nome comercial do combo
     * @param descricao descrição comercial
     * @param categoriaId UUID da categoria principal
     * @param modoPreco modo de precificação ({@link ModoPrecoCombo})
     * @param percentualDescontoKit percentual de desconto para soma recursiva
     * @param precoFixoOpcional preço fixo para modo fixo dedicado
     * @param quantidadesPorSkuComponente mapa de SKUs filhos e suas quantidades no kit
     * @return combo criado e persistido
     */
    @Override
    @Transactional
    public ComboKitComposite cadastrarComboKitPromocional(
        String skuCombo,
        String nomeCombo,
        String descricao,
        UUID categoriaId,
        ModoPrecoCombo modoPreco,
        BigDecimal percentualDescontoKit,
        BigDecimal precoFixoOpcional,
        Map<String, Integer> quantidadesPorSkuComponente
    ) {
        if (skuCombo == null || skuCombo.isBlank()) {
            throw new RegraNegocioModulo1Exception("O código SKU do combo é obrigatório.");
        }
        validarSkuGlobalmenteInedito(skuCombo.trim());

        if (!this.categoriaRepository.existsById(categoriaId)) {
            throw new RecursoNaoEncontradoException(
                "A categoria informada para o combo não existe: " + categoriaId
            );
        }
        if (quantidadesPorSkuComponente == null || quantidadesPorSkuComponente.isEmpty()) {
            throw new RegraNegocioModulo1Exception("O combo deve informar seus componentes filhos.");
        }

        ComboKitBuilder builder = new ComboKitBuilder()
            .comSku(skuCombo)
            .comNome(nomeCombo)
            .comDescricao(descricao)
            .naCategoria(categoriaId)
            .comConfiguracaoPreco(modoPreco, percentualDescontoKit, precoFixoOpcional);

        for (Map.Entry<String, Integer> entrada : quantidadesPorSkuComponente.entrySet()) {
            ItemCatalogoComponent componenteFilho =
                this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(entrada.getKey());
            builder.adicionarItemComponente(componenteFilho, entrada.getValue());
        }

        ComboKitComposite comboValidado = this.criadorComboKitPromocional.fabricarComboKitValidado(builder);
        ComboKitComposite comboSalvo = this.comboRepository.save(comboValidado);
        this.servicoConsultaCatalogo.hidratarComponentesDoComboRecursivamente(comboSalvo);
        return comboSalvo;
    }

    /**
     * Altera o preço final de uma variação física ({@link ProdutoVariacaoLeaf}) ou o parâmetro de
     * preço de um combo ({@link ComboKitComposite}).
     *
     * <p>Objetivo Funcional: Atualizar preços comerciais no catálogo.</p>
     * <p>Papel no Design Pattern: Operação do <i>RealSubject</i> envolvida pelo <b>Proxy</b> de auditoria.</p>
     *
     * @param sku código SKU do item ou combo
     * @param novoValor novo preço final ou novo percentual de desconto
     * @return item atualizado
     */
    @Override
    @Transactional
    public ItemCatalogoComponent alterarPrecoItemCatalogo(String sku, BigDecimal novoValor) {
        Optional<ProdutoVariacaoLeaf> variacaoOpt = this.variacaoRepository.findBySkuIgnoreCase(sku);
        if (variacaoOpt.isPresent()) {
            ProdutoVariacaoLeaf variacao = variacaoOpt.get();
            variacao.definirNovoPrecoFinal(novoValor);
            return this.variacaoRepository.save(variacao);
        }

        Optional<ComboKitComposite> comboOpt = this.comboRepository.findBySkuIgnoreCase(sku);
        if (comboOpt.isPresent()) {
            ComboKitComposite combo = comboOpt.get();
            this.servicoConsultaCatalogo.hidratarComponentesDoComboRecursivamente(combo);
            combo.atualizarParametroPreco(novoValor);
            return this.comboRepository.save(combo);
        }

        throw new RecursoNaoEncontradoException("Item de catálogo não encontrado para o SKU: " + sku);
    }

    /**
     * Ativa ou inativa logicamente uma variação física ou um combo promocional pelo SKU.
     *
     * <p>Objetivo Funcional: Controlar visibilidade e permissão de venda do item sem apagá-lo.</p>
     * <p>Papel no Design Pattern: Operação do <i>RealSubject</i> envolvida pelo <b>Proxy</b> de auditoria.</p>
     *
     * @param sku código SKU da variação ou combo
     * @param ativo novo status de ativação
     * @return item atualizado
     */
    @Override
    @Transactional
    public ItemCatalogoComponent alterarStatusAtivacaoItemCatalogo(String sku, boolean ativo) {
        Optional<ProdutoVariacaoLeaf> variacaoOpt = this.variacaoRepository.findBySkuIgnoreCase(sku);
        if (variacaoOpt.isPresent()) {
            ProdutoVariacaoLeaf variacao = variacaoOpt.get();
            variacao.definirAtivo(ativo);
            return this.variacaoRepository.save(variacao);
        }

        Optional<ComboKitComposite> comboOpt = this.comboRepository.findBySkuIgnoreCase(sku);
        if (comboOpt.isPresent()) {
            ComboKitComposite combo = comboOpt.get();
            this.servicoConsultaCatalogo.hidratarComponentesDoComboRecursivamente(combo);
            combo.definirAtivo(ativo);
            return this.comboRepository.save(combo);
        }

        throw new RecursoNaoEncontradoException("Item de catálogo não encontrado para o SKU: " + sku);
    }

    /**
     * Garante que um código SKU ainda não esteja em uso nem por variações nem por combos.
     *
     * <p>Objetivo Funcional: Impedir colisão de SKUs no catálogo unificado.</p>
     * <p>Papel no Design Pattern: Validação interna de integridade do <i>RealSubject</i>.</p>
     *
     * @param sku código SKU a validar
     */
    private void validarSkuGlobalmenteInedito(String sku) {
        if (this.variacaoRepository.existsBySkuIgnoreCase(sku)
            || this.comboRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflitoDeEstadoOuDuplicidadeException(
                "Já existe um item ou combo cadastrado no catálogo com o SKU: " + sku
            );
        }
    }
}

