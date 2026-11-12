package br.edu.fatec.ecommerce.modulo3.pedido.aplicacao;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PedidoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo3.eventos.BarramentoEventosDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.PedidoEnviadoEvent;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.ResultadoCancelamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operações que alteram o ciclo de vida do pedido depois do checkout: cancelamento e expedição.
 *
 * <p><b>Papel Arquitetural:</b> serviço de aplicação transacional. Toda operação carrega o pedido com
 * <b>lock pessimista</b> (serializando, por exemplo, um cancelamento do cliente e um webhook de aprovação que chegam
 * ao mesmo tempo) e delega a regra de transição ao <i>State</i>. O cancelamento usa o <i>Template Method</i>
 * ({@link ServicoCancelamentoPedido}) com a especialização adequada ao perfil do solicitante.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoOperacoesPedido {

    private final PedidoJpaRepository pedidoRepository;
    private final TransacaoPagamentoJpaRepository transacaoRepository;
    private final CancelamentoPeloCliente cancelamentoPeloCliente;
    private final CancelamentoPeloAdministrador cancelamentoPeloAdministrador;
    private final BarramentoEventosDominio barramentoEventos;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Construtor com dependências injetadas.
     *
     * @param pedidoRepository repositório de pedidos
     * @param transacaoRepository repositório de transações
     * @param cancelamentoPeloCliente fluxo de cancelamento do cliente
     * @param cancelamentoPeloAdministrador fluxo de cancelamento do administrador
     * @param barramentoEventos barramento de eventos
     */
    public ServicoOperacoesPedido(PedidoJpaRepository pedidoRepository,
                                  TransacaoPagamentoJpaRepository transacaoRepository,
                                  CancelamentoPeloCliente cancelamentoPeloCliente,
                                  CancelamentoPeloAdministrador cancelamentoPeloAdministrador,
                                  BarramentoEventosDominio barramentoEventos) {
        this.pedidoRepository = pedidoRepository;
        this.transacaoRepository = transacaoRepository;
        this.cancelamentoPeloCliente = cancelamentoPeloCliente;
        this.cancelamentoPeloAdministrador = cancelamentoPeloAdministrador;
        this.barramentoEventos = barramentoEventos;
    }

    /**
     * Cancela um pedido.
     *
     * @param pedidoId UUID do pedido
     * @param motivo motivo informado (opcional)
     * @param emailSolicitante e-mail do usuário autenticado
     * @param administrador se o solicitante tem perfil administrativo
     * @return resultado do cancelamento
     */
    @Transactional
    public ResultadoCancelamentoDTO cancelar(UUID pedidoId, String motivo, String emailSolicitante, boolean administrador) {
        Pedido pedido = carregarComLock(pedidoId);
        ServicoCancelamentoPedido fluxo = administrador ? cancelamentoPeloAdministrador : cancelamentoPeloCliente;
        return fluxo.cancelar(pedido, motivo, emailSolicitante);
    }

    /**
     * Inicia a separação de um pedido pago ({@code PAGO -> EM_PREPARACAO}).
     *
     * @param pedidoId UUID do pedido
     * @return pedido atualizado
     */
    @Transactional
    public PedidoResponseDTO iniciarPreparacao(UUID pedidoId) {
        Pedido pedido = carregarComLock(pedidoId);
        pedido.iniciarPreparacao();
        return paraDto(pedido);
    }

    /**
     * Despacha o pedido ({@code EM_PREPARACAO -> ENVIADO}) e notifica o cliente com o rastreio.
     *
     * @param pedidoId UUID do pedido
     * @param codigoRastreio código da transportadora (gerado automaticamente se vazio)
     * @return pedido atualizado
     */
    @Transactional
    public PedidoResponseDTO despachar(UUID pedidoId, String codigoRastreio) {
        Pedido pedido = carregarComLock(pedidoId);
        String codigo = codigoRastreio == null || codigoRastreio.isBlank() ? gerarCodigoRastreio() : codigoRastreio.trim();
        pedido.despacharParaEntrega(codigo);
        barramentoEventos.publicar(PedidoEnviadoEvent.de(pedido));
        return paraDto(pedido);
    }

    /**
     * Registra a entrega ({@code ENVIADO -> ENTREGUE}).
     *
     * @param pedidoId UUID do pedido
     * @return pedido atualizado
     */
    @Transactional
    public PedidoResponseDTO confirmarEntrega(UUID pedidoId) {
        Pedido pedido = carregarComLock(pedidoId);
        pedido.confirmarEntrega();
        return paraDto(pedido);
    }

    /**
     * Carrega o pedido com lock pessimista.
     *
     * @param pedidoId UUID do pedido
     * @return pedido bloqueado até o fim da transação
     * @throws PedidoNaoEncontradoException se não existir
     */
    private Pedido carregarComLock(UUID pedidoId) {
        return pedidoRepository.buscarPorIdComLock(pedidoId).orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
    }

    /**
     * Gera um código de rastreio no formato dos Correios ({@code BR123456789BR}).
     *
     * @return código de rastreio simulado
     */
    private String gerarCodigoRastreio() {
        return String.format("BR%09dBR", aleatorio.nextInt(1_000_000_000));
    }

    /**
     * Converte o pedido em DTO com a transação.
     *
     * @param pedido pedido
     * @return DTO
     */
    private PedidoResponseDTO paraDto(Pedido pedido) {
        return PedidoResponseDTO.de(pedido,
                transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId()).orElse(null));
    }
}
