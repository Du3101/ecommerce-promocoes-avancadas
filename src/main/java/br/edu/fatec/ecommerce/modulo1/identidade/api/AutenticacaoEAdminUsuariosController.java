package br.edu.fatec.ecommerce.modulo1.identidade.api;

import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.OpcaoEnderecoEntrega;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST responsável pelos endpoints de auto-cadastro público de clientes,
 * autenticação por credenciais e provisão administrativa de operadores internos (FR-001, FR-002).
 */
@RestController
public class AutenticacaoEAdminUsuariosController {

    private final ServicoIdentidadeECliente servicoIdentidade;

    /**
     * Construtor com injeção explícita do serviço de identidade e clientes.
     *
     * <p>Objetivo Funcional: Conectar a camada HTTP REST aos casos de uso de identidade.</p>
     * <p>Papel no Design Pattern: Controller da camada de apresentação delegando ao serviço de aplicação.</p>
     *
     * @param servicoIdentidade serviço de aplicação de identidade e clientes
     */
    public AutenticacaoEAdminUsuariosController(ServicoIdentidadeECliente servicoIdentidade) {
        this.servicoIdentidade = servicoIdentidade;
    }

    /**
     * Endpoint público para auto-cadastro exclusivo de contas com perfil {@code CLIENTE} (FR-001).
     *
     * <p>Objetivo Funcional: Cadastrar comprador com senha criptografada via BCrypt e inicializar
     * seu perfil comercial com {@code EnderecoAusenteNullObject}.</p>
     * <p>Papel no Design Pattern: Aciona indiretamente o <b>Static Factory Method</b> de {@link Usuario},
     * o <b>Builder</b> de {@link Cliente} e o <b>Null Object</b> de endereço ausente.</p>
     *
     * @param requisicao payload JSON validado com dados do novo cliente
     * @return resposta HTTP 201 Created com dados cadastrais e resumo do endereço inicial
     */
    @PostMapping("/api/v1/auth/clientes/cadastro")
    public ResponseEntity<Map<String, Object>> cadastrarClientePublico(
        @Valid @RequestBody RequisicaoCadastroClienteDTO requisicao
    ) {
        Cliente cliente = this.servicoIdentidade.cadastrarClientePublico(
            requisicao.nomeCompleto(),
            requisicao.email(),
            requisicao.cpf(),
            requisicao.telefone(),
            requisicao.senha()
        );
        OpcaoEnderecoEntrega opcaoEndereco = cliente.obterEnderecoSelecionado();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("clienteId", cliente.obterId());
        resposta.put("usuarioId", cliente.obterUsuarioId());
        resposta.put("nomeCompleto", cliente.obterNomeCompleto());
        resposta.put("email", requisicao.email().trim().toLowerCase());
        resposta.put("cpf", cliente.obterCpf());
        resposta.put("telefone", cliente.obterTelefone());
        resposta.put("perfis", Set.of(PerfilUsuario.CLIENTE));
        resposta.put("primeiraCompraConcluida", cliente.jaConcluiuPrimeiraCompra());
        resposta.put("enderecoSelecionado", Map.of(
            "possuiEnderecoValido", opcaoEndereco.possuiEnderecoValido(),
            "cep", opcaoEndereco.obterCepDestino(),
            "uf", opcaoEndereco.obterUfDestino(),
            "resumoFormatado", opcaoEndereco.obterResumoFormatado()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * Endpoint público de login que valida e-mail e senha contra o hash BCrypt armazenado (FR-001, FR-002).
     *
     * <p>Objetivo Funcional: Autenticar o usuário e retornar seus identificadores e perfis de acesso.</p>
     * <p>Papel no Design Pattern: Entrada de autenticação na API REST.</p>
     *
     * @param requisicao payload com e-mail e senha
     * @return resposta HTTP 200 OK com dados do usuário autenticado ou 401 se inválido
     */
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<Map<String, Object>> realizarLogin(
        @Valid @RequestBody RequisicaoLoginDTO requisicao
    ) {
        Usuario usuario = this.servicoIdentidade.autenticarUsuario(requisicao.email(), requisicao.senha());
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("usuarioId", usuario.obterId());
        resposta.put("email", usuario.obterEmail());
        resposta.put("perfis", usuario.obterPerfis());
        resposta.put("ativo", usuario.estaAtivo());
        resposta.put("autenticado", true);
        return ResponseEntity.ok(resposta);
    }

    /**
     * Endpoint restrito ao perfil {@code ADMIN} para provisionamento de operadores internos (FR-002).
     *
     * <p>Objetivo Funcional: Criar contas para {@code OPERADOR_COMERCIAL}, {@code RESPONSAVEL_ESTOQUE},
     * {@code RESPONSAVEL_PROMOCOES} ou {@code ADMIN} com registro na trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Aciona o <b>Static Factory Method</b>
     * {@link Usuario#criarContaOperadorInterno}.</p>
     *
     * @param requisicao payload com e-mail, senha e perfis do operador
     * @param principal principal autenticado (administrador)
     * @return resposta HTTP 201 Created com dados do operador criado
     */
    @PostMapping("/api/v1/admin/usuarios/operadores")
    public ResponseEntity<Map<String, Object>> provisionarOperador(
        @Valid @RequestBody RequisicaoProvisaoOperadorDTO requisicao,
        Principal principal
    ) {
        String emailExecutor = principal != null ? principal.getName() : "ADMIN_SISTEMA";
        Usuario operador = this.servicoIdentidade.provisionarOperadorInterno(
            requisicao.email(),
            requisicao.senha(),
            requisicao.perfis(),
            emailExecutor
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("usuarioId", operador.obterId());
        resposta.put("email", operador.obterEmail());
        resposta.put("perfis", operador.obterPerfis());
        resposta.put("ativo", operador.estaAtivo());
        resposta.put("criadoEm", operador.obterCriadoEm().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * DTO de requisição para auto-cadastro público de cliente.
     *
     * @param nomeCompleto nome completo do comprador
     * @param email endereço de e-mail
     * @param cpf CPF de 11 dígitos
     * @param telefone telefone de contato
     * @param senha senha em texto plano
     */
    public record RequisicaoCadastroClienteDTO(
        @NotBlank(message = "O nome completo é obrigatório.") String nomeCompleto,
        @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "Formato de e-mail inválido.") String email,
        @NotBlank(message = "O CPF é obrigatório.") String cpf,
        @NotBlank(message = "O telefone é obrigatório.") String telefone,
        @NotBlank(message = "A senha é obrigatória.") String senha
    ) {
    }

    /**
     * DTO de requisição para login.
     *
     * @param email e-mail do usuário
     * @param senha senha em texto plano
     */
    public record RequisicaoLoginDTO(
        @NotBlank(message = "O e-mail é obrigatório.") String email,
        @NotBlank(message = "A senha é obrigatória.") String senha
    ) {
    }

    /**
     * DTO de requisição para criação de operador administrativo/interno.
     *
     * @param email e-mail corporativo do operador
     * @param senha senha inicial
     * @param perfis conjunto de perfis atribuídos
     */
    public record RequisicaoProvisaoOperadorDTO(
        @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "Formato de e-mail inválido.") String email,
        @NotBlank(message = "A senha é obrigatória.") String senha,
        @NotEmpty(message = "Informe ao menos um perfil operacional.") Set<PerfilUsuario> perfis
    ) {
    }
}

