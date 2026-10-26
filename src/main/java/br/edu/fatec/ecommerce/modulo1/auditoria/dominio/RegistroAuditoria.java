package br.edu.fatec.ecommerce.modulo1.auditoria.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio e persistência que representa um evento imutável na trilha de auditoria (FR-012).
 *
 * <p><b>Design Pattern Aplicado: Static Factory Method (Objetos Static Factory)</b></p>
 * <ul>
 *   <li><b>1. Qual problema de projeto existia?</b> Criar registros de auditoria para diferentes
 *   operações (alteração de preço, ajuste de estoque, criação de categoria, mudança de status)
 *   usando um único construtor genérico de 10 parâmetros era confuso e sujeito a troca de campos.</li>
 *   <li><b>2. Por que este padrão foi escolhido?</b> Métodos fábrica estáticos com nomes expressivos
 *   de negócio documentam exatamente qual evento de auditoria está sendo construído e padronizam a
 *   formatação dos resumos anterior e novo.</li>
 *   <li><b>3. O que ficou mais simples de modificar?</b> Adicionar novos tipos de registros de
 *   auditoria (como auditoria de campanhas ou cancelamentos) exige apenas um novo Static Factory
 *   Method sem impactar os existentes.</li>
 * </ul>
 */
@Entity
@Table(name = "registros_auditoria")
public class RegistroAuditoria {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_responsavel_id", nullable = false, updatable = false)
    private UUID usuarioResponsavelId;

    @Column(name = "email_responsavel", length = 180, nullable = false, updatable = false)
    private String emailResponsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_responsavel", length = 40, nullable = false, updatable = false)
    private PerfilUsuario perfilResponsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", length = 60, nullable = false, updatable = false)
    private TipoOperacaoAuditoria tipoOperacao;

    @Column(name = "tipo_entidade_alvo", length = 80, nullable = false, updatable = false)
    private String tipoEntidadeAlvo;

    @Column(name = "identificador_alvo", length = 100, nullable = false, updatable = false)
    private String identificadorAlvo;

