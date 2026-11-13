package br.edu.fatec.ecommerce.modulo3.pedido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.EnderecoEntregaSnapshot;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.SnapshotFinanceiro;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários da máquina de estados do pedido (padrão State) e do Builder do agregado (T012).
 *
 * <p>Cobre o item 6 do Princípio VI da Constituição: tentativa de transição inválida no ciclo de vida.</p>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class MaquinaEstadosPedidoTest {

    /**
     * Cria um pedido válido no estado CRIADO pelo Builder.
     *
     * @return pedido novo
     */
    private Pedido novoPedido() {
        return Pedido.builder()
                .comNumero("PED-TESTE-" + UUID.randomUUID().toString().substring(0, 6))
                .paraCliente(UUID.randomUUID(), "cliente@teste.com", "Cliente Teste")
                .comPagamento(MetodoPagamento.PIX)
                .comEntrega(ModalidadeFrete.CORREIOS_SEDEX, 2,
                        new EnderecoEntregaSnapshot("Casa", "01310100", "Av. Paulista", "1000", null,
                                "Bela Vista", "São Paulo", "SP"))
                .comReservaEstoque("CHK-TESTE-" + UUID.randomUUID())
                .comFinanceiro(SnapshotFinanceiro.criarComPisoZero(
                        new BigDecimal("600.00"), new BigDecimal("110.00"), new BigDecimal("30.00"), false))
                .comBeneficios("CUPOM10", List.of("Leve 3 Pague 2", "Desconto Combo"))
                .adicionarItem("SKU-1", "Teclado", "PRODUTO", new BigDecimal("300.00"), 1, new BigDecimal("50.00"), 850)
                .adicionarItem("SKU-2", "Mouse", "PRODUTO", new BigDecimal("150.00"), 2, new BigDecimal("30.00"), 400)
                .construir();
    }

    @Test
    @DisplayName("Builder cria o pedido em CRIADO com snapshot financeiro, itens, cupom e promoções congelados")
    void builderDeveCriarPedidoComSnapshotCompleto() {
        Pedido pedido = novoPedido();

        assertEquals(StatusPedido.CRIADO, pedido.getStatus());
        assertEquals(new BigDecimal("520.00"), pedido.getFinanceiro().getValorTotalLiquido());
        assertEquals(2, pedido.getItens().size());
        assertEquals(new BigDecimal("240.00"), pedido.getItens().get(1).getSubtotalLiquido());
        assertEquals("CUPOM10", pedido.getCupomUtilizado());
        assertEquals(List.of("Leve 3 Pague 2", "Desconto Combo"), pedido.getPromocoesAplicadas());
    }

    @Test
    @DisplayName("Builder rejeita pedido sem itens (snapshot incompleto)")
    void builderDeveRejeitarPedidoSemItens() {
        Pedido.Builder builder = Pedido.builder()
                .comNumero("PED-X")
                .paraCliente(UUID.randomUUID(), "a@b.com", "A")
                .comPagamento(MetodoPagamento.PIX)
                .comEntrega(ModalidadeFrete.CORREIOS_PAC, 6,
                        new EnderecoEntregaSnapshot(null, "01310100", "Rua", "1", null, "B", "C", "SP"))
                .comReservaEstoque("CHK-X")
                .comFinanceiro(SnapshotFinanceiro.criarComPisoZero(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, false));

        assertThrows(RegraNegocioModulo3Exception.class, builder::construir);
    }

    @Test
    @DisplayName("Ciclo feliz: CRIADO -> AGUARDANDO_PAGAMENTO -> PAGO -> EM_PREPARACAO -> ENVIADO -> ENTREGUE")
    void deveTransitarPeloCicloDeVidaCompleto() {
        Pedido pedido = novoPedido();

        pedido.aguardarConfirmacaoPagamento();
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        assertFalse(pedido.pagamentoJaCapturado());

        pedido.confirmarPagamento("PIX-123");
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        assertTrue(pedido.pagamentoJaCapturado());

        pedido.iniciarPreparacao();
        assertEquals(StatusPedido.EM_PREPARACAO, pedido.getStatus());

        pedido.despacharParaEntrega("BR123456789BR");
        assertEquals(StatusPedido.ENVIADO, pedido.getStatus());
        assertEquals("BR123456789BR", pedido.getCodigoRastreio());

        pedido.confirmarEntrega();
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        assertTrue(pedido.getStatus().isEstadoTerminal());
    }

    @Test
    @DisplayName("Cartão aprovado online: CRIADO -> PAGO direto")
    void cartaoAprovadoDevePagarDiretoDoEstadoCriado() {
        Pedido pedido = novoPedido();
        pedido.confirmarPagamento("CC-AUTH-1");
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
    }

    @Test
    @DisplayName("Não é possível cancelar pedido ENVIADO (exige devolução) nem ENTREGUE")
    void naoDeveCancelarPedidoEnviadoOuEntregue() {
        Pedido pedido = novoPedido();
        pedido.confirmarPagamento("CC-AUTH-1");
        pedido.iniciarPreparacao();
        pedido.despacharParaEntrega("BR000000001BR");

        TransicaoEstadoInvalidaException erro = assertThrows(TransicaoEstadoInvalidaException.class,
                () -> pedido.cancelar("Desisti"));
        assertTrue(erro.getMessage().contains("devolução"));
        assertEquals(StatusPedido.ENVIADO, pedido.getStatus());

        pedido.confirmarEntrega();
        assertThrows(TransicaoEstadoInvalidaException.class, () -> pedido.cancelar("Desisti"));
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
    }

    @Test
    @DisplayName("Não é possível pagar pedido CANCELADO nem pagar duas vezes")
    void naoDevePagarPedidoCanceladoNemPagoDuasVezes() {
        Pedido cancelado = novoPedido();
        cancelado.aguardarConfirmacaoPagamento();
        cancelado.cancelar("Cliente desistiu");
        assertEquals(StatusPedido.CANCELADO, cancelado.getStatus());
        assertEquals("Cliente desistiu", cancelado.getMotivoCancelamento());
        assertThrows(TransicaoEstadoInvalidaException.class, () -> cancelado.confirmarPagamento("PIX-1"));

        Pedido pago = novoPedido();
        pago.confirmarPagamento("PIX-1");
        assertThrows(TransicaoEstadoInvalidaException.class, () -> pago.confirmarPagamento("PIX-1"));
        assertEquals(StatusPedido.PAGO, pago.getStatus());
    }

    @Test
    @DisplayName("Transições fora de ordem são bloqueadas: despachar sem pagar, entregar sem enviar, preparar sem pagar")
    void deveBloquearTransicoesForaDeOrdem() {
        Pedido pedido = novoPedido();
        pedido.aguardarConfirmacaoPagamento();

        assertThrows(TransicaoEstadoInvalidaException.class, () -> pedido.despacharParaEntrega("BR1"));
        assertThrows(TransicaoEstadoInvalidaException.class, pedido::confirmarEntrega);
        assertThrows(TransicaoEstadoInvalidaException.class, pedido::iniciarPreparacao);
        assertThrows(TransicaoEstadoInvalidaException.class, pedido::aguardarConfirmacaoPagamento);
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
    }

    @Test
    @DisplayName("Recusa de pagamento é terminal: não aceita pagamento nem cancelamento depois")
    void pagamentoRecusadoDeveSerTerminal() {
        Pedido pedido = novoPedido();
        pedido.aguardarConfirmacaoPagamento();
        pedido.recusarPagamento("Saldo insuficiente");

        assertEquals(StatusPedido.PAGAMENTO_RECUSADO, pedido.getStatus());
        assertThrows(TransicaoEstadoInvalidaException.class, () -> pedido.confirmarPagamento("PIX-1"));
        assertThrows(TransicaoEstadoInvalidaException.class, () -> pedido.cancelar("x"));
    }

    @Test
    @DisplayName("Pedido pago ou em preparação ainda pode ser cancelado (com estorno)")
    void devePermitirCancelarPedidoPagoOuEmPreparacao() {
        Pedido pago = novoPedido();
        pago.confirmarPagamento("CC-1");
        pago.cancelar("Arrependimento");
        assertEquals(StatusPedido.CANCELADO, pago.getStatus());

        Pedido emPreparacao = novoPedido();
        emPreparacao.confirmarPagamento("CC-2");
        emPreparacao.iniciarPreparacao();
        assertTrue(emPreparacao.pagamentoJaCapturado());
        emPreparacao.cancelar("Falta de produto");
        assertEquals(StatusPedido.CANCELADO, emPreparacao.getStatus());
    }
}
