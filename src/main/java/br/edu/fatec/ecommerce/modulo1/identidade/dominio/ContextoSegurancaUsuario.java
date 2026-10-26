package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitário explícito de domínio/aplicação para consulta do usuário autenticado na thread atual.
 *
 * <p>Responsabilidade Comercial: Permitir que os Protection &amp; Audit Proxies identifiquem
 * quem está executando uma operação administrativa ou de cliente e quais são seus perfis.</p>
 */
public final class ContextoSegurancaUsuario {

    /**
     * Construtor privado explícito para classe utilitária estática.
     *
     * <p>Objetivo Funcional: Impedir instanciação desnecessária.</p>
     * <p>Papel no Design Pattern: Auxiliar de contexto consumido pelos Proxies de segurança e auditoria.</p>
     */
    private ContextoSegurancaUsuario() {
        // Classe utilitária não instanciável
    }

    /**
     * Obtém o e-mail (username) do usuário autenticado no contexto atual do Spring Security.
     *
     * <p>Objetivo Funcional: Identificar o autor da requisição atual.</p>
     * <p>Papel no Design Pattern: Fonte de identidade para o <b>Proxy</b> de auditoria.</p>
     *
     * @return {@link Optional} contendo o e-mail autenticado ou vazio se anônimo
     */
    public static Optional<String> obterEmailUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    /**
     * Obtém o e-mail (username) do usuário autenticado ou retorna o valor padrão informado.
     *
     * <p>Objetivo Funcional: Fornecer identificação segura do autor mesmo em chamadas internas ou de teste.</p>
     * <p>Papel no Design Pattern: Resolução de identidade para o <b>Proxy</b> e serviços de domínio.</p>
     *
     * @param padraoCasoAnonimo valor padrão caso não haja usuário autenticado na thread
     * @return e-mail do usuário autenticado ou o valor padrão
     */
    public static String obterUsernameOuPadrao(String padraoCasoAnonimo) {
        return obterEmailUsuarioAutenticado().orElse(padraoCasoAnonimo);
    }

    /**
     * Obtém o conjunto de perfis de domínio ({@link PerfilUsuario}) do usuário autenticado.
     *
     * <p>Objetivo Funcional: Verificar se o usuário possui os papéis necessários para a operação.</p>
     * <p>Papel no Design Pattern: Verificação de controle de acesso nos Protection Proxies (<b>Proxy</b>).</p>
     *
     * @return conjunto de perfis associados ao usuário atual
     */
    public static Set<PerfilUsuario> obterPerfisUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Set.of();
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return Set.of();
        }
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(ContextoSegurancaUsuario::converterAuthorityParaPerfil)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Verifica se o usuário autenticado possui pelo menos um dos perfis exigidos.
     *
     * <p>Objetivo Funcional: Validar autorização baseada em papéis (RBAC).</p>
     * <p>Papel no Design Pattern: Guarda de segurança executada pelo <b>Proxy</b> antes de delegar ao RealSubject.</p>
     *
     * @param perfisPermitidos lista de perfis aceitos para a operação
     * @return {@code true} se o usuário possuir ao menos um perfil permitido
     */
    public static boolean possuiAlgumPerfil(PerfilUsuario... perfisPermitidos) {
        Set<PerfilUsuario> atuais = obterPerfisUsuarioAutenticado();
        for (PerfilUsuario permitido : perfisPermitidos) {
            if (atuais.contains(permitido)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve o perfil principal mais privilegiado do usuário atual para gravação em auditoria.
     *
     * <p>Objetivo Funcional: Registrar na trilha de auditoria sob qual papel a operação foi executada.</p>
     * <p>Papel no Design Pattern: Metadado de autoria para o <b>Proxy</b> de auditoria.</p>
     *
     * @param padraoCasoAusente perfil de fallback caso a chamada venha de teste unitário direto
     * @return perfil principal identificado
     */
    public static PerfilUsuario obterPerfilPrincipalOuPadrao(PerfilUsuario padraoCasoAusente) {
        Set<PerfilUsuario> perfis = obterPerfisUsuarioAutenticado();
        if (perfis.contains(PerfilUsuario.ADMIN)) {
            return PerfilUsuario.ADMIN;
        }
        if (perfis.contains(PerfilUsuario.OPERADOR_COMERCIAL)) {
            return PerfilUsuario.OPERADOR_COMERCIAL;
        }
        if (perfis.contains(PerfilUsuario.RESPONSAVEL_ESTOQUE)) {
            return PerfilUsuario.RESPONSAVEL_ESTOQUE;
        }
        if (perfis.contains(PerfilUsuario.RESPONSAVEL_PROMOCOES)) {
            return PerfilUsuario.RESPONSAVEL_PROMOCOES;
        }
        return perfis.stream().findFirst().orElse(padraoCasoAusente);
    }

    /**
     * Gera um UUID determinístico a partir do e-mail autenticado quando o ID de banco não está em cache na sessão.
     *
     * <p>Objetivo Funcional: Garantir que toda entrada de auditoria tenha um UUID válido de autor.</p>
     * <p>Papel no Design Pattern: Resolução de identificador de autor no <b>Proxy</b> de auditoria.</p>
     *
     * @param email e-mail do usuário responsável
     * @return UUID determinístico baseado no e-mail
     */
    public static UUID derivarUuidDeterministicoPorEmail(String email) {
        String normalizado = (email == null || email.isBlank()) ? "sistema@ecommerce.fatec.edu.br" : email.trim().toLowerCase();
        return UUID.nameUUIDFromBytes(normalizado.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converte uma string de autoridade do Spring Security (ex.: "ROLE_ADMIN") para {@link PerfilUsuario}.
     *
     * <p>Objetivo Funcional: Traduzir a nomenclatura do framework para o enum de domínio.</p>
     * <p>Papel no Design Pattern: Método auxiliar privado de tradução de perfil.</p>
     *
     * @param authority string da autoridade
     * @return {@link Optional} com o perfil correspondente, se reconhecido
     */
    private static Optional<PerfilUsuario> converterAuthorityParaPerfil(String authority) {
        if (authority == null) {
            return Optional.empty();
        }
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil.obterAuthoritySpringSecurity().equals(authority) || perfil.name().equals(authority)) {
                return Optional.of(perfil);
            }
        }
        return Optional.empty();
    }
}
