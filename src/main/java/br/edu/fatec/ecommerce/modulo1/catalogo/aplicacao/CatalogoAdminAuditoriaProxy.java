package br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.AcessoNegadoModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.TipoOperacaoAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proxy de Proteção (RBAC) e Auditoria Automática para todas as operações administrativas do Catálogo (FR-002, FR-012).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papéis <i>Protection Proxy</i> e <i>Audit Proxy</i>
 * (Princípio II da Constituição).</p>
 * <p>Justificativa: Implementa a mesma interface {@link ServicoAdminCatalogoPort} do sujeito real
 * ({@link ServicoAdminCatalogoReal}) e é injetado como {@code @Primary}. Antes de delegar ao sujeito real,
 * valida se o usuário autenticado possui perfil {@code ADMIN} ou {@code OPERADOR_COMERCIAL} e captura o
 * estado anterior; após a execução bem-sucedida, grava um {@link RegistroAuditoria} imutável.</p>
 */
@Service
@Primary
public class CatalogoAdminAuditoriaProxy implements ServicoAdminCatalogoPort {

    private final ServicoAdminCatalogoPort servicoReal;
    private final ServicoConsultaCatalogo servicoConsultaCatalogo;
    private final RegistroAuditoriaJpaRepository auditoriaRepository;
    private final UsuarioJpaRepository usuarioRepository;

