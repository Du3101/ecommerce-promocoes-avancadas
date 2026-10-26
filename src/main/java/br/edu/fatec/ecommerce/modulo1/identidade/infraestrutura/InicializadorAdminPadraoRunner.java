package br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador de bootstrap que garante a existência de uma conta {@code ADMIN} inicial
 * no banco de dados ao subir a aplicação (Clarificação Q1 / FR-002).
 *
 * <p>Credenciais padrão de desenvolvimento/avaliação:
 * {@code admin@ecommerce.fatec.edu.br} / {@code AdminFatec@2026}.</p>
 */
@Component
@Order(1)
public class InicializadorAdminPadraoRunner implements CommandLineRunner {

    /** E-mail padrão do administrador semente do sistema. */
    public static final String EMAIL_ADMIN_PADRAO = "admin@ecommerce.fatec.edu.br";

    /** Senha padrão do administrador semente do sistema. */
    public static final String SENHA_ADMIN_PADRAO = "AdminFatec@2026";

    private final UsuarioJpaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construtor com injeção do repositório de usuários e codificador BCrypt.
     *
     * <p>Objetivo Funcional: Permitir verificação e criação idempotente do usuário ADMIN no startup.</p>
     * <p>Papel no Design Pattern: Componente de inicialização que utiliza o <b>Static Factory Method</b>
     * de {@link Usuario}.</p>
     *
     * @param usuarioRepository repositório JPA de usuários
     * @param passwordEncoder codificador BCrypt
     */
    public InicializadorAdminPadraoRunner(
        UsuarioJpaRepository usuarioRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Executa a verificação e criação idempotente da conta administrativa inicial na subida do Spring Boot.
     *
     * <p>Objetivo Funcional: Assegurar que exista um administrador capaz de provisionar os demais
     * operadores internos ({@code OPERADOR_COMERCIAL}, {@code RESPONSAVEL_ESTOQUE}, etc.).</p>
     * <p>Papel no Design Pattern: Invoca {@link Usuario#criarContaOperadorInterno} (<b>Static Factory Method</b>).</p>
     *
     * @param args argumentos de linha de comando
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (!this.usuarioRepository.existsByEmailIgnoreCase(EMAIL_ADMIN_PADRAO)) {
            Usuario adminInicial = Usuario.criarContaOperadorInterno(
                EMAIL_ADMIN_PADRAO,
                SENHA_ADMIN_PADRAO,
                Set.of(PerfilUsuario.ADMIN),
                this.passwordEncoder
            );
            this.usuarioRepository.save(adminInicial);
        }
    }
}

