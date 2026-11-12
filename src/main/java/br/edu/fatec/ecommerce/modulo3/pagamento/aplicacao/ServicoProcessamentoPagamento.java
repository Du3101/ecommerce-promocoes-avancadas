package br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaComandoReservaEstoque;
import br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PagamentoInvalidoException;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PedidoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo3.eventos.BarramentoEventosDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.PagamentoConfirmadoEvent;
import br.edu.fatec.ecommerce.modulo3.mediador.CoordenadorCancelamentoMediator;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoRequestDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa confirmações e recusas de pagamento com idempotência estrita.
 *
 * <p><b>Como a idempotência é garantida</b> (Princípio IV da Constituição):
 * <ol>
 *   <li><b>Caminho rápido</b>: se já existe resposta registrada para a chave {@code WEBHOOK:<numeroPedido>},
 *       ela é devolvida com {@code idempotenteReexecutado = true} e nada é executado.</li>
 *   <li><b>Lock pessimista</b> no pedido ({@code SELECT ... FOR UPDATE}): webhooks repetidos que chegam ao mesmo
 *       tempo esperam um pelo outro.</li>
 *   <li><b>Segunda checagem</b> após obter o lock: quem esperou enxerga o registro gravado por quem processou primeiro.</li>
 *   <li>Processa uma única vez e grava a resposta, na mesma transação.</li>
 * </ol>
 * Além disso, a máquina de estados ({@code State}) e a reserva do Módulo 1 também são idempotentes, formando
 * camadas de defesa.
 *
 * <p><b>Papel Arquitetural:</b> serviço de aplicação; usa o <i>Mediator</i> para a recusa e publica eventos no
 * <i>Observer</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoProcessamentoPagamento {

    /** Prefixo da chave de idempotência das confirmações de pagamento. */
    private static final String PREFIXO_CHAVE_WEBHOOK = "WEBHOOK:";

    private final PedidoJpaRepository pedidoRepository;
    private final TransacaoPagamentoJpaRepository transacaoRepository;
    private final PortaComandoReservaEstoque portaEstoque;
    private final PortaConsultaCarrinhoPromocoes portaCarrinho;
    private final CoordenadorCancelamentoMediator mediador;
    private final ServicoIdempotenciaPagamento idempotencia;
    private final BarramentoEventosDominio barramentoEventos;

    /**
     * Construtor com dependências injetadas.
     *
     * @param pedidoRepository repositório de pedidos
     * @param transacaoRepository repositório de transações
     * @param portaEstoque porta de estoque do Módulo 1
     * @param portaCarrinho porta do carrinho/cupons do Módulo 2
     * @param mediador mediador de compensações
     * @param idempotencia serviço de idempotência
     * @param barramentoEventos barramento de eventos
     */
    public ServicoProcessamentoPagamento(PedidoJpaRepository pedidoRepository,
                                         TransacaoPagamentoJpaRepository transacaoRepository,
                                         PortaComandoReservaEstoque portaEstoque,
                                         PortaConsultaCarrinhoPromocoes portaCarrinho,
                                         CoordenadorCancelamentoMediator mediador,
                                         ServicoIdempotenciaPagamento idempotencia,
                                         BarramentoEventosDominio barramentoEventos) {
        this.pedidoRepository = pedidoRepository;
        this.transacaoRepository = transacaoRepository;
        this.portaEstoque = portaEstoque;
        this.portaCarrinho = portaCarrinho;
        this.mediador = mediador;
        this.idempotencia = idempotencia;
        this.barramentoEventos = barramentoEventos;
    }

    /**
     * Monta a chave de idempotência da confirmação de pagamento de um pedido.
     *
     * @param numeroPedido número do pedido
     * @return chave {@code WEBHOOK:<numeroPedido>}
     */
    public static String chaveIdempotenciaWebhook(String numeroPedido) {
        return PREFIXO_CHAVE_WEBHOOK + numeroPedido;
    }

    /**
     * Processa a notificação do gateway (aprovação ou recusa) de forma idempotente.
     *
     * @param requisicao notificação do gateway
     * @return resposta do processamento (original ou reexecução idempotente)
     * @throws PedidoNaoEncontradoException se o pedido não existir
     * @throws PagamentoInvalidoException se o valor pago divergir do total do pedido
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException
     *         se o pedido não estiver aguardando pagamento (ex.: cancelado antes)
     */
    @Transactional
    public ConfirmacaoPagamentoResponseDTO processarNotificacaoGateway(ConfirmacaoPagamentoRequestDTO requisicao) {
        String chave = chaveIdempotenciaWebhook(requisicao.chaveOperacaoPedido());

        // 1. Caminho rápido: confirmação repetida
        Optional<ConfirmacaoPagamentoResponseDTO> anterior = buscarRespostaAnterior(chave);
        if (anterior.isPresent()) {
            return anterior.get();
        }

        // 2. Lock pessimista: serializa webhooks concorrentes do mesmo pedido
        Pedido pedido = this.pedidoRepository.buscarPorNumeroComLock(requisicao.chaveOperacaoPedido())
                .orElseThrow(() -> new PedidoNaoEncontradoException(requisicao.chaveOperacaoPedido()));

        // 3. Segunda checagem: outra requisição pode ter concluído enquanto esperávamos o lock
        anterior = buscarRespostaAnterior(chave);
        if (anterior.isPresent()) {
            return anterior.get();
        }

        // 4. Processamento único
        ConfirmacaoPagamentoResponseDTO resposta;
        if (Boolean.TRUE.equals(requisicao.sucesso())) {
            validarValorPago(pedido, requisicao);
            aplicarConfirmacaoPagamento(pedido, requisicao.idTransacaoGateway());
            resposta = new ConfirmacaoPagamentoResponseDTO(ConfirmacaoPagamentoResponseDTO.PROCESSADO_SUCESSO,
                    pedido.getId(), pedido.getNumeroPedido(), pedido.getStatus(),
                    "Pagamento confirmado com sucesso e baixa definitiva de estoque consolidada.", false);
        } else {
            this.mediador.coordenarRecusaPagamento(pedido, requisicao.motivoRecusa());
            resposta = new ConfirmacaoPagamentoResponseDTO(ConfirmacaoPagamentoResponseDTO.PROCESSADO_RECUSA,
                    pedido.getId(), pedido.getNumeroPedido(), pedido.getStatus(),
                    "Pagamento recusado; a reserva de estoque foi liberada.", false);
        }

        // 5. Grava a resposta para responder repetições sem reprocessar
        this.idempotencia.registrar(chave, resposta.statusProcessamento(), resposta);
        return resposta;
    }

    /**
     * Aplica todos os efeitos de um pagamento aprovado (usado pelo webhook e pelo cartão aprovado no checkout).
     *
     * <p>Passos: (1) transição de estado para {@code PAGO}; (2) confirma a transação; (3) baixa definitiva do
     * estoque no Módulo 1; (4) consome o cupom no Módulo 2; (5) publica {@link PagamentoConfirmadoEvent}.</p>
     *
     * @param pedido pedido (gerenciado pela transação corrente)
     * @param identificadorTransacao identificador da liquidação no gateway
     */
    public void aplicarConfirmacaoPagamento(Pedido pedido, String identificadorTransacao) {
        // 1. State valida a transição (pedido cancelado ou recusado não pode ser pago)
        pedido.confirmarPagamento(identificadorTransacao);
        // 2. Registro financeiro
        this.transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId())
                .ifPresent(transacao -> transacao.confirmar(identificadorTransacao));
        // 3. Reserva ATIVA -> CONFIRMADA (saída física definitiva)
        this.portaEstoque.confirmarBaixaDefinitiva(pedido.getChaveOperacaoEstoque());
        // 4. Cupom só é consumido quando o dinheiro entra
        if (pedido.getCupomUtilizado() != null) {
            this.portaCarrinho.confirmarConsumoDeCupom(pedido.getCupomUtilizado());
        }
        // 5. Notificações desacopladas
        this.barramentoEventos.publicar(PagamentoConfirmadoEvent.de(pedido, identificadorTransacao));
    }

    /**
     * Registra a resposta "pagamento confirmado" de um pedido pago online no checkout, para que um webhook
     * tardio do gateway sobre o mesmo pedido seja respondido como repetição idempotente.
     *
     * @param pedido pedido pago no checkout
     */
    public void registrarConfirmacaoOnline(Pedido pedido) {
        ConfirmacaoPagamentoResponseDTO resposta = new ConfirmacaoPagamentoResponseDTO(
                ConfirmacaoPagamentoResponseDTO.PROCESSADO_SUCESSO, pedido.getId(), pedido.getNumeroPedido(),
                pedido.getStatus(), "Pagamento aprovado online no checkout.", false);
        this.idempotencia.registrar(chaveIdempotenciaWebhook(pedido.getNumeroPedido()),
                resposta.statusProcessamento(), resposta);
    }

    /**
     * Busca uma resposta já registrada e a marca como reexecução.
     *
     * @param chave chave de idempotência
     * @return resposta anterior marcada como reexecução, ou vazio
     */
    private Optional<ConfirmacaoPagamentoResponseDTO> buscarRespostaAnterior(String chave) {
        return this.idempotencia.buscarResposta(chave, ConfirmacaoPagamentoResponseDTO.class)
                .map(ConfirmacaoPagamentoResponseDTO::comoReexecucao);
    }

    /**
     * Garante que o valor liquidado é exatamente o total do pedido.
     *
     * @param pedido pedido
     * @param requisicao notificação do gateway
     * @throws PagamentoInvalidoException se o valor divergir
     */
    private void validarValorPago(Pedido pedido, ConfirmacaoPagamentoRequestDTO requisicao) {
        if (requisicao.valorPago() != null
                && requisicao.valorPago().compareTo(pedido.getFinanceiro().getValorTotalLiquido()) != 0) {
            throw new PagamentoInvalidoException(String.format(
                    "Valor pago (R$ %s) diverge do total do pedido %s (R$ %s).",
                    requisicao.valorPago(), pedido.getNumeroPedido(), pedido.getFinanceiro().getValorTotalLiquido()));
        }
    }

    /**
     * Retorna a transação mais recente do pedido.
     *
     * @param pedido pedido
     * @return transação, se existir
     */
    public Optional<TransacaoPagamento> buscarTransacao(Pedido pedido) {
        return this.transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId());
    }
}
