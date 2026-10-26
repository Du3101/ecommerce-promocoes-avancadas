package br.edu.fatec.ecommerce.modulo1.identidade.api;

import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Endereco;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.OpcaoEnderecoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consulta do perfil do cliente autenticado e gerenciamento
 * de seus múltiplos endereços de entrega (FR-003, FR-004).
 *
 * <p>Demonstra na API REST o comportamento polimórfico do padrão <b>Null Object</b>
 * ({@link br.edu.fatec.ecommerce.modulo1.identidade.dominio.EnderecoAusenteNullObject}) quando
 * nenhum endereço de entrega está selecionado.</p>
 */
@RestController
@RequestMapping("/api/v1/clientes/me")
public class ClienteEnderecosController {

    private final ServicoIdentidadeECliente servicoIdentidade;

    /**
     * Construtor com injeção explícita do serviço de identidade e clientes.
     *
     * <p>Objetivo Funcional: Permitir manipulação dos endereços do cliente autenticado.</p>
     * <p>Papel no Design Pattern: Controller REST integrado ao agregado {@link Cliente}.</p>
     *
     * @param servicoIdentidade serviço de aplicação de identidade e clientes
     */
    public ClienteEnderecosController(ServicoIdentidadeECliente servicoIdentidade) {
        this.servicoIdentidade = servicoIdentidade;
    }

