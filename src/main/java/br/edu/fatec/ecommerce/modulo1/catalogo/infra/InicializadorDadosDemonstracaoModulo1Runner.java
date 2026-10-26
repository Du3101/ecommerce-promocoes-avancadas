package br.edu.fatec.ecommerce.modulo1.catalogo.infra;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoPort;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.FabricaAtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.TipoAtributoVariacao;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.CategoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.InicializadorAdminPadraoRunner;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador de dados de demonstração do Módulo 1 para apresentações interativas via Swagger UI.
 *
 * <p>Quando a propriedade {@code ecommerce.modulo1.seed-demo-ativo=true} está habilitada, popula de
 * forma idempotente uma árvore de categorias de 3 níveis (<b>Composite</b> + <b>Null Object</b>),
 * dois produtos físicos com variações compartilhando instâncias em memória (<b>Flyweight</b> +
 * <b>Builder</b> + <b>Factory Method</b>), saldos iniciais de estoque e um pacote promocional
 * (<b>ComboKitComposite</b>), passando pelo <b>Proxy</b> de auditoria para que o histórico
 * administrativo já apresente registros prontos para consulta.</p>
 */
@Component
@Order(2)
public class InicializadorDadosDemonstracaoModulo1Runner implements CommandLineRunner {

    /** Slug da categoria raiz de demonstração utilizada para verificação de idempotência. */
    public static final String SLUG_CATEGORIA_RAIZ_DEMO = "eletronicos-demo";

    /** SKU da variação de teclado mecânico de demonstração. */
    public static final String SKU_TECLADO_DEMO = "DEMO-TEC-PRO-PRETO";

    /** SKU da variação de mouse óptico de demonstração. */
    public static final String SKU_MOUSE_DEMO = "DEMO-MOU-PREC-PRETO";

    /** SKU do Combo/Kit promocional de demonstração (1 Teclado + 2 Mouses). */
    public static final String SKU_COMBO_GAMER_DEMO = "DEMO-KIT-GAMER-01";

    private final CategoriaJpaRepository categoriaRepository;
    private final ServicoAdminCatalogoPort servicoAdminCatalogoProxy;
    private final FabricaAtributoVariacaoFlyweight fabricaFlyweight;
    private final boolean seedDemoAtivo;

    /**
     * Construtor com injeção explícita das dependências de catálogo, fábrica Flyweight e flag de ativação.
     *
     * <p>Objetivo Funcional: Configurar o runner de carga de demonstração para apresentações em memória.</p>
     * <p>Papel no Design Pattern: Cliente orquestrador que exercita os padrões <b>Proxy</b>,
     * <b>Composite</b>, <b>Flyweight</b>, <b>Builder</b>, <b>Factory Method</b> e <b>Static Factory Method</b>.</p>
     *
     * @param categoriaRepository repositório JPA de categorias para checagem idempotente
     * @param servicoAdminCatalogoProxy porta administrativa primária envolvida pelo {@code CatalogoAdminAuditoriaProxy}
     * @param fabricaFlyweight fábrica compartilhada de atributos intrínsecos de variação
     * @param seedDemoAtivo indica se a carga automática deve rodar na subida da aplicação
     */
    public InicializadorDadosDemonstracaoModulo1Runner(
        CategoriaJpaRepository categoriaRepository,
        ServicoAdminCatalogoPort servicoAdminCatalogoProxy,
        FabricaAtributoVariacaoFlyweight fabricaFlyweight,
        @Value("${ecommerce.modulo1.seed-demo-ativo:false}") boolean seedDemoAtivo
    ) {
        this.categoriaRepository = categoriaRepository;
        this.servicoAdminCatalogoProxy = servicoAdminCatalogoProxy;
        this.fabricaFlyweight = fabricaFlyweight;
        this.seedDemoAtivo = seedDemoAtivo;
    }

