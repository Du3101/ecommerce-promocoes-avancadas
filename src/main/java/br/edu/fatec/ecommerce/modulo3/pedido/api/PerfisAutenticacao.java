package br.edu.fatec.ecommerce.modulo3.pedido.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Utilitário da camada HTTP para ler perfis do usuário autenticado no Spring Security.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public final class PerfisAutenticacao {

    /** Authority do perfil administrador no Spring Security. */
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Classe utilitária: não instanciável.
     */
    private PerfisAutenticacao() {
    }

    /**
     * Indica se o usuário autenticado tem o perfil ADMIN.
     *
     * @param autenticacao autenticação corrente
     * @return {@code true} se for administrador
     */
    public static boolean ehAdministrador(Authentication autenticacao) {
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }
}