    /**
     * Retorna os dados do cliente autenticado, seus endereços e a opção de endereço selecionada (FR-004).
     *
     * <p>Objetivo Funcional: Exibir perfil do cliente sem jamais retornar {@code null} no campo
     * {@code enderecoSelecionado}.</p>
     * <p>Papel no Design Pattern: Consome {@link OpcaoEnderecoEntrega} polimorficamente via padrão
     * <b>Null Object</b>.</p>
     *
     * @param principal principal autenticado (e-mail do cliente)
     * @return resposta HTTP 200 OK com dados do perfil e endereço selecionado
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> consultarMeuPerfil(Principal principal) {
        Cliente cliente = this.servicoIdentidade.obterClientePorEmailUsuario(principal.getName());
        return ResponseEntity.ok(montarMapaPerfilCliente(cliente, principal.getName()));
    }

    /**
     * Adiciona um novo endereço de entrega à conta do cliente autenticado (FR-003).
     *
     * <p>Objetivo Funcional: Cadastrar endereço validado por {@code EnderecoBuilder}.</p>
     * <p>Papel no Design Pattern: Dispara construção do endereço via padrão <b>Builder</b>.</p>
     *
     * @param requisicao payload com campos do endereço
     * @param principal principal autenticado
     * @return resposta HTTP 201 Created com os dados do endereço criado
     */
    @PostMapping("/enderecos")
    public ResponseEntity<Map<String, Object>> adicionarEndereco(
        @Valid @RequestBody RequisicaoEnderecoDTO requisicao,
        Principal principal
    ) {
        Endereco endereco = this.servicoIdentidade.adicionarEnderecoAoCliente(
            principal.getName(),
            requisicao.apelido(),
            requisicao.cep(),
            requisicao.logradouro(),
            requisicao.numero(),
            requisicao.complemento(),
            requisicao.bairro(),
            requisicao.cidade(),
            requisicao.uf(),
            requisicao.selecionarParaEntrega()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(montarMapaEndereco(endereco));
    }

    /**
     * Seleciona um endereço específico do cliente como endereço ativo para entrega (FR-003).
     *
     * <p>Objetivo Funcional: Alternar o endereço de entrega ativo.</p>
     * <p>Papel no Design Pattern: Atualiza a seleção ativa no agregado {@link Cliente}.</p>
     *
     * @param enderecoId UUID do endereço a selecionar
     * @param principal principal autenticado
     * @return resposta HTTP 200 OK com o perfil atualizado do cliente
     */
    @PutMapping("/enderecos/{enderecoId}/selecionar")
    public ResponseEntity<Map<String, Object>> selecionarEnderecoParaEntrega(
        @PathVariable UUID enderecoId,
        Principal principal
    ) {
        Cliente cliente = this.servicoIdentidade.selecionarEnderecoDoCliente(principal.getName(), enderecoId);
        return ResponseEntity.ok(montarMapaPerfilCliente(cliente, principal.getName()));
    }

    /**
     * Remove um endereço da conta do cliente autenticado (FR-003, FR-004).
     *
     * <p>Objetivo Funcional: Excluir endereço; caso fosse o selecionado, o cliente volta a retornar
     * {@code EnderecoAusenteNullObject}.</p>
     * <p>Papel no Design Pattern: Preserva o contrato <b>Null Object</b> após exclusão.</p>
     *
     * @param enderecoId UUID do endereço a remover
     * @param principal principal autenticado
     * @return resposta HTTP 204 No Content
     */
    @DeleteMapping("/enderecos/{enderecoId}")
    public ResponseEntity<Void> removerEndereco(
        @PathVariable UUID enderecoId,
        Principal principal
    ) {
        this.servicoIdentidade.removerEnderecoDoCliente(principal.getName(), enderecoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converte o agregado {@link Cliente} em mapa JSON estruturado para resposta REST.
     *
     * <p>Objetivo Funcional: Formatar os dados do cliente e seu {@link OpcaoEnderecoEntrega}.</p>
     * <p>Papel no Design Pattern: Serialização uniforme de {@link Endereco} real ou
     * {@code EnderecoAusenteNullObject} sem necessidade de {@code if (endereco != null)}.</p>
     *
     * @param cliente agregado Cliente
     * @param email e-mail do cliente
     * @return mapa pronto para serialização JSON
     */
    private Map<String, Object> montarMapaPerfilCliente(Cliente cliente, String email) {
        OpcaoEnderecoEntrega selecionado = cliente.obterEnderecoSelecionado();
        List<Map<String, Object>> listaEnderecos = cliente.obterEnderecos().stream()
            .map(this::montarMapaEndereco)
            .collect(Collectors.toList());

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("clienteId", cliente.obterId());
        mapa.put("nomeCompleto", cliente.obterNomeCompleto());
        mapa.put("email", email);
        mapa.put("cpf", cliente.obterCpf());
        mapa.put("telefone", cliente.obterTelefone());
        mapa.put("primeiraCompraConcluida", cliente.jaConcluiuPrimeiraCompra());
        mapa.put("enderecoSelecionado", Map.of(
            "possuiEnderecoValido", selecionado.possuiEnderecoValido(),
            "cep", selecionado.obterCepDestino(),
            "uf", selecionado.obterUfDestino(),
            "resumoFormatado", selecionado.obterResumoFormatado()
        ));
        mapa.put("enderecos", listaEnderecos);
        return mapa;
    }

    /**
     * Converte uma entidade {@link Endereco} em mapa JSON estruturado.
     *
     * <p>Objetivo Funcional: Expor todos os atributos do endereço cadastrado.</p>
     * <p>Papel no Design Pattern: Método auxiliar de apresentação.</p>
     *
     * @param endereco entidade Endereco
     * @return mapa JSON com dados do endereço
     */
    private Map<String, Object> montarMapaEndereco(Endereco endereco) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", endereco.obterId());
        mapa.put("apelido", endereco.obterApelido());
        mapa.put("cep", endereco.obterCepDestino());
        mapa.put("logradouro", endereco.obterLogradouro());
        mapa.put("numero", endereco.obterNumero());
        mapa.put("complemento", endereco.obterComplemento());
        mapa.put("bairro", endereco.obterBairro());
        mapa.put("cidade", endereco.obterCidade());
        mapa.put("uf", endereco.obterUfDestino());
        mapa.put("selecionadoParaEntrega", endereco.estaSelecionadoParaEntrega());
        mapa.put("resumoFormatado", endereco.obterResumoFormatado());
        return mapa;
    }

    /**
     * DTO de requisição para cadastro de endereço de entrega do cliente.
     *
     * @param apelido nome identificador do endereço
     * @param cep CEP numérico de 8 dígitos
     * @param logradouro rua/avenida
     * @param numero número do imóvel
     * @param complemento complemento opcional
     * @param bairro bairro
     * @param cidade cidade
     * @param uf sigla do estado (2 letras)
     * @param selecionarParaEntrega flag para marcar imediatamente como endereço selecionado
     */
    public record RequisicaoEnderecoDTO(
        @NotBlank(message = "O apelido do endereço é obrigatório.") String apelido,
        @NotBlank(message = "O CEP é obrigatório.") String cep,
        @NotBlank(message = "O logradouro é obrigatório.") String logradouro,
        @NotBlank(message = "O número é obrigatório.") String numero,
        String complemento,
        @NotBlank(message = "O bairro é obrigatório.") String bairro,
        @NotBlank(message = "A cidade é obrigatória.") String cidade,
        @NotBlank(message = "A UF é obrigatória.") String uf,
        boolean selecionarParaEntrega
    ) {
    }
}

