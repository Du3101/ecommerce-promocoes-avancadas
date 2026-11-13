package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaComandoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes;
import br.edu.fatec.ecommerce.modulo3.eventos.BarramentoEventosDominio;
import br.edu.fatec.ecommerce.modulo3.frete.aplicacao.ServicoCalculoFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Conjunto de colaboradores usados pelas etapas comuns do checkout.
 *
 * <p><b>Papel Arquitetural:</b> agrupa as dependências do {@link ProcessadorCheckoutTemplate} para que cada
 * especialização receba um único parâmetro no construtor. Mostra também, em um só lugar, que o Módulo 3 fala com
 * os Módulos 1 e 2 <b>apenas</b> pelas portas de integração.
 *
 * @param portaCliente porta de consulta de cliente/catálogo (Módulo 1)
 * @param portaEstoque porta de comando de estoque (Módulo 1)
 * @param portaCarrinho porta do carrinho e promoções (Módulo 2)
 * @param servicoFrete serviço de cotação de frete
 * @param pedidoRepository repositório de pedidos
 * @param transacaoRepository repositório de transações
 * @param barramentoEventos barramento de eventos (Observer)
 * @param geradorNumeroPedido gerador de números de pedido
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public record RecursosCheckout(
        PortaConsultaCatalogoCliente portaCliente,
        PortaComandoReservaEstoque portaEstoque,
        PortaConsultaCarrinhoPromocoes portaCarrinho,
        ServicoCalculoFrete servicoFrete,
        PedidoJpaRepository pedidoRepository,
        TransacaoPagamentoJpaRepository transacaoRepository,
        BarramentoEventosDominio barramentoEventos,
        GeradorNumeroPedido geradorNumeroPedido
) {
}
