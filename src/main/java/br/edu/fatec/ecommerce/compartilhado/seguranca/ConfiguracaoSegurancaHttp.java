package br.edu.fatec.ecommerce.compartilhado.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração global de Segurança HTTP, Hash de Senhas e Autorização por Perfil (RBAC).
 *
 * <p>Responsabilidade Comercial: Proteger todas as rotas da API REST conforme os perfis
 * de usuário definidos em FR-001 e FR-002 e fornecer o codificador BCrypt irreversível.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracaoSegurancaHttp {

    /**
     * Registra o codificador irreversível de senhas baseado no algoritmo BCrypt (FR-001).
     *
     * <p>Objetivo Funcional: Garantir que nenhuma senha seja armazenada ou comparada em texto puro.</p>
     * <p>Papel no Design Pattern: Fábrica de componente de criptografia injetado nos serviços de identidade.</p>
     *
     * @return instância de {@link BCryptPasswordEncoder} com fator de custo seguro (10)
     */
    @Bean
    public PasswordEncoder codificadorDeSenhaBcrypt() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Configura a cadeia de filtros de segurança HTTP e as regras de autorização por rota e perfil.
     *
     * <p>Objetivo Funcional: Liberar o auto-cadastro público de clientes e a consulta pública do catálogo,
     * restringindo rotas de clientes, catálogo administrativo, estoque e auditoria aos respectivos perfis.</p>
     * <p>Papel no Design Pattern: Cadeia de filtros de segurança na borda HTTP que complementa os
     * Protection Proxies da camada de aplicação.</p>
     *
     * @param http objeto builder de segurança HTTP do Spring Security
     * @return cadeia de filtros configurada ({@link SecurityFilterChain})
     * @throws Exception caso ocorra erro na construção da cadeia
     */
    @Bean
    public SecurityFilterChain cadeiaDeFiltrosSeguranca(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                // Documentação OpenAPI/Swagger e console H2 de desenvolvimento
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**").permitAll()
                // Rotas públicas de cadastro de cliente e login
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/clientes/cadastro", "/api/v1/auth/login").permitAll()
                // Rotas públicas de consulta ao catálogo e estoque informativo
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**", "/api/v1/estoque/**").permitAll()
                // Rotas exclusivas do próprio Cliente autenticado
                .requestMatchers("/api/v1/clientes/me/**").hasRole("CLIENTE")
                // Gestão de operadores e consulta de auditoria: exclusivo ADMIN
                .requestMatchers("/api/v1/admin/usuarios/**", "/api/v1/admin/auditoria/**").hasRole("ADMIN")
                // Administração de catálogo (categorias, produtos, variações, preços, combos): ADMIN ou OPERADOR_COMERCIAL
                .requestMatchers("/api/v1/admin/catalogo/**").hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                // Administração de estoque (ajustes manuais e expiração de reservas): ADMIN ou RESPONSAVEL_ESTOQUE
                .requestMatchers("/api/v1/admin/estoque/**").hasAnyRole("ADMIN", "RESPONSAVEL_ESTOQUE")
                // Operações de reserva/confirmação/liberação de estoque no checkout
                .requestMatchers("/api/v1/estoque/reservas/**").hasAnyRole("CLIENTE", "ADMIN", "RESPONSAVEL_ESTOQUE")
                // Qualquer outra rota exige autenticação
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

