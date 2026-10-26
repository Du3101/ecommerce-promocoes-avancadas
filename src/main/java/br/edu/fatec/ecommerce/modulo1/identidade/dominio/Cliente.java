package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado de domínio que representa o perfil comercial de um cliente comprador (FR-001, FR-003, FR-004).
 *
 * <p>Responsabilidade Comercial: Manter os dados cadastrais do comprador, o indicador de primeira
 * compra e a coleção de endereços de entrega, retornando sempre um {@link OpcaoEnderecoEntrega}
 * válido ou {@link EnderecoAusenteNullObject} (padrão <b>Null Object</b>) sem referências nulas.</p>
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, unique = true, updatable = false)
    private UUID usuarioId;

    @Column(name = "nome_completo", length = 150, nullable = false)
    private String nomeCompleto;

    @Column(name = "cpf", length = 11, nullable = false, unique = true, updatable = false)
    private String cpf;

    @Column(name = "telefone", length = 20, nullable = false)
    private String telefone;

    @Column(name = "primeira_compra_concluida", nullable = false)
    private boolean primeiraCompraConcluida;

    @OneToMany(mappedBy = "clienteId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Endereco> enderecos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a instanciação direta em favor do {@link ClienteBuilder} (<b>Builder</b>).</p>
     */
    protected Cliente() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor de pacote invocado exclusivamente pelo {@link ClienteBuilder}.
     *
     * <p>Objetivo Funcional: Inicializar o agregado Cliente com dados previamente validados.</p>
     * <p>Papel no Design Pattern: Produto concreto construído pelo padrão <b>Builder</b>.</p>
     *
     * @param id identificador único do cliente
     * @param usuarioId identificador da conta Usuario associada
     * @param nomeCompleto nome completo validado
     * @param cpf CPF numérico de 11 dígitos validado
     * @param telefone telefone de contato
     * @param primeiraCompraConcluida flag indicando se já realizou compra anterior
     */
    Cliente(
        UUID id,
        UUID usuarioId,
        String nomeCompleto,
        String cpf,
        String telefone,
        boolean primeiraCompraConcluida
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nomeCompleto = nomeCompleto;
        this.cpf = cpf;
        this.telefone = telefone;
        this.primeiraCompraConcluida = primeiraCompraConcluida;
        this.criadoEm = Instant.now();
    }

    /**
     * Adiciona um novo endereço à conta do cliente, ajustando a seleção de entrega caso necessário.
     *
     * <p>Objetivo Funcional: Permitir o cadastro de múltiplos endereços pelo cliente (FR-003),
     * garantindo que no máximo um endereço esteja marcado como selecionado.</p>
     * <p>Papel no Design Pattern: Método de gestão do agregado que preserva a invariante de seleção única.</p>
     *
     * @param novoEndereco endereço construído via {@link EnderecoBuilder}
     */
    public void adicionarEndereco(Endereco novoEndereco) {
        if (novoEndereco.estaSelecionadoParaEntrega()) {
            // Desmarca quaisquer endereços anteriores para garantir seleção única
            for (Endereco existente : this.enderecos) {
                existente.definirSelecionadoParaEntrega(false);
            }
        }
        this.enderecos.add(novoEndereco);
    }

    /**
     * Seleciona qual endereço da lista do cliente será utilizado para a próxima entrega (FR-003).
     *
     * <p>Objetivo Funcional: Alternar o endereço ativo de entrega do cliente.</p>
     * <p>Papel no Design Pattern: Transição do estado interno de seleção que substitui o
     * {@link EnderecoAusenteNullObject} pelo {@link Endereco} real escolhido.</p>
     *
     * @param enderecoId UUID do endereço pertencente ao cliente
     * @throws RecursoNaoEncontradoException caso o endereço não pertença a este cliente
     */
    public void selecionarEnderecoParaEntrega(UUID enderecoId) {
        boolean encontrado = false;
        for (Endereco endereco : this.enderecos) {
            if (endereco.obterId().equals(enderecoId)) {
                endereco.definirSelecionadoParaEntrega(true);
                encontrado = true;
            } else {
                endereco.definirSelecionadoParaEntrega(false);
            }
        }
        if (!encontrado) {
            throw new RecursoNaoEncontradoException(
                "Endereço de entrega não encontrado na conta do cliente: " + enderecoId
            );
        }
    }

    /**
     * Remove um endereço da conta do cliente pelo seu identificador.
     *
     * <p>Objetivo Funcional: Permitir exclusão de endereços antigos; caso o endereço removido fosse
     * o selecionado, o cliente passa automaticamente a retornar {@link EnderecoAusenteNullObject}.</p>
     * <p>Papel no Design Pattern: Operação do agregado integrada ao padrão <b>Null Object</b>.</p>
     *
     * @param enderecoId UUID do endereço a ser removido
     * @throws RecursoNaoEncontradoException caso o endereço não exista na conta do cliente
     */
    public void removerEndereco(UUID enderecoId) {
        boolean removido = this.enderecos.removeIf(e -> e.obterId().equals(enderecoId));
        if (!removido) {
            throw new RecursoNaoEncontradoException(
                "Endereço de entrega não encontrado na conta do cliente: " + enderecoId
            );
        }
    }

    /**
     * Obtém a opção atual de endereço de entrega do cliente sem jamais retornar {@code null} (FR-004).
     *
     * <p>Objetivo Funcional: Fornecer o endereço ativo de entrega ou o objeto neutro de ausência
     * para o perfil, carrinho, promoções e cálculo de frete.</p>
     * <p>Papel no Design Pattern: Ponto de entrega polimórfica do padrão <b>Null Object</b>
     * ({@link Endereco} real ou {@link EnderecoAusenteNullObject}).</p>
     *
     * @return instância de {@link OpcaoEnderecoEntrega} (nunca {@code null})
     */
    public OpcaoEnderecoEntrega obterEnderecoSelecionado() {
        for (Endereco endereco : this.enderecos) {
            if (endereco.estaSelecionadoParaEntrega()) {
                return endereco;
            }
        }
        return EnderecoAusenteNullObject.obterInstancia();
    }

    /**
     * Registra que o cliente concluiu sua primeira compra com sucesso.
     *
     * <p>Objetivo Funcional: Atualizar elegibilidade para promoções do tipo "10% na primeira compra".</p>
     * <p>Papel no Design Pattern: Atualização explícita de estado de domínio.</p>
     */
    public void marcarPrimeiraCompraConcluida() {
        this.primeiraCompraConcluida = true;
    }

    /**
     * Obtém o identificador único do cliente.
     *
     * <p>Objetivo Funcional: Identificar o comprador em carrinhos, reservas e pedidos.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do cliente
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o identificador da conta de usuário vinculada ao cliente.
     *
     * <p>Objetivo Funcional: Associar o perfil comercial à credencial de autenticação.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do usuário
     */
    public UUID obterUsuarioId() {
        return this.usuarioId;
    }

    /**
     * Obtém o nome completo do cliente.
     *
     * <p>Objetivo Funcional: Identificação nominal em pedidos e entregas.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return nome completo
     */
    public String obterNomeCompleto() {
        return this.nomeCompleto;
    }

    /**
     * Obtém o CPF (11 dígitos numéricos) do cliente.
     *
     * <p>Objetivo Funcional: Identificação fiscal única do comprador.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return CPF de 11 dígitos
     */
    public String obterCpf() {
        return this.cpf;
    }

    /**
     * Obtém o telefone de contato do cliente.
     *
     * <p>Objetivo Funcional: Contato para entrega e notificações.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return telefone
     */
    public String obterTelefone() {
        return this.telefone;
    }

    /**
     * Indica se o cliente já concluiu sua primeira compra na loja.
     *
     * <p>Objetivo Funcional: Informar ao motor de promoções (Módulo 2) se o cliente é elegível ao
     * benefício de primeira compra.</p>
     * <p>Papel no Design Pattern: Consulta de predicado comercial.</p>
     *
     * @return {@code true} se já concluiu compra anterior; {@code false} se ainda está na primeira compra
     */
    public boolean jaConcluiuPrimeiraCompra() {
        return this.primeiraCompraConcluida;
    }

    /**
     * Retorna uma visão imutável da lista de endereços cadastrados pelo cliente.
     *
     * <p>Objetivo Funcional: Listar todos os endereços disponíveis na conta do cliente.</p>
     * <p>Papel no Design Pattern: Encapsulamento defensivo da coleção do agregado.</p>
     *
     * @return lista imutável de {@link Endereco}
     */
    public List<Endereco> obterEnderecos() {
        return Collections.unmodifiableList(this.enderecos);
    }

    /**
     * Obtém o instante de criação do cadastro do cliente.
     *
     * <p>Objetivo Funcional: Rastreabilidade temporal do cliente.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC de criação
     */
    public Instant obterCriadoEm() {
        return this.criadoEm;
    }
}

