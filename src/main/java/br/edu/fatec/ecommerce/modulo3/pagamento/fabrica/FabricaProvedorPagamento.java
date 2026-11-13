package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;

/**
 * Fábrica abstrata de uma família de objetos relacionados a um meio de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Abstract Factory</i>. Cada meio de pagamento tem uma família
 * coerente: o adaptador do provedor ({@link GatewayPagamentoPort}), o formatador das instruções ao cliente
 * ({@link FormatadorInstrucoesPagamento}) e o tipo de liquidação ({@link TipoLiquidacao}). Misturar peças de
 * famílias diferentes (ex.: gateway de cartão com instruções de boleto) é impossível, porque o código cliente só
 * pede tudo à mesma fábrica.
 *
 * <p><b>Guia de extensão:</b> para um novo meio (ex.: Carteira Digital), crie a constante em
 * {@link MetodoPagamento}, o adaptador e uma nova fábrica {@code @Component}. O
 * {@link GatewayPagamentoServiceLocator} a registra sozinho.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface FabricaProvedorPagamento {

    /**
     * Meio de pagamento atendido por esta família.
     *
     * @return método de pagamento
     */
    MetodoPagamento metodo();

    /**
     * Forma de liquidação desta família.
     *
     * @return {@link TipoLiquidacao#ONLINE} ou {@link TipoLiquidacao#ASSINCRONA}
     */
    TipoLiquidacao tipoLiquidacao();

    /**
     * Cria (fornece) o adaptador do provedor desta família.
     *
     * @return gateway de pagamento
     */
    GatewayPagamentoPort criarGateway();

    /**
     * Cria o formatador de instruções desta família.
     *
     * @return formatador de instruções ao cliente
     */
    FormatadorInstrucoesPagamento criarFormatadorInstrucoes();
}
