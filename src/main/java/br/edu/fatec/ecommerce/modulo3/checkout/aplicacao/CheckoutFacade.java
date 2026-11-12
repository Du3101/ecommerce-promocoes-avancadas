package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutRequestDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import br.edu.fatec.ecommerce.modulo3.pagamento.fabrica.FabricaProvedorPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.fabrica.GatewayPagamentoServiceLocator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto único de entrada do fechamento de compra.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Facade</i>. O checkout envolve sete subsistemas: cliente e
 * endereço (Módulo 1), carrinho e promoções (Módulo 2), estoque (Módulo 1), frete, pagamento, pedido e eventos.
 * O controller REST chama um único método — {@link #executarCheckout(String, CheckoutRequestDTO)} — e não conhece
 * nenhum deles.
 *
 * <p>A Facade também:
 * <ul>
 *   <li>abre a <b>transação</b> que envolve todas as etapas (tudo ou nada);</li>
 *   <li>resolve a família de pagamento pelo <b>Service Locator</b> e, pelo tipo de liquidação dessa família,
 *       escolhe a especialização do <b>Template Method</b> (online ou assíncrona) — sem {@code if} por meio de
 *       pagamento.</li>
 * </ul>
 *
 * <p><b>Guia de extensão:</b> uma nova etapa global (ex.: análise antifraude) entra no template ou aqui, sem mudar
 * o controller nem os módulos vizinhos.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class CheckoutFacade {

    private final GatewayPagamentoServiceLocator localizadorPagamento;
    private final Map<TipoLiquidacao, ProcessadorCheckoutTemplate> processadores = new EnumMap<>(TipoLiquidacao.class);

    /**
     * Construtor que indexa as especializações do checkout por tipo de liquidação.
     *
     * @param localizadorPagamento localizador das famílias de pagamento
     * @param processadores especializações do Template Method descobertas pelo Spring
     */
    public CheckoutFacade(GatewayPagamentoServiceLocator localizadorPagamento,
                          List<ProcessadorCheckoutTemplate> processadores) {
        this.localizadorPagamento = localizadorPagamento;
        for (ProcessadorCheckoutTemplate processador : processadores) {
            this.processadores.put(processador.tipoLiquidacaoAtendida(), processador);
        }
    }

    /**
     * Executa o checkout do usuário autenticado.
     *
     * @param emailUsuario e-mail do usuário autenticado
     * @param requisicao escolhas de pagamento e frete
     * @return pedido criado (ou reaproveitado, se a chave de idempotência se repetiu)
     */
    @Transactional
    public CheckoutResponseDTO executarCheckout(String emailUsuario, CheckoutRequestDTO requisicao) {
        // 1. Família de pagamento (Abstract Factory) via Service Locator
        FabricaProvedorPagamento fabrica = this.localizadorPagamento.obterFabrica(requisicao.metodoPagamento());
        // 2. Especialização do Template Method pela forma de liquidação da família
        ProcessadorCheckoutTemplate processador = this.processadores.get(fabrica.tipoLiquidacao());
        // 3. Fluxo invariante
        return processador.processarCheckout(new ContextoCheckout(emailUsuario, requisicao, fabrica));
    }
}
