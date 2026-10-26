package br.edu.fatec.ecommerce.modulo1.identidade.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.ConflitoDeEstadoOuDuplicidadeException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ClienteBuilder;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Endereco;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.EnderecoBuilder;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.ClienteJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.UsuarioJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação responsável pelos casos de uso de Identidade, Auto-cadastro de Clientes,
 * Provisão de Operadores Internos e Gestão de Endereços de Entrega (FR-001 a FR-004).
 *
 * <p>Orquestra os padrões <b>Static Factory Method</b> ({@link Usuario} e {@link RegistroAuditoria}),
 * <b>Builder</b> ({@link ClienteBuilder} e {@link EnderecoBuilder}) e <b>Null Object</b>
 * ({@link br.edu.fatec.ecommerce.modulo1.identidade.dominio.EnderecoAusenteNullObject}).</p>
 */
@Service
public class ServicoIdentidadeECliente implements UserDetailsService {

    private final UsuarioJpaRepository usuarioRepository;
    private final ClienteJpaRepository clienteRepository;
    private final RegistroAuditoriaJpaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construtor com injeção explícita das dependências de infraestrutura e criptografia.
     *
     * <p>Objetivo Funcional: Inicializar o serviço de aplicação com acesso aos repositórios e hash BCrypt.</p>
     * <p>Papel no Design Pattern: Injeção de dependências na camada de aplicação (DDD).</p>
     *
     * @param usuarioRepository repositório de contas de usuário
     * @param clienteRepository repositório de perfis de cliente
     * @param auditoriaRepository repositório de auditoria administrativa
     * @param passwordEncoder codificador BCrypt de senhas
     */
    public ServicoIdentidadeECliente(
        UsuarioJpaRepository usuarioRepository,
        ClienteJpaRepository clienteRepository,
        RegistroAuditoriaJpaRepository auditoriaRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carrega os detalhes de autenticação e autoridades RBAC de um usuário pelo e-mail para o Spring Security.
     *
     * <p>Objetivo Funcional: Integrar a entidade {@link Usuario} com o filtro HTTP Basic do Spring Security.</p>
     * <p>Papel no Design Pattern: Adaptador de autenticação para o mecanismo de segurança e RBAC.</p>
     *
     * @param email endereço de e-mail usado como username
     * @return instância de {@link UserDetails} com hash BCrypt e autoridades {@code ROLE_*}
     * @throws UsernameNotFoundException se o usuário não existir ou estiver inativo
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = this.usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado para o e-mail: " + email));

        List<SimpleGrantedAuthority> autoridades = usuario.obterPerfis().stream()
            .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.name()))
            .collect(Collectors.toList());

        return User.builder()
            .username(usuario.obterEmail())
            .password(usuario.obterSenhaHash())
            .disabled(!usuario.estaAtivo())
            .authorities(autoridades)
            .build();
    }

    /**
     * Realiza o auto-cadastro público de um novo comprador com perfil exclusivo {@code CLIENTE} (FR-001).
     *
     * <p>Objetivo Funcional: Criar atomicamente a credencial {@link Usuario} (com senha criptografada via BCrypt)
     * e o perfil {@link Cliente} inicialmente associado ao {@code EnderecoAusenteNullObject}.</p>
     * <p>Papel no Design Pattern: Orquestra o <b>Static Factory Method</b>
     * {@link Usuario#criarContaClientePublico} e o <b>Builder</b> {@link ClienteBuilder}.</p>
     *
     * @param nomeCompleto nome civil completo do comprador
     * @param email endereço de e-mail único
     * @param cpf CPF de 11 dígitos único
     * @param telefone telefone de contato
     * @param senhaEmTextoPlano senha em texto plano para hash BCrypt
     * @return instância persistida de {@link Cliente}
     * @throws ConflitoDeEstadoOuDuplicidadeException se o e-mail ou CPF já estiver cadastrado
     */
    @Transactional
    public Cliente cadastrarClientePublico(
        String nomeCompleto,
        String email,
        String cpf,
        String telefone,
        String senhaEmTextoPlano
    ) {
        if (email != null && this.usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflitoDeEstadoOuDuplicidadeException(
                "Já existe uma conta cadastrada com o e-mail informado: " + email
            );
        }

        // Instancia via Static Factory Method (aplica validação de e-mail, força da senha e hash BCrypt)
        Usuario novoUsuario = Usuario.criarContaClientePublico(email, senhaEmTextoPlano, this.passwordEncoder);

        // Constrói via Builder (valida CPF, nome e telefone)
        Cliente novoCliente = new ClienteBuilder()
            .paraUsuario(novoUsuario.obterId())
            .comNomeCompleto(nomeCompleto)
            .comCpf(cpf)
            .comTelefone(telefone)
            .comPrimeiraCompraConcluida(false)
            .construir();

        if (this.clienteRepository.existsByCpf(novoCliente.obterCpf())) {
            throw new ConflitoDeEstadoOuDuplicidadeException(
                "Já existe um cliente cadastrado com o CPF informado: " + novoCliente.obterCpf()
            );
        }

        this.usuarioRepository.save(novoUsuario);
        return this.clienteRepository.save(novoCliente);
    }

    /**
     * Provisiona uma conta administrativa ou operacional interna (FR-002), restrita ao perfil {@code ADMIN}.
     *
     * <p>Objetivo Funcional: Criar contas para {@code OPERADOR_COMERCIAL}, {@code RESPONSAVEL_ESTOQUE},
     * {@code RESPONSAVEL_PROMOCOES} ou {@code ADMIN} e gravar registro imutável na auditoria.</p>
     * <p>Papel no Design Pattern: Orquestra os <b>Static Factory Methods</b>
     * {@link Usuario#criarContaOperadorInterno} e {@link RegistroAuditoria#registrarCriacaoOperador}.</p>
     *
     * @param email endereço de e-mail corporativo do operador
     * @param senhaEmTextoPlano senha inicial em texto plano
     * @param perfis conjunto de perfis de permissão atribuídos
     * @param emailAdminExecutor e-mail do administrador autenticado que executou a provisão
     * @return conta {@link Usuario} criada e persistida
     */
    @Transactional
    public Usuario provisionarOperadorInterno(
        String email,
        String senhaEmTextoPlano,
        Set<PerfilUsuario> perfis,
        String emailAdminExecutor
    ) {
        if (email != null && this.usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflitoDeEstadoOuDuplicidadeException(
                "Já existe um usuário cadastrado com o e-mail informado: " + email
            );
        }

        Usuario operador = Usuario.criarContaOperadorInterno(
            email,
            senhaEmTextoPlano,
            perfis,
            this.passwordEncoder
        );
        Usuario salvo = this.usuarioRepository.save(operador);

        RegistroAuditoria registroAuditoria = RegistroAuditoria.registrarCriacaoOperador(
            emailAdminExecutor != null ? emailAdminExecutor : "ADMIN_SISTEMA",
            salvo.obterEmail(),
            salvo.obterPerfis().toString()
        );
        this.auditoriaRepository.save(registroAuditoria);

        return salvo;
    }

    /**
     * Valida as credenciais de login (e-mail e senha) de um usuário e retorna sua conta autenticada.
     *
     * <p>Objetivo Funcional: Autenticar o usuário no endpoint {@code POST /api/v1/auth/login} (FR-001, FR-002).</p>
     * <p>Papel no Design Pattern: Verificação explícita de credenciais usando hash BCrypt da entidade.</p>
     *
     * @param email endereço de e-mail informado
     * @param senhaEmTextoPlano senha em texto plano informada
     * @return entidade {@link Usuario} autenticada
     * @throws BadCredentialsException se o e-mail não existir ou a senha não corresponder ao hash
     */
    @Transactional(readOnly = true)
    public Usuario autenticarUsuario(String email, String senhaEmTextoPlano) {
        if (email == null || email.isBlank() || senhaEmTextoPlano == null || senhaEmTextoPlano.isBlank()) {
            throw new BadCredentialsException("E-mail e senha são obrigatórios para autenticação.");
        }
        Usuario usuario = this.usuarioRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new BadCredentialsException("Credenciais de login inválidas."));

        if (!usuario.autenticarComSenha(senhaEmTextoPlano, this.passwordEncoder)) {
            throw new BadCredentialsException("Credenciais de login inválidas.");
        }
        return usuario;
    }

    /**
     * Recupera a conta de usuário associada a um endereço de e-mail.
     *
     * <p>Objetivo Funcional: Obter metadados da conta de usuário a partir do principal autenticado.</p>
     * <p>Papel no Design Pattern: Consulta de aplicação.</p>
     *
     * @param email endereço de e-mail do usuário
     * @return entidade {@link Usuario}
     * @throws RecursoNaoEncontradoException se o usuário não for encontrado
     */
    @Transactional(readOnly = true)
    public Usuario obterUsuarioPorEmail(String email) {
        return this.usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + email));
    }

    /**
     * Recupera o perfil comercial {@link Cliente} a partir do e-mail do usuário autenticado.
     *
     * <p>Objetivo Funcional: Carregar os dados do comprador e sua opção de endereço de entrega.</p>
     * <p>Papel no Design Pattern: Consulta de aplicação que expõe o resultado do padrão <b>Null Object</b>
     * caso o cliente não possua endereço selecionado.</p>
     *
     * @param emailUsuario e-mail do cliente autenticado
     * @return entidade {@link Cliente}
     * @throws RecursoNaoEncontradoException se o perfil de cliente não existir
     */
    @Transactional(readOnly = true)
    public Cliente obterClientePorEmailUsuario(String emailUsuario) {
        Usuario usuario = obterUsuarioPorEmail(emailUsuario);
        return this.clienteRepository.findByUsuarioId(usuario.obterId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Perfil de cliente não encontrado para o usuário: " + emailUsuario
            ));
    }

    /**
     * Recupera o perfil comercial {@link Cliente} pelo seu identificador único (UUID).
     *
     * <p>Objetivo Funcional: Permitir consulta direta do cliente pelas portas de integração com Módulos 2 e 3.</p>
     * <p>Papel no Design Pattern: Consulta de aplicação por ID de agregado.</p>
     *
     * @param clienteId UUID do cliente
     * @return entidade {@link Cliente}
     * @throws RecursoNaoEncontradoException se o cliente não existir
     */
    @Transactional(readOnly = true)
    public Cliente obterClientePorId(UUID clienteId) {
        return this.clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com ID: " + clienteId));
    }

    /**
     * Constrói e adiciona um novo endereço de entrega ao perfil do cliente autenticado (FR-003).
     *
     * <p>Objetivo Funcional: Permitir que o cliente cadastre múltiplos endereços e opcionalmente já o
     * selecione para entrega.</p>
     * <p>Papel no Design Pattern: Utiliza o padrão <b>Builder</b> ({@link EnderecoBuilder}) para validar
     * CEP, UF e demais campos antes de anexar ao agregado {@link Cliente}.</p>
     *
     * @param emailUsuario e-mail do cliente autenticado
     * @param apelido apelido identificador (ex.: "Casa", "Trabalho")
     * @param cep CEP numérico de 8 dígitos
     * @param logradouro nome da rua/avenida
     * @param numero número do imóvel
     * @param complemento complemento opcional
     * @param bairro bairro
     * @param cidade município
     * @param uf sigla da UF com 2 letras
     * @param selecionarParaEntrega se {@code true}, torna este endereço o selecionado para entrega
     * @return instância de {@link Endereco} recém-adicionada
     */
    @Transactional
    public Endereco adicionarEnderecoAoCliente(
        String emailUsuario,
        String apelido,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        boolean selecionarParaEntrega
    ) {
        Cliente cliente = obterClientePorEmailUsuario(emailUsuario);
        Endereco novoEndereco = new EnderecoBuilder()
            .paraCliente(cliente.obterId())
            .comApelido(apelido)
            .comCep(cep)
            .comLogradouro(logradouro)
            .comNumero(numero)
            .comComplemento(complemento)
            .comBairro(bairro)
            .comCidade(cidade)
            .comUf(uf)
            .marcarComoSelecionadoParaEntrega(selecionarParaEntrega)
            .construir();

        cliente.adicionarEndereco(novoEndereco);
        this.clienteRepository.save(cliente);
        return novoEndereco;
    }

    /**
     * Define qual endereço existente do cliente será usado para entrega atual (FR-003).
     *
     * <p>Objetivo Funcional: Alternar o endereço selecionado, substituindo o retorno de
     * {@code EnderecoAusenteNullObject} pelo endereço escolhido.</p>
     * <p>Papel no Design Pattern: Transição do estado de endereço ativo no agregado {@link Cliente}.</p>
     *
     * @param emailUsuario e-mail do cliente autenticado
     * @param enderecoId UUID do endereço a selecionar
     * @return cliente atualizado com o novo endereço selecionado
     */
    @Transactional
    public Cliente selecionarEnderecoDoCliente(String emailUsuario, UUID enderecoId) {
        Cliente cliente = obterClientePorEmailUsuario(emailUsuario);
        cliente.selecionarEnderecoParaEntrega(enderecoId);
        return this.clienteRepository.save(cliente);
    }

    /**
     * Remove um endereço da conta do cliente autenticado (FR-003, FR-004).
     *
     * <p>Objetivo Funcional: Excluir endereço; caso fosse o endereço selecionado, o cliente volta a
     * retornar {@code EnderecoAusenteNullObject} sem referências nulas.</p>
     * <p>Papel no Design Pattern: Garante retorno seguro ao estado <b>Null Object</b> após remoção.</p>
     *
     * @param emailUsuario e-mail do cliente autenticado
     * @param enderecoId UUID do endereço a remover
     */
    @Transactional
    public void removerEnderecoDoCliente(String emailUsuario, UUID enderecoId) {
        Cliente cliente = obterClientePorEmailUsuario(emailUsuario);
        cliente.removerEndereco(enderecoId);
        this.clienteRepository.save(cliente);
    }
}

