package br.edu.fatec.ecommerce.compartilhado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração global de Segurança HTTP, Hash de Senhas e Autorização por Perfil (RBAC).
 *
 * <p>Responsabilidade Comercial: Proteger todas as rotas da API REST conforme os perfis
 * de usuário definidos em FR-001 e FR-002, fornecer o codificador BCrypt irreversível e liberar
 * o portal web estático servido em {@code /}.</p>
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
     * Registra o ponto de entrada customizado de autenticação REST que omite o cabeçalho
     * {@code WWW-Authenticate} para evitar caixas de diálogo nativas do navegador.
     *
     * <p>Objetivo Funcional: Padronizar respostas 401 em JSON em toda chamada {@code fetch()} do portal web.</p>
     * <p>Papel no Design Pattern: Fábrica de {@link AuthenticationEntryPoint} customizado da borda HTTP.</p>
     *
     * @param objectMapper serializador JSON gerenciado pelo Spring Boot
     * @return instância configurada de {@link EntradaAutenticacaoRestSemPopup}
     */
    @Bean
    public AuthenticationEntryPoint entradaAutenticacaoRestSemPopup(ObjectMapper objectMapper) {
        return new EntradaAutenticacaoRestSemPopup(objectMapper);
    }

    /**
     * Configura a cadeia de filtros de segurança HTTP e as regras de autorização por rota e perfil.
     *
     * <p>Objetivo Funcional: Liberar os recursos estáticos do portal web, endpoints públicos de vitrine
     * e auto-cadastro, restringindo rotas autenticadas por perfil RBAC e configurando resposta 401
     * JSON sem popup nativo.</p>
     * <p>Papel no Design Pattern: Cadeia de filtros de segurança na borda HTTP que complementa os
     * Protection Proxies da camada de aplicação.</p>
     *
     * @param http objeto builder de segurança HTTP do Spring Security
     * @param entradaAutenticacao point de resposta 401 sem cabeçalho {@code WWW-Authenticate}
     * @return cadeia de filtros configurada ({@link SecurityFilterChain})
     * @throws Exception caso ocorra erro na construção da cadeia
     */
    @Bean
    public SecurityFilterChain cadeiaDeFiltrosSeguranca(
        HttpSecurity http,
        AuthenticationEntryPoint entradaAutenticacao
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                // Portal web estático servido diretamente pelo Spring Boot na raiz
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                // Documentação OpenAPI/Swagger e console H2 de desenvolvimento
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**").permitAll()
                // Rotas públicas de cadastro de cliente e login
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/clientes/cadastro", "/api/v1/auth/login").permitAll()
                // Rotas públicas de consulta ao catálogo, estoque, promoções ativas e configuração da vitrine
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/catalogo/**",
                    "/api/v1/estoque/**",
                    "/api/v1/promocoes/ativas",
                    "/api/v1/loja/configuracao"
                ).permitAll()
                // Mesclagem de carrinho visitante ao logar exige cliente autenticado (regra antes de /carrinho/**)
                .requestMatchers(HttpMethod.POST, "/api/v1/carrinho/mesclar").hasRole("CLIENTE")
                // Rotas públicas de consulta e manipulação de carrinho (Módulo 2)
                .requestMatchers("/api/v1/carrinho/**").permitAll()
                // Rotas exclusivas do próprio Cliente autenticado
                .requestMatchers("/api/v1/clientes/me/**").hasRole("CLIENTE")
                // Gestão de operadores e consulta de auditoria: exclusivo ADMIN
                .requestMatchers("/api/v1/admin/usuarios/**", "/api/v1/admin/auditoria/**").hasRole("ADMIN")
                // Administração de catálogo (categorias, produtos, variações, preços, combos): ADMIN ou OPERADOR_COMERCIAL
                .requestMatchers("/api/v1/admin/catalogo/**").hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                // Administração de promoções e cupons (Módulo 2): ADMIN ou RESPONSAVEL_PROMOCOES
                .requestMatchers("/api/v1/admin/promocoes/**", "/api/v1/admin/cupons/**").hasAnyRole("ADMIN", "RESPONSAVEL_PROMOCOES")
                // Administração de estoque (ajustes manuais e expiração de reservas): ADMIN ou RESPONSAVEL_ESTOQUE
                .requestMatchers("/api/v1/admin/estoque/**").hasAnyRole("ADMIN", "RESPONSAVEL_ESTOQUE")
                // Operações de reserva/confirmação/liberação de estoque no checkout
                .requestMatchers("/api/v1/estoque/reservas/**").hasAnyRole("CLIENTE", "ADMIN", "RESPONSAVEL_ESTOQUE")
                // Módulo 3 — webhook do gateway (autenticado por X-Gateway-Token) e simulação de frete: públicos
                .requestMatchers(HttpMethod.POST, "/api/v1/pagamentos/confirmacao", "/api/v1/frete/calcular").permitAll()
                // Módulo 3 — checkout e histórico próprio: somente clientes
                .requestMatchers(HttpMethod.POST, "/api/v1/checkout").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/me").hasRole("CLIENTE")
                // Módulo 3 — detalhe, simulação demo e cancelamento: cliente (dono) ou ADMIN
                .requestMatchers("/api/v1/pedidos/**").hasAnyRole("CLIENTE", "ADMIN")
                // Módulo 3 — expedição: ADMIN ou OPERADOR_COMERCIAL
                .requestMatchers("/api/v1/admin/pedidos/**").hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                // Módulo 3 — relatórios gerenciais: exclusivo ADMIN
                .requestMatchers("/api/v1/admin/relatorios/**").hasRole("ADMIN")
                // Qualquer outra rota exige autenticação
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(entradaAutenticacao))
            .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(entradaAutenticacao));

        return http.build();
    }
}