    @Column(name = "valor_anterior_resumo", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String valorAnteriorResumo;

    @Column(name = "valor_novo_resumo", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String valorNovoResumo;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    /**
     * Construtor protegido exigido exclusivamente pelo especificador JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate na leitura do banco.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta para forçar o uso dos
     * <b>Static Factory Methods</b>.</p>
     */
    protected RegistroAuditoria() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor privado explícito utilizado internamente pelos Static Factory Methods.
     *
     * <p>Objetivo Funcional: Validar e inicializar um registro imutável de auditoria.</p>
     * <p>Papel no Design Pattern: Construtor base encapsulado pelos <b>Static Factory Methods</b>.</p>
     *
     * @param usuarioResponsavelId identificador do autor da operação
     * @param emailResponsavel e-mail do autor
     * @param perfilResponsavel perfil ativo do autor no momento da ação
     * @param tipoOperacao classificação da operação auditada
     * @param tipoEntidadeAlvo nome da entidade afetada
     * @param identificadorAlvo chave/SKU do recurso afetado
     * @param valorAnteriorResumo estado anterior serializado em resumo legível
     * @param valorNovoResumo novo estado serializado em resumo legível
     */
    private RegistroAuditoria(
        UUID usuarioResponsavelId,
        String emailResponsavel,
        PerfilUsuario perfilResponsavel,
        TipoOperacaoAuditoria tipoOperacao,
        String tipoEntidadeAlvo,
        String identificadorAlvo,
        String valorAnteriorResumo,
        String valorNovoResumo
    ) {
        if (usuarioResponsavelId == null || emailResponsavel == null || emailResponsavel.isBlank()) {
            throw new RegraNegocioModulo1Exception("O usuário responsável pela operação auditada é obrigatório.");
        }
        if (perfilResponsavel == null || tipoOperacao == null) {
            throw new RegraNegocioModulo1Exception("O perfil e o tipo de operação de auditoria são obrigatórios.");
        }
        if (identificadorAlvo == null || identificadorAlvo.isBlank()) {
            throw new RegraNegocioModulo1Exception("O identificador da entidade auditada é obrigatório.");
        }
        this.id = UUID.randomUUID();
        this.usuarioResponsavelId = usuarioResponsavelId;
        this.emailResponsavel = emailResponsavel.trim().toLowerCase();
        this.perfilResponsavel = perfilResponsavel;
        this.tipoOperacao = tipoOperacao;
        this.tipoEntidadeAlvo = (tipoEntidadeAlvo == null || tipoEntidadeAlvo.isBlank()) ? "ENTIDADE" : tipoEntidadeAlvo;
        this.identificadorAlvo = identificadorAlvo.trim();
        this.valorAnteriorResumo = valorAnteriorResumo == null ? "N/A" : valorAnteriorResumo;
        this.valorNovoResumo = valorNovoResumo == null ? "N/A" : valorNovoResumo;
        this.ocorridoEm = Instant.now();
    }

    /**
     * Cria um registro imutável de auditoria para alteração de preço de produto ou variação.
     *
     * <p>Objetivo Funcional: Registrar quem alterou o preço de um SKU, de qual valor e para qual valor.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> especializado para auditoria de preço.</p>
     *
     * @param usuarioId ID do operador ou administrador
     * @param email e-mail do operador
     * @param perfil perfil do operador
     * @param sku código SKU do item cujo preço mudou
     * @param precoAnterior valor monetário anterior
     * @param precoNovo novo valor monetário aplicado
     * @param ehCombo indica se o item alterado é um Combo/Kit comercial
     * @return instância imutável de {@link RegistroAuditoria}
     */
    public static RegistroAuditoria registrarAlteracaoPreco(
        UUID usuarioId,
        String email,
        PerfilUsuario perfil,
        String sku,
        BigDecimal precoAnterior,
        BigDecimal precoNovo,
        boolean ehCombo
    ) {
        TipoOperacaoAuditoria operacao = ehCombo
            ? TipoOperacaoAuditoria.ALTERACAO_PRECO_COMBO
            : TipoOperacaoAuditoria.ALTERACAO_PRECO_PRODUTO;
        String tipoAlvo = ehCombo ? "ComboKitComposite" : "ItemCatalogo";
        return new RegistroAuditoria(
            usuarioId,
            email,
            perfil,
            operacao,
            tipoAlvo,
            sku,
            precoAnterior == null ? "0.00" : precoAnterior.toPlainString(),
            precoNovo == null ? "0.00" : precoNovo.toPlainString()
        );
    }

    /**
     * Cria um registro imutável de auditoria para criação ou modificação de categoria/subcategoria.
     *
     * <p>Objetivo Funcional: Auditar mudanças na estrutura da árvore de categorias.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> especializado para categorias.</p>
     *
     * @param usuarioId ID do operador responsável
     * @param email e-mail do operador
     * @param perfil perfil do operador
     * @param slugCategoria slug identificador da categoria
     * @param estadoAnterior descrição do estado anterior
     * @param estadoNovo descrição do novo estado
     * @return instância imutável de {@link RegistroAuditoria}
     */
    public static RegistroAuditoria registrarOperacaoCategoria(
        UUID usuarioId,
        String email,
        PerfilUsuario perfil,
        String slugCategoria,
        String estadoAnterior,
        String estadoNovo
    ) {
        return new RegistroAuditoria(
            usuarioId,
            email,
            perfil,
            TipoOperacaoAuditoria.CRIACAO_ALTERACAO_CATEGORIA,
            "CategoriaComp",
            slugCategoria,
            estadoAnterior,
            estadoNovo
        );
    }

    /**
     * Cria um registro imutável de auditoria para cadastro de produto ou Combo/Kit, ou mudança de status ativo.
     *
     * <p>Objetivo Funcional: Auditar inclusões no catálogo e ativações/inativações lógicas.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> para eventos de ciclo de vida de item de catálogo.</p>
     *
     * @param usuarioId ID do operador responsável
     * @param email e-mail do operador
     * @param perfil perfil do operador
     * @param tipoOperacao tipo específico da operação (cadastro ou ativação/inativação)
     * @param tipoEntidade classe de domínio alvo
     * @param sku identificador do item
     * @param resumoAnterior estado anterior
     * @param resumoNovo novo estado
     * @return instância imutável de {@link RegistroAuditoria}
     */
    public static RegistroAuditoria registrarEventoCatalogo(
        UUID usuarioId,
        String email,
        PerfilUsuario perfil,
        TipoOperacaoAuditoria tipoOperacao,
        String tipoEntidade,
        String sku,
        String resumoAnterior,
        String resumoNovo
    ) {
        return new RegistroAuditoria(
            usuarioId,
            email,
            perfil,
            tipoOperacao,
            tipoEntidade,
            sku,
            resumoAnterior,
            resumoNovo
        );
    }

    /**
     * Cria um registro imutável de auditoria para ajustes manuais ou entradas de estoque.
     *
     * <p>Objetivo Funcional: Rastrear toda alteração administrativa de saldo físico de um SKU.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> especializado invocado pelo
     * {@code EstoqueAdminSegurancaProxy}.</p>
     *
     * @param usuarioId ID do responsável pelo estoque ou admin
     * @param email e-mail do responsável
     * @param perfil perfil do responsável
     * @param sku código SKU ajustado
     * @param saldoAnterior quantidade física disponível antes do ajuste
     * @param saldoNovo quantidade física disponível após o ajuste
     * @param motivo justificativa do ajuste
     * @return instância imutável de {@link RegistroAuditoria}
     */
    public static RegistroAuditoria registrarAjusteEstoque(
        UUID usuarioId,
        String email,
        PerfilUsuario perfil,
        String sku,
        int saldoAnterior,
        int saldoNovo,
        String motivo
    ) {
        return new RegistroAuditoria(
            usuarioId,
            email,
            perfil,
            TipoOperacaoAuditoria.AJUSTE_MANUAL_ESTOQUE,
            "ItemEstoque",
            sku,
            "disponivel=" + saldoAnterior,
            "disponivel=" + saldoNovo + " (motivo=" + (motivo == null ? "N/A" : motivo) + ")"
        );
    }

    /**
     * Cria um registro imutável de auditoria para provisão de um novo operador administrativo/interno.
     *
     * <p>Objetivo Funcional: Auditar a criação de contas de operadores por um administrador (FR-002).</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> especializado para provisão de contas internas.</p>
     *
     * @param emailAdminExecutor e-mail do administrador que executou a provisão
     * @param emailNovoOperador e-mail do operador criado
     * @param perfisAtribuidos descrição dos perfis concedidos
     * @return instância imutável de {@link RegistroAuditoria}
     */
    public static RegistroAuditoria registrarCriacaoOperador(
        String emailAdminExecutor,
        String emailNovoOperador,
        String perfisAtribuidos
    ) {
        return new RegistroAuditoria(
            UUID.nameUUIDFromBytes((emailAdminExecutor != null ? emailAdminExecutor : "ADMIN").getBytes()),
            emailAdminExecutor != null ? emailAdminExecutor : "admin@ecommerce.fatec.edu.br",
            PerfilUsuario.ADMIN,
            TipoOperacaoAuditoria.ATIVACAO_INATIVACAO_ITEM,
            "UsuarioOperador",
            emailNovoOperador,
            "INEXISTENTE",
            "ATIVO perfis=" + perfisAtribuidos
        );
    }

    /**
     * Obtém o identificador único do registro de auditoria.
     *
     * <p>Objetivo Funcional: Identificar univocamente o evento histórico.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return UUID do registro
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o ID do usuário que executou a operação.
     *
     * <p>Objetivo Funcional: Permitir filtro de auditoria por operador.</p>
     * <p>Papel no Design Pattern: Getter explícito de autoria.</p>
     *
     * @return UUID do usuário responsável
     */
    public UUID obterUsuarioResponsavelId() {
        return this.usuarioResponsavelId;
    }

    /**
     * Obtém o e-mail do usuário responsável pela ação.
     *
     * <p>Objetivo Funcional: Exibir a identificação legível do operador em relatórios de auditoria.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return e-mail do usuário responsável
     */
    public String obterEmailResponsavel() {
        return this.emailResponsavel;
    }

    /**
     * Obtém o perfil sob o qual o usuário realizou a operação.
     *
     * <p>Objetivo Funcional: Comprovar qual papel autorizou a mudança.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return perfil do usuário responsável
     */
    public PerfilUsuario obterPerfilResponsavel() {
        return this.perfilResponsavel;
    }

    /**
     * Obtém o tipo da operação administrativa registrada.
     *
     * <p>Objetivo Funcional: Classificar a ação na trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return enum {@link TipoOperacaoAuditoria}
     */
    public TipoOperacaoAuditoria obterTipoOperacao() {
        return this.tipoOperacao;
    }

    /**
     * Obtém o nome do tipo de entidade afetada pela operação.
     *
     * <p>Objetivo Funcional: Indicar qual agregado sofreu a alteração.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return nome da entidade alvo
     */
    public String obterTipoEntidadeAlvo() {
        return this.tipoEntidadeAlvo;
    }

    /**
     * Obtém o código ou identificador da entidade afetada (ex.: SKU ou slug).
     *
     * <p>Objetivo Funcional: Rastrear exatamente o item alterado.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return identificador textual do recurso alvo
     */
    public String obterIdentificadorAlvo() {
        return this.identificadorAlvo;
    }

    /**
     * Obtém o resumo do valor ou estado anterior à operação.
     *
     * <p>Objetivo Funcional: Preservar o snapshot anterior para comparação.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return resumo textual do estado anterior
     */
    public String obterValorAnteriorResumo() {
        return this.valorAnteriorResumo;
    }

    /**
     * Obtém o resumo do novo valor ou estado resultante da operação.
     *
     * <p>Objetivo Funcional: Preservar o novo valor aplicado.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado imutável.</p>
     *
     * @return resumo textual do novo estado
     */
    public String obterValorNovoResumo() {
        return this.valorNovoResumo;
    }

    /**
     * Obtém o instante exato em que a operação foi gravada.
     *
     * <p>Objetivo Funcional: Garantir ordenação cronológica exata da trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Getter explícito de timestamp imutável.</p>
     *
     * @return instante UTC da ocorrência
     */
    public Instant obterOcorridoEm() {
        return this.ocorridoEm;
    }
}