    /**
     * Callback do Spring Boot acionado logo após a inicialização do usuário ADMIN padrão ({@code @Order(2)}).
     *
     * <p>Objetivo Funcional: Popular automaticamente o catálogo e o estoque quando a flag
     * {@code ecommerce.modulo1.seed-demo-ativo} estiver ativa.</p>
     * <p>Papel no Design Pattern: Ponto de entrada de bootstrap da aplicação.</p>
     *
     * @param args argumentos de linha de comando
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (this.seedDemoAtivo) {
            executarCargaDemonstracaoIdempotente();
        }
    }

    /**
     * Executa de maneira idempotente a criação da hierarquia de categorias, produtos, variações,
     * estoques e combo promocional de demonstração.
     *
     * <p>Objetivo Funcional: Garantir que o catálogo de demonstração seja criado apenas uma vez,
     * registrando todas as operações na trilha de auditoria sob a identidade do administrador inicial.</p>
     * <p>Papel no Design Pattern: Exercita de ponta a ponta o <b>Proxy</b> ({@code CatalogoAdminAuditoriaProxy}),
     * <b>Composite</b> ({@link CategoriaComp} e {@code ComboKitComposite}), <b>Flyweight</b>
     * ({@link FabricaAtributoVariacaoFlyweight}) e <b>Builder</b> ({@link ProdutoBuilder}).</p>
     *
     * @return {@code true} se os dados foram inseridos nesta chamada; {@code false} se já existiam
     */
    @Transactional
    public boolean executarCargaDemonstracaoIdempotente() {
        if (this.categoriaRepository.existsBySlugIgnoreCase(SLUG_CATEGORIA_RAIZ_DEMO)) {
            return false;
        }

        Authentication autenticacaoAnterior = SecurityContextHolder.getContext().getAuthentication();
        try {
            configurarContextoSegurancaAdminTemporario();
            CategoriaComp categoriaFolha = criarArvoreCategoriasDemonstracao();
            cadastrarProdutosComVariacoesFlyweightDemonstracao(categoriaFolha);
            cadastrarComboKitPromocionalDemonstracao(categoriaFolha);
            return true;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(autenticacaoAnterior);
        }
    }

