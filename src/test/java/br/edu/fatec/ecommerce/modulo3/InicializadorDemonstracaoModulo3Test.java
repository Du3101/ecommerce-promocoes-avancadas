package br.edu.fatec.ecommerce.modulo3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.fatec.ecommerce.modulo3.compartilhado.InicializadorDemonstracaoModulo3Runner;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Garante que a carga de demonstração do Módulo 3 (T040) percorre os fluxos reais e deixa um pedido em cada estado
 * usado na apresentação.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class InicializadorDemonstracaoModulo3Test extends BaseIntegracaoModulo3Test {

    @Autowired
    private InicializadorDemonstracaoModulo3Runner runner;

    @Test
    @DisplayName("Carga de demonstração cria pedidos AGUARDANDO_PAGAMENTO, PAGO, ENVIADO e PAGAMENTO_RECUSADO")
    void cargaDemonstracaoDeveCriarPedidosEmVariosEstados() {
        runner.executarCargaDemonstracao();

        List<Pedido> pedidos = pedidoRepository.findAll();
        Map<StatusPedido, Long> porStatus = pedidos.stream()
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));

        assertEquals(4, pedidos.size());
        assertEquals(Map.of(
                StatusPedido.AGUARDANDO_PAGAMENTO, 1L,
                StatusPedido.PAGO, 1L,
                StatusPedido.ENVIADO, 1L,
                StatusPedido.PAGAMENTO_RECUSADO, 1L), porStatus);
        // 2 teclados saíram (1 reservado no PIX pendente + 1 baixado no cartão enviado)
        assertEquals(8, disponivel(TECLADO));
        // 1 mouse baixado (PIX pago); o do cartão recusado voltou ao estoque
        assertEquals(4, disponivel(MOUSE));
        pedidos.stream().map(Pedido::getEmailCliente).collect(Collectors.toMap(Function.identity(), e -> 1, Integer::sum))
                .forEach((email, quantidade) -> assertEquals(InicializadorDemonstracaoModulo3Runner.EMAIL_CLIENTE_DEMO, email));
    }
}
