package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.EstadoPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoAguardandoPagamentoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoCanceladoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoCriadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEmPreparacaoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEntregueState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEnviadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoPagamentoRecusadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoPagoState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz que representa um Pedido de compra no e-commerce.
 *
 * <p><b>Papéis Arquiteturais nos Design Patterns:</b>
 * <ul>
 *   <li><b>State (Context)</b>: todas as transições de ciclo de vida são delegadas ao objeto polimórfico
 *       {@link EstadoPedido}. O pedido não contém nenhum {@code switch} de regras de negócio por status.</li>
 *   <li><b>Builder</b>: o pedido é montado passo a passo por {@link Builder}, que valida o snapshot completo
 *       antes de criar o agregado (cliente, entrega, pagamento, valores e ao menos um item).</li>
 *   <li><b>Snapshot Comercial Imutável</b> (Princípio IV): endereço ({@link EnderecoEntregaSnapshot}), valores
 *       ({@link SnapshotFinanceiro}), itens ({@link ItemPedidoSnapshot}), cupom e promoções ficam congelados.
 *       Não existem setters públicos para esses dados.</li>
 * </ul>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    /** Separador usado para persistir a lista de promoções aplicadas em uma única coluna de texto. */
    private static final String SEPARADOR_PROMOCOES = " | ";

    @Id
    private UUID id;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 40)
    private String numeroPedido;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "email_cliente", nullable = false, length = 180)
    private String emailCliente;

    @Column(name = "nome_cliente", nullable = false, length = 150)
    private String nomeCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPedido status;

    @Transient
    private EstadoPedido estadoAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 30)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_frete", nullable = false, length = 50)
    private ModalidadeFrete modalidadeFrete;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private int prazoEntregaDias;

    @Column(name = "codigo_rastreio", length = 60)
    private String codigoRastreio;

    @Column(name = "chave_operacao_estoque", nullable = false, unique = true, length = 120)
    private String chaveOperacaoEstoque;

    @Column(name = "cupom_utilizado", length = 60)
    private String cupomUtilizado;

    @Column(name = "promocoes_aplicadas", length = 1000)
    private String promocoesAplicadas;

    @Embedded
    private SnapshotFinanceiro financeiro;

    @Embedded
    private EnderecoEntregaSnapshot enderecoEntrega;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemPedidoSnapshot> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /**
     * Construtor protegido padrão exigido pela JPA.
     */
    protected Pedido() {
    }

    /**
     * Construtor privado usado exclusivamente pelo {@link Builder}.
     *
     * <p>Papel no Design Pattern: produto final do <b>Builder</b>; inicia o ciclo de vida no estado
     * {@link PedidoCriadoState} (State).</p>
     *
     * @param builder construtor já validado
     */
    private Pedido(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.numeroPedido = builder.numeroPedido;
        this.clienteId = builder.clienteId;
        this.emailCliente = builder.emailCliente;
        this.nomeCliente = builder.nomeCliente;
        this.metodoPagamento = builder.metodoPagamento;
        this.modalidadeFrete = builder.modalidadeFrete;
        this.prazoEntregaDias = Math.max(1, builder.prazoEntregaDias);
        this.chaveOperacaoEstoque = builder.chaveOperacaoEstoque;
        this.enderecoEntrega = builder.enderecoEntrega;
        this.financeiro = builder.financeiro;
        this.cupomUtilizado = builder.cupomUtilizado;
        this.promocoesAplicadas = builder.promocoesAplicadas.isEmpty()
                ? null
                : String.join(SEPARADOR_PROMOCOES, builder.promocoesAplicadas);
        for (ItemPedidoSnapshot item : builder.itens) {
            item.associarPedido(this);
            this.itens.add(item);
        }
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
        this.mudarEstado(new PedidoCriadoState());
    }

    /**
     * Inicia a construção fluente de um novo pedido.
     *
     * <p>Papel no Design Pattern: ponto de entrada do <b>Builder</b> (Static Factory Method).</p>
     *
     * @return novo {@link Builder} vazio
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reconstrói o objeto polimórfico de estado após o carregamento do banco de dados.
     *
     * <p>Objetivo Funcional: apenas o enum {@code status} é persistido; o objeto {@link EstadoPedido}
     * é {@code @Transient} e precisa ser recriado.</p>
     * <p>Papel no Design Pattern: ponto único de mapeamento enum → <i>Concrete State</i>. É o único
     * {@code switch} do módulo, e ele não contém regras de negócio, apenas a fábrica de estados.</p>
     */
    @PostLoad
    protected void inicializarEstadoPolimorfico() {
        if (this.status == null) {
            this.estadoAtual = new PedidoCriadoState();
            return;
        }
        this.estadoAtual = switch (this.status) {
            case CRIADO -> new PedidoCriadoState();
            case AGUARDANDO_PAGAMENTO -> new PedidoAguardandoPagamentoState();
            case PAGO -> new PedidoPagoState();
            case EM_PREPARACAO -> new PedidoEmPreparacaoState();
            case ENVIADO -> new PedidoEnviadoState();
            case ENTREGUE -> new PedidoEntregueState();
            case CANCELADO -> new PedidoCanceladoState();
            case PAGAMENTO_RECUSADO -> new PedidoPagamentoRecusadoState();
        };
    }

    /**
     * Troca o estado polimórfico do pedido e sincroniza o enum persistido.
     *
     * <p>Papel no Design Pattern: método do <i>Context</i> chamado somente pelos <i>Concrete States</i> ao
     * executar uma transição válida. É público apenas porque os estados vivem em outro pacote.</p>
     *
     * @param novoEstado nova instância concreta do padrão State
     */
    public void mudarEstado(EstadoPedido novoEstado) {
        this.estadoAtual = Objects.requireNonNull(novoEstado, "Novo estado não pode ser nulo.");
        this.status = novoEstado.getStatus();
        this.atualizadoEm = Instant.now();
    }

    /**
     * Coloca o pedido em espera pela confirmação do pagamento (PIX/boleto).
     *
     * <p>Papel no Design Pattern: delegação ao State ({@code CRIADO -> AGUARDANDO_PAGAMENTO}).</p>
     *
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void aguardarConfirmacaoPagamento() {
        garantirEstadoInicializado();
        this.estadoAtual.aguardarPagamento(this);
    }

    /**
     * Confirma a quitação financeira do pedido.
     *
     * <p>Papel no Design Pattern: delegação ao State (transição para {@code PAGO}).</p>
     *
     * @param transacaoId identificador retornado pelo gateway
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void confirmarPagamento(String transacaoId) {
        garantirEstadoInicializado();
        this.estadoAtual.pagar(this, transacaoId);
    }

    /**
     * Registra a recusa definitiva do pagamento.
     *
     * <p>Papel no Design Pattern: delegação ao State (transição para {@code PAGAMENTO_RECUSADO}).</p>
     *
     * @param motivo justificativa da recusa
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void recusarPagamento(String motivo) {
        garantirEstadoInicializado();
        this.estadoAtual.recusarPagamento(this, motivo);
    }

    /**
     * Inicia a preparação e separação física na expedição.
     *
     * <p>Papel no Design Pattern: delegação ao State ({@code PAGO -> EM_PREPARACAO}).</p>
     *
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void iniciarPreparacao() {
        garantirEstadoInicializado();
        this.estadoAtual.preparar(this);
    }

    /**
     * Despacha o pedido para a transportadora.
     *
     * <p>Papel no Design Pattern: delegação ao State ({@code EM_PREPARACAO -> ENVIADO}).</p>
     *
     * @param codigoRastreio código de rastreamento da transportadora
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void despacharParaEntrega(String codigoRastreio) {
        garantirEstadoInicializado();
        this.estadoAtual.despachar(this, codigoRastreio);
    }

    /**
     * Registra a entrega final ao cliente.
     *
     * <p>Papel no Design Pattern: delegação ao State ({@code ENVIADO -> ENTREGUE}).</p>
     *
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir
     */
    public void confirmarEntrega() {
        garantirEstadoInicializado();
        this.estadoAtual.entregar(this);
    }

    /**
     * Cancela o pedido.
     *
     * <p>Papel no Design Pattern: delegação ao State (transição para {@code CANCELADO}). As compensações
     * (estorno e estoque) são coordenadas fora do agregado, pelo Mediator de cancelamento.</p>
     *
     * @param motivo justificativa gravada no pedido
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o estado atual não permitir (ex.: pedido já enviado)
     */
    public void cancelar(String motivo) {
        garantirEstadoInicializado();
        this.estadoAtual.cancelar(this, motivo);
    }

    /**
     * Informa se o pagamento já foi capturado no estado atual.
     *
     * <p>Papel no Design Pattern: consulta polimórfica delegada ao State.</p>
     *
     * @return {@code true} se o pedido já estiver pago (ou em etapa posterior)
     */
    public boolean pagamentoJaCapturado() {
        garantirEstadoInicializado();
        return this.estadoAtual.pagamentoJaCapturado();
    }

    /**
     * Verifica se o pedido pertence ao cliente informado.
     *
     * <p>Objetivo Funcional: impedir que um cliente consulte ou cancele pedidos de outra pessoa
     * (Princípio V — segregação de dados do cliente).</p>
     *
     * @param outroClienteId UUID do cliente autenticado
     * @return {@code true} se o pedido for do cliente
     */
    public boolean pertenceAoCliente(UUID outroClienteId) {
        return this.clienteId.equals(outroClienteId);
    }

    /**
     * Grava o código de rastreamento logístico.
     *
     * <p>Papel no Design Pattern: método auxiliar do Context chamado pelo estado {@code EM_PREPARACAO}.</p>
     *
     * @param codigoRastreio código emitido pela transportadora
     */
    public void definirCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }

    /**
     * Grava o motivo de cancelamento ou recusa.
     *
     * <p>Papel no Design Pattern: método auxiliar do Context chamado pelos estados que cancelam ou recusam.</p>
     *
     * @param motivo justificativa
     */
    public void definirMotivoCancelamento(String motivo) {
        this.motivoCancelamento = motivo;
    }

    /**
     * Garante que o estado polimórfico esteja inicializado antes de delegar uma operação.
     *
     * <p>Papel no Design Pattern: proteção do Context para instâncias ainda não passadas pelo {@code @PostLoad}.</p>
     */
    private void garantirEstadoInicializado() {
        if (this.estadoAtual == null) {
            inicializarEstadoPolimorfico();
        }
    }

    /**
     * Retorna o identificador único do pedido.
     *
     * @return UUID do pedido
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retorna o número comercial legível do pedido.
     *
     * @return número no formato {@code PED-AAAAMM-XXXXXX}
     */
    public String getNumeroPedido() {
        return numeroPedido;
    }

    /**
     * Retorna o ID do cliente comprador (Módulo 1).
     *
     * @return UUID do cliente
     */
    public UUID getClienteId() {
        return clienteId;
    }

    /**
     * Retorna o e-mail do cliente congelado no momento da compra (destinatário das notificações).
     *
     * @return e-mail do cliente
     */
    public String getEmailCliente() {
        return emailCliente;
    }

    /**
     * Retorna o nome do cliente congelado no momento da compra.
     *
     * @return nome completo do cliente
     */
    public String getNomeCliente() {
        return nomeCliente;
    }

    /**
     * Retorna o status persistido do pedido.
     *
     * @return status atual
     */
    public StatusPedido getStatus() {
        return status;
    }

    /**
     * Retorna o objeto polimórfico do estado atual.
     *
     * @return estado atual (Concrete State)
     */
    public EstadoPedido getEstadoAtual() {
        garantirEstadoInicializado();
        return estadoAtual;
    }

    /**
     * Retorna o método de pagamento escolhido.
     *
     * @return método de pagamento
     */
    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    /**
     * Retorna a modalidade de frete escolhida.
     *
     * @return modalidade de frete
     */
    public ModalidadeFrete getModalidadeFrete() {
        return modalidadeFrete;
    }

    /**
     * Retorna o prazo de entrega em dias úteis congelado na compra.
     *
     * @return prazo em dias úteis
     */
    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    /**
     * Retorna o código de rastreio (preenchido no despacho).
     *
     * @return código de rastreio ou {@code null}
     */
    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    /**
     * Retorna a chave de operação usada na reserva de estoque do Módulo 1.
     *
     * @return chave de operação (também usada como chave idempotente do checkout)
     */
    public String getChaveOperacaoEstoque() {
        return chaveOperacaoEstoque;
    }

    /**
     * Retorna o código do cupom usado na compra.
     *
     * @return código do cupom ou {@code null}
     */
    public String getCupomUtilizado() {
        return cupomUtilizado;
    }

    /**
     * Retorna os nomes das promoções que concederam desconto nesta compra.
     *
     * @return lista imutável (vazia se nenhuma promoção foi aplicada)
     */
    public List<String> getPromocoesAplicadas() {
        if (promocoesAplicadas == null || promocoesAplicadas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(promocoesAplicadas.split("\\s\\|\\s")).toList();
    }

    /**
     * Retorna a memória de cálculo financeira congelada.
     *
     * @return snapshot financeiro
     */
    public SnapshotFinanceiro getFinanceiro() {
        return financeiro;
    }

    /**
     * Retorna o endereço de entrega congelado.
     *
     * @return snapshot do endereço
     */
    public EnderecoEntregaSnapshot getEnderecoEntrega() {
        return enderecoEntrega;
    }

    /**
     * Retorna o motivo de cancelamento ou recusa.
     *
     * @return motivo ou {@code null}
     */
    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    /**
     * Retorna as linhas imutáveis do pedido.
     *
     * @return lista não modificável de itens
     */
    public List<ItemPedidoSnapshot> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /**
     * Retorna o instante de criação do pedido.
     *
     * @return instante de criação
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /**
     * Retorna o instante da última transição de estado.
     *
     * @return instante da última atualização
     */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /**
     * Construtor fluente e validado do agregado {@link Pedido}.
     *
     * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Builder</i>. O checkout reúne dados de quatro fontes
     * diferentes (cliente do Módulo 1, carrinho do Módulo 2, frete e pagamento do Módulo 3). Um construtor com
     * 15 parâmetros seria ilegível e fácil de errar; o Builder nomeia cada etapa e valida tudo em
     * {@link #construir()} antes de existir um pedido inconsistente.</p>
     */
    public static final class Builder {

        private UUID id;
        private String numeroPedido;
        private UUID clienteId;
        private String emailCliente;
        private String nomeCliente;
        private MetodoPagamento metodoPagamento;
        private ModalidadeFrete modalidadeFrete;
        private int prazoEntregaDias;
        private String chaveOperacaoEstoque;
        private EnderecoEntregaSnapshot enderecoEntrega;
        private SnapshotFinanceiro financeiro;
        private String cupomUtilizado;
        private final List<String> promocoesAplicadas = new ArrayList<>();
        private final List<ItemPedidoSnapshot> itens = new ArrayList<>();

        /**
         * Construtor privado: use {@link Pedido#builder()}.
         */
        private Builder() {
        }

        /**
         * Define explicitamente o ID do pedido (opcional; gerado se omitido).
         *
         * @param id UUID desejado
         * @return este builder
         */
        public Builder comId(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Define o número comercial do pedido.
         *
         * @param numeroPedido número legível (ex.: PED-202611-AB12CD)
         * @return este builder
         */
        public Builder comNumero(String numeroPedido) {
            this.numeroPedido = numeroPedido;
            return this;
        }

        /**
         * Define o cliente comprador.
         *
         * @param clienteId UUID do cliente no Módulo 1
         * @param emailCliente e-mail para notificações
         * @param nomeCliente nome completo congelado
         * @return este builder
         */
        public Builder paraCliente(UUID clienteId, String emailCliente, String nomeCliente) {
            this.clienteId = clienteId;
            this.emailCliente = emailCliente;
            this.nomeCliente = nomeCliente;
            return this;
        }

        /**
         * Define o método de pagamento.
         *
         * @param metodoPagamento PIX, cartão ou boleto
         * @return este builder
         */
        public Builder comPagamento(MetodoPagamento metodoPagamento) {
            this.metodoPagamento = metodoPagamento;
            return this;
        }

        /**
         * Define a entrega (modalidade, prazo e endereço congelado).
         *
         * @param modalidadeFrete modalidade escolhida
         * @param prazoEntregaDias prazo cotado em dias úteis
         * @param enderecoEntrega snapshot do endereço
         * @return este builder
         */
        public Builder comEntrega(ModalidadeFrete modalidadeFrete, int prazoEntregaDias,
                                  EnderecoEntregaSnapshot enderecoEntrega) {
            this.modalidadeFrete = modalidadeFrete;
            this.prazoEntregaDias = prazoEntregaDias;
            this.enderecoEntrega = enderecoEntrega;
            return this;
        }

        /**
         * Define a chave da reserva de estoque no Módulo 1.
         *
         * @param chaveOperacaoEstoque chave única da reserva
         * @return este builder
         */
        public Builder comReservaEstoque(String chaveOperacaoEstoque) {
            this.chaveOperacaoEstoque = chaveOperacaoEstoque;
            return this;
        }

        /**
         * Define a memória de cálculo financeira.
         *
         * @param financeiro snapshot financeiro
         * @return este builder
         */
        public Builder comFinanceiro(SnapshotFinanceiro financeiro) {
            this.financeiro = financeiro;
            return this;
        }

        /**
         * Define o cupom e as promoções aplicadas pelo Módulo 2.
         *
         * @param cupomUtilizado código do cupom (pode ser nulo)
         * @param promocoes nomes das promoções que deram desconto (pode ser nulo)
         * @return este builder
         */
        public Builder comBeneficios(String cupomUtilizado, List<String> promocoes) {
            this.cupomUtilizado = cupomUtilizado != null && !cupomUtilizado.isBlank() ? cupomUtilizado : null;
            if (promocoes != null) {
                this.promocoesAplicadas.addAll(promocoes);
            }
            return this;
        }

        /**
         * Adiciona uma linha congelada ao pedido.
         *
         * @param sku SKU do item
         * @param nome nome no momento da compra
         * @param tipoItem PRODUTO ou COMBO_KIT
         * @param precoUnitarioTabela preço unitário antes de descontos
         * @param quantidade quantidade comprada
         * @param descontoUnitario desconto rateado por unidade
         * @param pesoTotalGramas peso total da linha
         * @return este builder
         */
        public Builder adicionarItem(String sku, String nome, String tipoItem, BigDecimal precoUnitarioTabela,
                                     int quantidade, BigDecimal descontoUnitario, int pesoTotalGramas) {
            this.itens.add(new ItemPedidoSnapshot(null, null, sku, nome, tipoItem, precoUnitarioTabela,
                    quantidade, descontoUnitario, pesoTotalGramas));
            return this;
        }

        /**
         * Valida o snapshot completo e cria o pedido no estado {@code CRIADO}.
         *
         * <p>Papel no Design Pattern: etapa final do <b>Builder</b> (validação centralizada).</p>
         *
         * @return novo {@link Pedido}
         * @throws RegraNegocioModulo3Exception se algum dado obrigatório estiver ausente
         */
        public Pedido construir() {
            exigir(numeroPedido != null && !numeroPedido.isBlank(), "Número do pedido é obrigatório.");
            exigir(clienteId != null, "ID do cliente é obrigatório.");
            exigir(emailCliente != null && !emailCliente.isBlank(), "E-mail do cliente é obrigatório.");
            exigir(nomeCliente != null && !nomeCliente.isBlank(), "Nome do cliente é obrigatório.");
            exigir(metodoPagamento != null, "Método de pagamento é obrigatório.");
            exigir(modalidadeFrete != null, "Modalidade de frete é obrigatória.");
            exigir(enderecoEntrega != null, "Endereço de entrega é obrigatório.");
            exigir(chaveOperacaoEstoque != null && !chaveOperacaoEstoque.isBlank(),
                    "Chave da reserva de estoque é obrigatória.");
            exigir(financeiro != null, "Snapshot financeiro é obrigatório.");
            exigir(!itens.isEmpty(), "O pedido precisa de ao menos um item.");
            return new Pedido(this);
        }

        /**
         * Lança exceção de regra de negócio se a condição não for satisfeita.
         *
         * @param condicao condição exigida
         * @param mensagem mensagem de erro
         * @throws RegraNegocioModulo3Exception se {@code condicao} for falsa
         */
        private static void exigir(boolean condicao, String mensagem) {
            if (!condicao) {
                throw new RegraNegocioModulo3Exception(mensagem);
            }
        }
    }
}
