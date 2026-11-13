package br.edu.fatec.ecommerce.modulo3.compartilhado;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.UsuarioJpaRepository;
import br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao.ServicoCarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutRequestDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutResponseDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.aplicacao.CheckoutFacade;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoRequestDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao.ServicoProcessamentoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoOperacoesPedido;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Carga de demonstração do Módulo 3 para a apresentação (Swagger com pedidos prontos em vários estados).
 *
 * <p>Diferente de um seed que grava linhas direto no banco, este runner percorre os <b>fluxos reais</b> do sistema
 * (carrinho do Módulo 2 → checkout → webhook → expedição), então os dados de demonstração são consistentes com o
 * estoque, as promoções e os eventos. Ao final ficam disponíveis:
 * <ul>
 *   <li>um pedido PIX em {@code AGUARDANDO_PAGAMENTO} (para demonstrar o webhook idempotente e o cancelamento);</li>
 *   <li>um pedido PIX {@code PAGO} via webhook;</li>
 *   <li>um pedido no cartão {@code ENVIADO} (para demonstrar que o cancelamento é bloqueado pelo State);</li>
 *   <li>um pedido no cartão {@code PAGAMENTO_RECUSADO} (cartão final 0000).</li>
 * </ul>
 *
 * <p>Por ser infraestrutura de demonstração, e não regra de negócio, usa diretamente os serviços de cadastro de
 * cliente (Módulo 1) e de carrinho (Módulo 2) — algo que o código de negócio do Módulo 3 nunca faz.
 *
 * <p>Executa após todos os {@code CommandLineRunner} (catálogo do Módulo 1 e promoções do Módulo 2) e só quando
 * {@code ecommerce.modulo3.seed-demo-ativo=true}. É idempotente: não roda de novo se o cliente demo já existir.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class InicializadorDemonstracaoModulo3Runner {

    /** E-mail do cliente de demonstração. */
    public static final String EMAIL_CLIENTE_DEMO = "cliente.demo@ecommerce.fatec.edu.br";

    /** Senha do cliente de demonstração. */
    public static final String SENHA_CLIENTE_DEMO = "ClienteFatec@2026";

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorDemonstracaoModulo3Runner.class);

    private final boolean seedAtivo;
    private final ServicoIdentidadeECliente servicoIdentidade;
    private final UsuarioJpaRepository usuarioRepository;
    private final ServicoCarrinhoCompras servicoCarrinho;
    private final PortaConsultaCarrinhoPromocoes portaCarrinho;
    private final CheckoutFacade checkoutFacade;
    private final ServicoProcessamentoPagamento servicoPagamento;
    private final ServicoOperacoesPedido servicoOperacoes;

    /**
     * Construtor com dependências injetadas.
     *
     * @param seedAtivo flag {@code ecommerce.modulo3.seed-demo-ativo}
     * @param servicoIdentidade cadastro de clientes (Módulo 1)
     * @param usuarioRepository repositório de usuários (checagem de idempotência)
     * @param servicoCarrinho serviço de carrinho (Módulo 2)
     * @param portaCarrinho porta do carrinho (identificador por e-mail)
     * @param checkoutFacade fachada do checkout
     * @param servicoPagamento serviço de pagamento (webhook)
     * @param servicoOperacoes operações de expedição
     */
    public InicializadorDemonstracaoModulo3Runner(
            @Value("${ecommerce.modulo3.seed-demo-ativo:false}") boolean seedAtivo,
            ServicoIdentidadeECliente servicoIdentidade,
            UsuarioJpaRepository usuarioRepository,
            ServicoCarrinhoCompras servicoCarrinho,
            PortaConsultaCarrinhoPromocoes portaCarrinho,
            CheckoutFacade checkoutFacade,
            ServicoProcessamentoPagamento servicoPagamento,
            ServicoOperacoesPedido servicoOperacoes) {
        this.seedAtivo = seedAtivo;
        this.servicoIdentidade = servicoIdentidade;
        this.usuarioRepository = usuarioRepository;
        this.servicoCarrinho = servicoCarrinho;
        this.portaCarrinho = portaCarrinho;
        this.checkoutFacade = checkoutFacade;
        this.servicoPagamento = servicoPagamento;
        this.servicoOperacoes = servicoOperacoes;
    }

    /**
     * Dispara a carga quando a aplicação termina de subir.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciarAplicacao() {
        if (!seedAtivo || usuarioRepository.findByEmailIgnoreCase(EMAIL_CLIENTE_DEMO).isPresent()) {
            return;
        }
        try {
            executarCargaDemonstracao();
        } catch (RuntimeException erro) {
            LOG.warn("[DEMO M3] Carga de demonstração do Módulo 3 não concluída: {}", erro.getMessage());
        }
    }

    /**
     * Cria o cliente demo e percorre os fluxos reais para gerar pedidos em vários estados.
     */
    public void executarCargaDemonstracao() {
        // 1. Cliente com endereço selecionado
        servicoIdentidade.cadastrarClientePublico("Cliente Demonstração FATEC", EMAIL_CLIENTE_DEMO,
                "52998224725", "11999990000", SENHA_CLIENTE_DEMO);
        servicoIdentidade.adicionarEnderecoAoCliente(EMAIL_CLIENTE_DEMO, "Casa", "01310100", "Avenida Paulista",
                "1000", "Apto 101", "Bela Vista", "São Paulo", "SP", true);
        UUID carrinho = portaCarrinho.resolverIdentificadorCarrinhoPorEmail(EMAIL_CLIENTE_DEMO);

        // 2. Pedido PIX aguardando pagamento
        servicoCarrinho.adicionarItem(carrinho, null, InicializadorDadosDemonstracaoModulo1Runner.SKU_TECLADO_DEMO, 1);
        CheckoutResponseDTO pixPendente = checkout(MetodoPagamento.PIX, ModalidadeFrete.CORREIOS_PAC, null);

        // 3. Pedido PIX pago via webhook
        servicoCarrinho.adicionarItem(carrinho, null, InicializadorDadosDemonstracaoModulo1Runner.SKU_MOUSE_DEMO, 1);
        CheckoutResponseDTO pixPago = checkout(MetodoPagamento.PIX, ModalidadeFrete.CORREIOS_SEDEX, null);
        servicoPagamento.processarNotificacaoGateway(new ConfirmacaoPagamentoRequestDTO(
                pixPago.numeroPedido(), "PIX-E2E-DEMO-0001", true, pixPago.snapshotFinanceiro().valorTotalLiquido(), null));

        // 4. Pedido no cartão aprovado, separado e enviado
        servicoCarrinho.adicionarItem(carrinho, null, InicializadorDadosDemonstracaoModulo1Runner.SKU_TECLADO_DEMO, 1);
        CheckoutResponseDTO cartaoEnviado = checkout(MetodoPagamento.CARTAO_CREDITO,
                ModalidadeFrete.TRANSPORTADORA_EXPRESSA, "4111111111111111");
        servicoOperacoes.iniciarPreparacao(cartaoEnviado.pedidoId());
        servicoOperacoes.despachar(cartaoEnviado.pedidoId(), null);

        // 5. Pedido no cartão recusado (final 0000) — reserva liberada; o carrinho é mantido e depois esvaziado
        servicoCarrinho.adicionarItem(carrinho, null, InicializadorDadosDemonstracaoModulo1Runner.SKU_MOUSE_DEMO, 1);
        CheckoutResponseDTO cartaoRecusado = checkout(MetodoPagamento.CARTAO_CREDITO,
                ModalidadeFrete.CORREIOS_PAC, "4000000000000000");
        servicoCarrinho.esvaziarCarrinho(carrinho, null);

        LOG.info("[DEMO M3] Pedidos de demonstração criados para {} (senha {}): {} AGUARDANDO_PAGAMENTO, {} PAGO, "
                        + "{} ENVIADO, {} PAGAMENTO_RECUSADO", EMAIL_CLIENTE_DEMO, SENHA_CLIENTE_DEMO,
                pixPendente.numeroPedido(), pixPago.numeroPedido(), cartaoEnviado.numeroPedido(),
                cartaoRecusado.numeroPedido());
    }

    /**
     * Executa um checkout do cliente demo.
     *
     * @param metodo meio de pagamento
     * @param modalidade modalidade de frete
     * @param cartao número de cartão fictício (ou {@code null})
     * @return resposta do checkout
     */
    private CheckoutResponseDTO checkout(MetodoPagamento metodo, ModalidadeFrete modalidade, String cartao) {
        return checkoutFacade.executarCheckout(EMAIL_CLIENTE_DEMO,
                new CheckoutRequestDTO(metodo, modalidade, null, cartao, cartao == null ? null : "CLIENTE DEMO"));
    }
}
