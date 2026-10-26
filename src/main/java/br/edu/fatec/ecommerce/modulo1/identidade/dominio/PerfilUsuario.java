package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

/**
 * Perfis de acesso e autorização suportados pelo sistema de e-commerce (FR-002).
 *
 * <p>Responsabilidade Comercial: Segregar rigorosamente as permissões entre compradores
 * (clientes) e os diferentes papéis administrativos/operacionais da loja.</p>
 */
public enum PerfilUsuario {

    /**
     * Comprador da loja virtual; único perfil atribuído no auto-cadastro público (FR-001).
     */
    CLIENTE("ROLE_CLIENTE", false),

    /**
     * Administrador geral com permissão total sobre operadores, catálogo, estoque e auditoria.
     */
    ADMIN("ROLE_ADMIN", true),

    /**
     * Operador responsável pela gestão de produtos, variações, categorias, preços e combos.
     */
    OPERADOR_COMERCIAL("ROLE_OPERADOR_COMERCIAL", true),

    /**
     * Operador responsável por ajustes físicos de estoque e expiração de reservas vencidas.
     */
    RESPONSAVEL_ESTOQUE("ROLE_RESPONSAVEL_ESTOQUE", true),

    /**
     * Operador responsável pela configuração de campanhas promocionais e cupons (Módulo 2).
     */
    RESPONSAVEL_PROMOCOES("ROLE_RESPONSAVEL_PROMOCOES", true);

    private final String authoritySpringSecurity;
    private final boolean perfilInternoAdministrativo;

    /**
     * Construtor explícito do enum de perfil de usuário.
     *
     * <p>Objetivo Funcional: Associar cada perfil ao seu identificador de Role do Spring Security
     * e indicar se se trata de um perfil interno da loja.</p>
     * <p>Papel no Design Pattern: Suporte de metadados de domínio para os Protection Proxies (<b>Proxy</b>).</p>
     *
     * @param authoritySpringSecurity nome da autoridade com prefixo ROLE_
     * @param perfilInternoAdministrativo verdadeiro se for um perfil administrativo/operacional interno
     */
    PerfilUsuario(String authoritySpringSecurity, boolean perfilInternoAdministrativo) {
        this.authoritySpringSecurity = authoritySpringSecurity;
        this.perfilInternoAdministrativo = perfilInternoAdministrativo;
    }

    /**
     * Retorna a string de autoridade compatível com o Spring Security (ex.: "ROLE_ADMIN").
     *
     * <p>Objetivo Funcional: Integrar o perfil de domínio às regras de autorização HTTP.</p>
     * <p>Papel no Design Pattern: Adaptador de representação de perfil para segurança.</p>
     *
     * @return nome da autoridade precedido por ROLE_
     */
    public String obterAuthoritySpringSecurity() {
        return this.authoritySpringSecurity;
    }

    /**
     * Indica se este perfil pertence ao quadro interno administrativo/operacional da empresa.
     *
     * <p>Objetivo Funcional: Impedir que auto-cadastros públicos assumam perfis internos (FR-001).</p>
     * <p>Papel no Design Pattern: Regra de guarda utilizada pelos <b>Static Factory Methods</b> de Usuario.</p>
     *
     * @return {@code true} se for perfil administrativo ou operacional; {@code false} se for CLIENTE
     */
    public boolean ehPerfilInternoAdministrativo() {
        return this.perfilInternoAdministrativo;
    }
}

