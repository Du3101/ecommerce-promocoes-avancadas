package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.VisaoClienteElegibilidadeDTO;
import br.edu.fatec.ecommerce.modulo1.integracao.VisaoEnderecoEntregaDTO;
import br.edu.fatec.ecommerce.modulo2.integracao.ResumoCarrinhoCheckoutDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutRequestDTO;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.fabrica.FabricaProvedorPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.util.UUID;

/**
 * Estado de trabalho de uma execução do checkout, compartilhado entre as etapas do Template Method.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> objeto de contexto do <b>Template Method</b>. Cada etapa lê o
 * que as anteriores produziram e grava o seu resultado aqui, evitando métodos com longas listas de parâmetros.
 * Os campos são visíveis apenas dentro do pacote {@code checkout.aplicacao} (o template e suas especializações).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class ContextoCheckout {

    /** E-mail do usuário autenticado (Spring Security). */
    final String emailUsuario;

    /** Escolhas do cliente (pagamento, frete, chave de idempotência, cartão fictício). */
    final CheckoutRequestDTO requisicao;

    /** Família de pagamento escolhida (Abstract Factory). */
    final FabricaProvedorPagamento fabricaPagamento;

    /** ID real do cliente no Módulo 1. */
    UUID clienteId;

    /** Identificador do carrinho do usuário no Módulo 2. */
    UUID identificadorCarrinho;

    /** Número comercial gerado para o pedido. */
    String numeroPedido;

    /** Chave da reserva de estoque (também chave idempotente do checkout). */
    String chaveOperacaoEstoque;

    /** Dados cadastrais do cliente (Módulo 1). */
    VisaoClienteElegibilidadeDTO cliente;

    /** Endereço de entrega selecionado (Módulo 1). */
    VisaoEnderecoEntregaDTO endereco;

    /** Resumo do carrinho com preços e descontos (Módulo 2). */
    ResumoCarrinhoCheckoutDTO carrinho;

    /** Cotação de frete da modalidade escolhida. */
    CotacaoFrete cotacaoFrete;

    /** Pedido criado (gerenciado pela transação). */
    Pedido pedido;

    /** Resposta do gateway à cobrança. */
    RespostaPagamentoDTO respostaCobranca;

    /** Instruções de pagamento formatadas pela família de pagamento. */
    String instrucoesPagamento;

    /**
     * Cria o contexto de uma execução.
     *
     * @param emailUsuario e-mail do usuário autenticado
     * @param requisicao escolhas do cliente
     * @param fabricaPagamento família de pagamento escolhida
     */
    public ContextoCheckout(String emailUsuario, CheckoutRequestDTO requisicao, FabricaProvedorPagamento fabricaPagamento) {
        this.emailUsuario = emailUsuario;
        this.requisicao = requisicao;
        this.fabricaPagamento = fabricaPagamento;
    }
}