    /**
     * Configura temporariamente no {@link SecurityContextHolder} a autenticação do administrador
     * padrão para que o {@code CatalogoAdminAuditoriaProxy} valide o perfil {@code ADMIN} e grave
     * a autoria correta na trilha de auditoria.
     *
     * <p>Objetivo Funcional: Permitir que o bootstrap administrativo passe pelas mesmas regras de
     * segurança e auditoria das chamadas HTTP.</p>
     * <p>Papel no Design Pattern: Prepara o contexto de segurança consumido pelo <i>Protection &amp; Audit Proxy</i>.</p>
     */
    private void configurarContextoSegurancaAdminTemporario() {
        UsernamePasswordAuthenticationToken tokenAdmin = new UsernamePasswordAuthenticationToken(
            InicializadorAdminPadraoRunner.EMAIL_ADMIN_PADRAO,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(tokenAdmin);
    }

    /**
     * Cria uma árvore mercadológica de 3 níveis (`Eletrônicos > Periféricos > Teclados e Mouses`).
     *
     * <p>Objetivo Funcional: Demonstrar a navegação recursiva de categorias e subcategorias (US2 / FR-007).</p>
     * <p>Papel no Design Pattern: Constrói nós compostos da estrutura <b>Composite</b> ({@link CategoriaComp})
     * cujo topo referencia {@code CategoriaRaizNullObject} (<b>Null Object</b>).</p>
     *
     * @return categoria folha de 3º nível (`Teclados e Mouses`)
     */
    private CategoriaComp criarArvoreCategoriasDemonstracao() {
        CategoriaComp eletronicos = this.servicoAdminCatalogoProxy.criarCategoria(
            "Eletrônicos",
            SLUG_CATEGORIA_RAIZ_DEMO,
            null
        );
        CategoriaComp perifericos = this.servicoAdminCatalogoProxy.criarCategoria(
            "Periféricos",
            "perifericos-demo",
            eletronicos.obterId()
        );
        return this.servicoAdminCatalogoProxy.criarCategoria(
            "Teclados e Mouses",
            "teclados-e-mouses-demo",
            perifericos.obterId()
        );
    }

    /**
     * Cadastra dois produtos físicos na subcategoria informada, compartilhando o atributo intrínseco
     * {@code COR = Preto} via {@link FabricaAtributoVariacaoFlyweight} e inicializando seus saldos de estoque.
     *
     * <p>Objetivo Funcional: Disponibilizar itens unitários com variações e estoque para consulta imediata
     * no Swagger UI (US2 e US4).</p>
     * <p>Papel no Design Pattern: Demonstra o compartilhamento de estado intrínseco via <b>Flyweight</b>,
     * a construção fluente via <b>Builder</b> ({@link ProdutoBuilder}), o <b>Static Factory Method</b>
     * ({@link DimensoesFisicas#emCentimetrosEGramas}) e o <b>Factory Method</b> ({@code CriadorProdutoPadrao}).</p>
     *
     * @param categoriaFolha categoria onde os produtos serão vinculados
     */
    private void cadastrarProdutosComVariacoesFlyweightDemonstracao(CategoriaComp categoriaFolha) {
        AtributoVariacaoFlyweight corPretoCompartilhada = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.COR,
            "Preto",
            "#000000"
        );
        AtributoVariacaoFlyweight switchRed = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.ESPECIFICACAO,
            "Switch Mecânico Red",
            "SW-RED"
        );
        AtributoVariacaoFlyweight sensorOptico = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.ESPECIFICACAO,
            "Sensor Óptico 16000 DPI",
            "DPI-16K"
        );

        ProdutoBuilder builderTeclado = new ProdutoBuilder()
            .comNome("Teclado Mecânico Gamer FATEC Pro")
            .comDescricao("Teclado mecânico ABNT2 com iluminação RGB e switches lineares de alta precisão")
            .comMarca("FATEC Gear")
            .comPrecoBase(new BigDecimal("300.00"))
            .naCategoria(categoriaFolha.obterId())
            .adicionarAtributoEspecifico("Layout", "ABNT2")
            .adicionarAtributoEspecifico("Garantia", "12 meses")
            .adicionarVariacao(
                SKU_TECLADO_DEMO,
                "7890000100011",
                "Preto / Switch Red",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(850, new BigDecimal("4.50"), new BigDecimal("44.00"), new BigDecimal("14.00")),
                List.of(corPretoCompartilhada, switchRed)
            );
        this.servicoAdminCatalogoProxy.cadastrarProdutoComVariacoes(
            builderTeclado,
            Map.of(SKU_TECLADO_DEMO, 10)
        );

        ProdutoBuilder builderMouse = new ProdutoBuilder()
            .comNome("Mouse Gamer Óptico FATEC Precision")
            .comDescricao("Mouse gamer ultraleve com sensor óptico de 16000 DPI e cabo paracord")
            .comMarca("FATEC Gear")
            .comPrecoBase(new BigDecimal("150.00"))
            .naCategoria(categoriaFolha.obterId())
            .adicionarAtributoEspecifico("Conectividade", "USB-A Com Fio")
            .adicionarVariacao(
                SKU_MOUSE_DEMO,
                "7890000100028",
                "Preto / 16000 DPI",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(120, new BigDecimal("4.00"), new BigDecimal("6.50"), new BigDecimal("12.50")),
                List.of(corPretoCompartilhada, sensorOptico)
            );
        this.servicoAdminCatalogoProxy.cadastrarProdutoComVariacoes(
            builderMouse,
            Map.of(SKU_MOUSE_DEMO, 5)
        );
    }

    /**
     * Cadastra um Combo/Kit promocional composto por 1 Teclado (`10` em estoque) + 2 Mouses (`5` em estoque),
     * demonstrando o cálculo recursivo de preço com `10%` de desconto (`R$ 540,00`), peso (`1090g`) e
     * disponibilidade máxima limitada pelo gargalo (`2 kits`).
     *
     * <p>Objetivo Funcional: Demonstrar a composição recursiva de pacotes comerciais (US3 / FR-008).</p>
     * <p>Papel no Design Pattern: Exercita o <b>Composite</b> ({@code ComboKitComposite}),
     * <b>Builder</b> ({@code ComboKitBuilder}) e <b>Factory Method</b> ({@code CriadorComboKitPromocional}).</p>
     *
     * @param categoriaFolha categoria principal do combo
     */
    private void cadastrarComboKitPromocionalDemonstracao(CategoriaComp categoriaFolha) {
        Map<String, Integer> componentesKit = new LinkedHashMap<>();
        componentesKit.put(SKU_TECLADO_DEMO, 1);
        componentesKit.put(SKU_MOUSE_DEMO, 2);

        this.servicoAdminCatalogoProxy.cadastrarComboKitPromocional(
            SKU_COMBO_GAMER_DEMO,
            "Kit Gamer FATEC Completo (1 Teclado + 2 Mouses)",
            "Combo promocional com 10% de desconto automático sobre a soma recursiva dos componentes",
            categoriaFolha.obterId(),
            ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES,
            new BigDecimal("10.00"),
            null,
            componentesKit
        );
    }
}