    /**
     * Construtor com injeção explícita do sujeito real qualificado e dos repositórios de auditoria e identidade.
     *
     * <p>Objetivo Funcional: Encapsular o {@link ServicoAdminCatalogoReal} adicionando segurança e auditoria.</p>
     * <p>Papel no Design Pattern: Inicializa o <b>Proxy</b> mantendo referência ao <i>RealSubject</i>.</p>
     *
     * @param servicoReal instância de {@link ServicoAdminCatalogoReal}
     * @param servicoConsultaCatalogo serviço de leitura para captura de estado anterior
     * @param auditoriaRepository repositório da trilha de auditoria
     * @param usuarioRepository repositório de usuários para resolução do UUID do autor
     */
    public CatalogoAdminAuditoriaProxy(
        @Qualifier("servicoAdminCatalogoReal") ServicoAdminCatalogoPort servicoReal,
        ServicoConsultaCatalogo servicoConsultaCatalogo,
        RegistroAuditoriaJpaRepository auditoriaRepository,
        UsuarioJpaRepository usuarioRepository
    ) {
        this.servicoReal = servicoReal;
        this.servicoConsultaCatalogo = servicoConsultaCatalogo;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Intercepta a criação de categoria ou subcategoria exigindo perfil autorizado e gravando auditoria.
     *
     * <p>Objetivo Funcional: Garantir que apenas {@code ADMIN} ou {@code OPERADOR_COMERCIAL} criem categorias
     * e auditar a alteração na árvore mercadológica.</p>
     * <p>Papel no Design Pattern: Método interceptor do <b>Proxy</b> que delega ao <i>RealSubject</i>.</p>
     *
     * @param nome nome da categoria
     * @param slugOpcional slug opcional
     * @param categoriaPaiIdOpcional UUID opcional da categoria pai
     * @return categoria criada pelo sujeito real
     */
    @Override
    @Transactional
    public CategoriaComp criarCategoria(String nome, String slugOpcional, UUID categoriaPaiIdOpcional) {
        exigirPerfilComercialOuAdmin("criação de categoria no catálogo");
        CategoriaComp criada = this.servicoReal.criarCategoria(nome, slugOpcional, categoriaPaiIdOpcional);

        AutorAuditoria autor = resolverAutorAtual();
        RegistroAuditoria registro = RegistroAuditoria.registrarOperacaoCategoria(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            criada.obterSlug(),
            "INEXISTENTE",
            "CRIADA caminho='" + criada.obterCaminhoHierarquico() + "'"
        );
        this.auditoriaRepository.save(registro);
        return criada;
    }

    /**
     * Intercepta a vinculação de subcategoria validando permissão e auditando o caminho anterior e novo.
     *
     * <p>Objetivo Funcional: Auditar reorganizações na árvore de categorias.</p>
     * <p>Papel no Design Pattern: Método interceptor do <b>Proxy</b>.</p>
     *
     * @param categoriaPaiId UUID da categoria pai
     * @param subcategoriaId UUID da subcategoria
     * @return categoria pai atualizada
     */
    @Override
    @Transactional
    public CategoriaComp vincularSubcategoria(UUID categoriaPaiId, UUID subcategoriaId) {
        exigirPerfilComercialOuAdmin("vinculação de subcategoria");
        CategoriaComp subAntes = this.servicoConsultaCatalogo.obterCategoriaPorId(subcategoriaId);
        String caminhoAnterior = subAntes.obterCaminhoHierarquico();

        CategoriaComp paiAtualizado = this.servicoReal.vincularSubcategoria(categoriaPaiId, subcategoriaId);
        CategoriaComp subDepois = this.servicoConsultaCatalogo.obterCategoriaPorId(subcategoriaId);

        AutorAuditoria autor = resolverAutorAtual();
        RegistroAuditoria registro = RegistroAuditoria.registrarOperacaoCategoria(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            subDepois.obterSlug(),
            "caminho='" + caminhoAnterior + "'",
            "caminho='" + subDepois.obterCaminhoHierarquico() + "'"
        );
        this.auditoriaRepository.save(registro);
        return paiAtualizado;
    }

    /**
     * Intercepta o cadastro de novos produtos e variações validando permissão e gravando trilha de auditoria.
     *
     * <p>Objetivo Funcional: Auditar a inclusão de novos produtos e SKUs no catálogo.</p>
     * <p>Papel no Design Pattern: Método interceptor do <b>Proxy</b>.</p>
     *
     * @param builder builder configurado do produto
     * @param estoqueInicialPorSku mapa opcional de saldo inicial
     * @return produto cadastrado pelo sujeito real
     */
    @Override
    @Transactional
    public Produto cadastrarProdutoComVariacoes(
        ProdutoBuilder builder,
        Map<String, Integer> estoqueInicialPorSku
    ) {
        exigirPerfilComercialOuAdmin("cadastro de produto no catálogo");
        Produto salvo = this.servicoReal.cadastrarProdutoComVariacoes(builder, estoqueInicialPorSku);

        AutorAuditoria autor = resolverAutorAtual();
        String primeiroSku = salvo.obterVariacoes().get(0).obterSku();
        RegistroAuditoria registro = RegistroAuditoria.registrarEventoCatalogo(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            TipoOperacaoAuditoria.ALTERACAO_PRECO_PRODUTO,
            "Produto",
            primeiroSku,
            "INEXISTENTE",
            "CADASTRADO precoBase=" + salvo.obterPrecoBase().toPlainString()
                + " variacoes=" + salvo.obterVariacoes().size()
        );
        this.auditoriaRepository.save(registro);
        return salvo;
    }

    /**
     * Intercepta o cadastro de um Combo/Kit promocional validando permissão e auditando sua criação.
     *
     * <p>Objetivo Funcional: Auditar a criação de pacotes compostos no catálogo (FR-008, FR-012).</p>
     * <p>Papel no Design Pattern: Método interceptor do <b>Proxy</b>.</p>
     *
     * @param skuCombo código SKU do combo
     * @param nomeCombo nome do combo
     * @param descricao descrição
     * @param categoriaId UUID da categoria
     * @param modoPreco modo de preço
     * @param percentualDescontoKit percentual de desconto
     * @param precoFixoOpcional preço fixo opcional
     * @param quantidadesPorSkuComponente mapa de componentes filhos
     * @return combo criado pelo sujeito real
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
        exigirPerfilComercialOuAdmin("cadastro de combo/kit promocional");
        ComboKitComposite combo = this.servicoReal.cadastrarComboKitPromocional(
            skuCombo,
            nomeCombo,
            descricao,
            categoriaId,
            modoPreco,
            percentualDescontoKit,
            precoFixoOpcional,
            quantidadesPorSkuComponente
        );

        AutorAuditoria autor = resolverAutorAtual();
        RegistroAuditoria registro = RegistroAuditoria.registrarAlteracaoPreco(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            combo.obterSku(),
            BigDecimal.ZERO,
            combo.calcularPrecoVigente(),
            true
        );
        this.auditoriaRepository.save(registro);
        return combo;
    }

    /**
     * Intercepta alterações de preço de produtos ou combos capturando o preço vigente anterior
     * e o novo preço vigente para persistência na trilha de auditoria (FR-012).
     *
     * <p>Objetivo Funcional: Registrar exatamente quem alterou o preço de um SKU, quando, de qual
     * valor anterior e para qual valor novo.</p>
     * <p>Papel no Design Pattern: Operação de auditoria antes/depois (Around Advice) do padrão <b>Proxy</b>.</p>
     *
     * @param sku código SKU da variação ou combo
     * @param novoValor novo preço ou parâmetro de desconto
     * @return item atualizado pelo sujeito real
     */
    @Override
    @Transactional
    public ItemCatalogoComponent alterarPrecoItemCatalogo(String sku, BigDecimal novoValor) {
        exigirPerfilComercialOuAdmin("alteração de preço de item do catálogo");

        ItemCatalogoComponent antes = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        BigDecimal precoAnterior = antes.calcularPrecoVigente();
        boolean ehCombo = antes.eComboKit();

        ItemCatalogoComponent depois = this.servicoReal.alterarPrecoItemCatalogo(sku, novoValor);
        BigDecimal precoNovo = depois.calcularPrecoVigente();

        AutorAuditoria autor = resolverAutorAtual();
        RegistroAuditoria registro = RegistroAuditoria.registrarAlteracaoPreco(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            depois.obterSku(),
            precoAnterior,
            precoNovo,
            ehCombo
        );
        this.auditoriaRepository.save(registro);
        return depois;
    }

    /**
     * Intercepta ativações e inativações lógicas de itens do catálogo gravando trilha de auditoria.
     *
     * <p>Objetivo Funcional: Auditar mudanças de status comercial (ativo/inativo) por SKU.</p>
     * <p>Papel no Design Pattern: Método interceptor do <b>Proxy</b>.</p>
     *
     * @param sku código SKU da variação ou combo
     * @param ativo novo status
     * @return item atualizado pelo sujeito real
     */
    @Override
    @Transactional
    public ItemCatalogoComponent alterarStatusAtivacaoItemCatalogo(String sku, boolean ativo) {
        exigirPerfilComercialOuAdmin("ativação/inativação lógica de item do catálogo");

        ItemCatalogoComponent antes = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        boolean statusAnterior = antes.estaAtivo();

        ItemCatalogoComponent depois = this.servicoReal.alterarStatusAtivacaoItemCatalogo(sku, ativo);

        AutorAuditoria autor = resolverAutorAtual();
        RegistroAuditoria registro = RegistroAuditoria.registrarEventoCatalogo(
            autor.usuarioId(),
            autor.email(),
            autor.perfil(),
            TipoOperacaoAuditoria.ATIVACAO_INATIVACAO_ITEM,
            depois.eComboKit() ? "ComboKitComposite" : "ProdutoVariacaoLeaf",
            depois.obterSku(),
            "ativo=" + statusAnterior,
            "ativo=" + depois.estaAtivo()
        );
        this.auditoriaRepository.save(registro);
        return depois;
    }

    /**
     * Verifica se o usuário autenticado no contexto possui perfil {@code ADMIN} ou {@code OPERADOR_COMERCIAL}.
     *
     * <p>Objetivo Funcional: Impedir que clientes ou outros operadores modifiquem o catálogo (FR-002).</p>
     * <p>Papel no Design Pattern: Guarda de segurança do <i>Protection Proxy</i>.</p>
     *
     * @param descricaoOperacao nome da operação protegida
     * @throws AcessoNegadoModulo1Exception se o usuário não possuir perfil autorizado
     */
    private void exigirPerfilComercialOuAdmin(String descricaoOperacao) {
        if (!ContextoSegurancaUsuario.possuiAlgumPerfil(
            PerfilUsuario.ADMIN,
            PerfilUsuario.OPERADOR_COMERCIAL
        )) {
            throw new AcessoNegadoModulo1Exception(
                "Acesso negado para " + descricaoOperacao
                    + ": requer perfil ADMIN ou OPERADOR_COMERCIAL."
            );
        }
    }

    /**
     * Resolve os dados de identificação (UUID, e-mail e perfil) do usuário autenticado.
     *
     * <p>Objetivo Funcional: Preencher os metadados obrigatórios de autoria na trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Método auxiliar interno do <i>Audit Proxy</i>.</p>
     *
     * @return registro {@link AutorAuditoria} com ID, e-mail e perfil
     */
    private AutorAuditoria resolverAutorAtual() {
        String email = ContextoSegurancaUsuario.obterUsernameOuPadrao("comercial@ecommerce.fatec.edu.br");
        PerfilUsuario perfil = ContextoSegurancaUsuario.obterPerfilPrincipalOuPadrao(
            PerfilUsuario.OPERADOR_COMERCIAL
        );
        UUID usuarioId = this.usuarioRepository.findByEmailIgnoreCase(email)
            .map(Usuario::obterId)
            .orElseGet(() -> ContextoSegurancaUsuario.derivarUuidDeterministicoPorEmail(email));
        return new AutorAuditoria(usuarioId, email, perfil);
    }

    /**
     * Tupla interna imutável com os dados do autor da operação auditada.
     *
     * @param usuarioId UUID do usuário responsável
     * @param email e-mail do responsável
     * @param perfil perfil sob o qual executou a ação
     */
    private record AutorAuditoria(UUID usuarioId, String email, PerfilUsuario perfil) {
    }
}

