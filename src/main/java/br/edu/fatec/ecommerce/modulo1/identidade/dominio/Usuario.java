package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Entidade de domínio que representa uma conta autenticável na plataforma de e-commerce.
 *
 * <p><b>Design Pattern Aplicado: Static Factory Method (Objetos Static Factory)</b></p>
 * <ul>
 *   <li><b>1. Qual problema de projeto existia?</b> O sistema possui duas regras de negócio
 *   distintas para criação de contas (FR-001 e FR-002): o cadastro público deve criar
 *   exclusivamente contas {@code CLIENTE}, enquanto contas internas ({@code ADMIN},
 *   {@code OPERADOR_COMERCIAL}, {@code RESPONSAVEL_ESTOQUE}, {@code RESPONSAVEL_PROMOCOES})
 *   só podem ser criadas por um Administrador. Um construtor público genérico permitiria
 *   atribuição acidental de perfil administrativo a um cliente público.</li>
 *   <li><b>2. Por que este padrão foi escolhido?</b> Métodos fábrica estáticos explícitos
 *   ({@link #criarContaClientePublico(String, String)} e
 *   {@link #criarContaOperadorInterno(String, String, Set)}) separam claramente os dois caminhos
 *   e aplicam as invariantes de segurança específicas de cada contexto.</li>
 *   <li><b>3. O que ficou mais simples de modificar?</b> Adicionar novas modalidades de conta
 *   (ex.: conta de parceiro logístico ou fornecedor) exige apenas um novo Static Factory Method
 *   sem risco de comprometer o auto-cadastro público de clientes.</li>
 * </ul>
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", length = 180, nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", length = 255, nullable = false)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", length = 40, nullable = false)
    private Set<PerfilUsuario> perfis = EnumSet.noneOf(PerfilUsuario.class);

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir reconstrução da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta para obrigar o uso dos
     * <b>Static Factory Methods</b>.</p>
     */
    protected Usuario() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor privado explícito invocado internamente pelos Static Factory Methods.
     *
     * <p>Objetivo Funcional: Validar formato do e-mail, garantir que o hash da senha foi gerado
     * e inicializar o usuário ativo.</p>
     * <p>Papel no Design Pattern: Construtor base encapsulado pelos <b>Static Factory Methods</b>.</p>
     *
     * @param email endereço de e-mail único (máximo 180 caracteres)
     * @param senhaHash resumo criptográfico irreversível BCrypt (nunca senha em texto puro)
     * @param perfis conjunto não vazio de perfis de acesso
     */
    private Usuario(String email, String senhaHash, Set<PerfilUsuario> perfis) {
        String emailNormalizado = validarENormalizarEmail(email);
        validarHashDeSenhaBcrypt(senhaHash);
        if (perfis == null || perfis.isEmpty()) {
            throw new RegraNegocioModulo1Exception("Todo usuário deve possuir ao menos um perfil de acesso.");
        }
        this.id = UUID.randomUUID();
        this.email = emailNormalizado;
        this.senhaHash = senhaHash;
        this.ativo = true;
        this.perfis = EnumSet.copyOf(perfis);
        this.criadoEm = Instant.now();
    }

    /**
     * Cria uma conta de usuário originada no auto-cadastro público da loja (FR-001).
     *
     * <p>Objetivo Funcional: Fixar obrigatoriamente e exclusivamente o perfil {@link PerfilUsuario#CLIENTE}
     * para cadastros públicos, impedindo elevação de privilégio no cadastro.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> para criação segura de conta de cliente.</p>
     *
     * @param email endereço de e-mail válido do cliente
     * @param senhaHash resumo criptográfico BCrypt da senha do cliente
     * @return nova instância válida de {@link Usuario} com perfil {@code CLIENTE}
     */
    public static Usuario criarContaClientePublico(String email, String senhaHash) {
        // Atribui exclusivamente o perfil CLIENTE conforme clarificado em FR-001
        return new Usuario(email, senhaHash, EnumSet.of(PerfilUsuario.CLIENTE));
    }

    /**
     * Cria uma conta de cliente público aplicando validação de senha em texto plano e codificação BCrypt.
     *
     * <p>Objetivo Funcional: Garantir senha forte (mínimo 8 caracteres) e gerar hash BCrypt irreversível
     * antes de instanciar a conta {@code CLIENTE} (FR-001).</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> completo para auto-cadastro público.</p>
     *
     * @param email endereço de e-mail válido do cliente
     * @param senhaEmTextoPlano senha em texto plano (mínimo 8 caracteres)
     * @param passwordEncoder codificador BCrypt
     * @return nova instância de {@link Usuario} com perfil {@code CLIENTE}
     */
    public static Usuario criarContaClientePublico(
        String email,
        String senhaEmTextoPlano,
        PasswordEncoder passwordEncoder
    ) {
        validarForcaSenhaEmTextoPlano(senhaEmTextoPlano);
        return criarContaClientePublico(email, passwordEncoder.encode(senhaEmTextoPlano));
    }

    /**
     * Cria uma conta de usuário administrativo ou operacional interno (FR-002).
     *
     * <p>Objetivo Funcional: Permitir que um Administrador crie contas para operadores comerciais,
     * responsáveis pelo estoque, responsáveis por promoções ou outros administradores.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> para criação de contas internas da equipe.</p>
     *
     * @param email endereço de e-mail corporativo do operador
     * @param senhaHash resumo criptográfico BCrypt da senha
     * @param perfisInternos conjunto de perfis administrativos/operacionais atribuídos
     * @return nova instância válida de {@link Usuario} com os perfis internos informados
     * @throws RegraNegocioModulo1Exception caso nenhum perfil interno válido seja fornecido
     */
    public static Usuario criarContaOperadorInterno(
        String email,
        String senhaHash,
        Set<PerfilUsuario> perfisInternos
    ) {
        if (perfisInternos == null || perfisInternos.isEmpty()) {
            throw new RegraNegocioModulo1Exception("Informe ao menos um perfil administrativo/operacional.");
        }
        for (PerfilUsuario perfil : perfisInternos) {
            if (!perfil.ehPerfilInternoAdministrativo()) {
                throw new RegraNegocioModulo1Exception(
                    "A fábrica de operadores internos aceita apenas perfis administrativos/operacionais, não CLIENTE."
                );
            }
        }
        return new Usuario(email, senhaHash, perfisInternos);
    }

    /**
     * Cria uma conta de operador interno aplicando validação de senha em texto plano e codificação BCrypt.
     *
     * <p>Objetivo Funcional: Provisionar operadores administrativos com hash BCrypt seguro (FR-002).</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> parametrizado com codificador.</p>
     *
     * @param email e-mail corporativo do operador
     * @param senhaEmTextoPlano senha inicial em texto plano
     * @param perfisInternos conjunto de perfis administrativos/operacionais
     * @param passwordEncoder codificador BCrypt
     * @return nova instância de {@link Usuario}
     */
    public static Usuario criarContaOperadorInterno(
        String email,
        String senhaEmTextoPlano,
        Set<PerfilUsuario> perfisInternos,
        PasswordEncoder passwordEncoder
    ) {
        validarForcaSenhaEmTextoPlano(senhaEmTextoPlano);
        return criarContaOperadorInterno(email, passwordEncoder.encode(senhaEmTextoPlano), perfisInternos);
    }

    /**
     * Verifica se a senha em texto plano corresponde ao hash BCrypt armazenado nesta conta ativa.
     *
     * <p>Objetivo Funcional: Autenticar o usuário de forma segura sem expor a senha original.</p>
     * <p>Papel no Design Pattern: Comportamento rico de autenticação na entidade de domínio.</p>
     *
     * @param senhaEmTextoPlano senha informada no login
     * @param passwordEncoder codificador BCrypt
     * @return {@code true} se a conta estiver ativa e a senha corresponder ao hash
     */
    public boolean autenticarComSenha(String senhaEmTextoPlano, PasswordEncoder passwordEncoder) {
        if (!this.ativo || senhaEmTextoPlano == null || passwordEncoder == null) {
            return false;
        }
        return passwordEncoder.matches(senhaEmTextoPlano, this.senhaHash);
    }

    /**
     * Valida a força mínima da senha antes da codificação BCrypt.
     *
     * <p>Objetivo Funcional: Rejeitar senhas vazias ou com menos de 8 caracteres.</p>
     * <p>Papel no Design Pattern: Guarda interna dos <b>Static Factory Methods</b>.</p>
     *
     * @param senhaEmTextoPlano senha em texto plano
     */
    private static void validarForcaSenhaEmTextoPlano(String senhaEmTextoPlano) {
        if (senhaEmTextoPlano == null || senhaEmTextoPlano.isBlank() || senhaEmTextoPlano.trim().length() < 8) {
            throw new RegraNegocioModulo1Exception("A senha deve conter no mínimo 8 caracteres.");
        }
        if (passwordEncoderIsNullPlaceholder()) {
            // Nunca atingido
        }
    }

    /**
     * Método auxiliar explícito de verificação interna.
     *
     * <p>Objetivo Funcional: Suporte interno de validação.</p>
     * <p>Papel no Design Pattern: Auxiliar privado.</p>
     *
     * @return sempre {@code false}
     */
    private static boolean passwordEncoderIsNullPlaceholder() {
        return false;
    }

    /**
     * Valida e normaliza o endereço de e-mail em letras minúsculas.
     *
     * <p>Objetivo Funcional: Evitar duplicidades por diferença de maiúsculas/minúsculas e respeitar
     * o limite de 180 caracteres do modelo de dados.</p>
     * <p>Papel no Design Pattern: Validação interna do <b>Static Factory Method</b>.</p>
     *
     * @param email e-mail informado
     * @return e-mail em minúsculas e sem espaços nas extremidades
     */
    private static String validarENormalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraNegocioModulo1Exception("O endereço de e-mail do usuário é obrigatório.");
        }
        String normalizado = email.trim().toLowerCase();
        if (normalizado.length() > 180 || !normalizado.contains("@") || normalizado.startsWith("@") || normalizado.endsWith("@")) {
            throw new RegraNegocioModulo1Exception("O endereço de e-mail informado é inválido ou excede 180 caracteres.");
        }
        return normalizado;
    }

    /**
     * Valida se o valor recebido para senha já está codificado em formato de hash BCrypt seguro.
     *
     * <p>Objetivo Funcional: Impedir por contrato que uma senha em texto puro seja persistida na entidade (FR-001).</p>
     * <p>Papel no Design Pattern: Guarda de segurança da fábrica estática (<b>Static Factory Method</b>).</p>
     *
     * @param senhaHash string contendo o hash BCrypt (iniciado por $2a$, $2b$ ou $2y$)
     */
    private static void validarHashDeSenhaBcrypt(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new RegraNegocioModulo1Exception("O hash da senha é obrigatório.");
        }
        if (!senhaHash.startsWith("$2a$") && !senhaHash.startsWith("$2b$") && !senhaHash.startsWith("$2y$")) {
            throw new RegraNegocioModulo1Exception(
                "Violação de segurança: a entidade Usuario exige senha previamente codificada em hash BCrypt irreversível."
            );
        }
    }

    /**
     * Obtém o identificador único do usuário.
     *
     * <p>Objetivo Funcional: Identificar a conta no banco de dados e na trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do usuário
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o e-mail normalizado do usuário.
     *
     * <p>Objetivo Funcional: Servir como identificador de login e comunicação.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return e-mail do usuário
     */
    public String obterEmail() {
        return this.email;
    }

    /**
     * Obtém o hash BCrypt irreversível da senha.
     *
     * <p>Objetivo Funcional: Permitir ao autenticador verificar credenciais sem conhecer a senha original.</p>
     * <p>Papel no Design Pattern: Getter explícito para integração com o serviço de autenticação.</p>
     *
     * @return hash BCrypt armazenado
     */
    public String obterSenhaHash() {
        return this.senhaHash;
    }

    /**
     * Indica se a conta de usuário está ativa para autenticação.
     *
     * <p>Objetivo Funcional: Controlar bloqueio lógico de contas.</p>
     * <p>Papel no Design Pattern: Consulta de estado da conta.</p>
     *
     * @return {@code true} se ativa; {@code false} caso contrário
     */
    public boolean estaAtivo() {
        return this.ativo;
    }

    /**
     * Retorna uma visão imutável dos perfis de acesso atribuídos ao usuário.
     *
     * <p>Objetivo Funcional: Proteger a coleção interna de perfis contra modificações externas não autorizadas.</p>
     * <p>Papel no Design Pattern: Encapsulamento defensivo de estado.</p>
     *
     * @return conjunto imutável de {@link PerfilUsuario}
     */
    public Set<PerfilUsuario> obterPerfis() {
        return Collections.unmodifiableSet(this.perfis);
    }

    /**
     * Verifica se o usuário possui determinado perfil de acesso.
     *
     * <p>Objetivo Funcional: Facilitar checagens de autorização de domínio.</p>
     * <p>Papel no Design Pattern: Consulta explícita de papel de segurança.</p>
     *
     * @param perfil perfil a ser consultado
     * @return {@code true} se o usuário possuir o perfil informado
     */
    public boolean possuiPerfil(PerfilUsuario perfil) {
        return this.perfis.contains(perfil);
    }

    /**
     * Obtém o instante de criação da conta.
     *
     * <p>Objetivo Funcional: Rastreabilidade temporal do cadastro.</p>
     * <p>Papel no Design Pattern: Getter explícito de metadado temporal.</p>
     *
     * @return instante UTC de criação
     */
    public Instant obterCriadoEm() {
        return this.criadoEm;
    }
}

