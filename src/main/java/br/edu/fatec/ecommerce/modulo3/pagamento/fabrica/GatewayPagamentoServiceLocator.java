package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PagamentoInvalidoException;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Localizador e resolvedor dinâmico de adaptadores de gateway de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Combina os padrões <b>Service Locator</b> e <b>Abstract Factory</b> com injeção dinâmica de
 * dependências do Spring. Evita cadeias rígidas de {@code if/else} e atende ao Princípio Aberto-Fechado (OCP).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class GatewayPagamentoServiceLocator {

    private final List<GatewayPagamentoPort> adaptadores;

    /**
     * Construtor com injeção automática de todas as implementações de {@link GatewayPagamentoPort}.
     *
     * @param adaptadores Lista de beans gerenciados pelo Spring.
     */
    public GatewayPagamentoServiceLocator(List<GatewayPagamentoPort> adaptadores) {
        this.adaptadores = adaptadores;
    }

    /**
     * Localiza o adaptador compatível com o método de pagamento solicitado.
     *
     * @param metodo Método de pagamento desejado.
     * @return Instância de GatewayPagamentoPort.
     * @throws PagamentoInvalidoException Se não houver adaptador registrado para o método.
     */
    public GatewayPagamentoPort obterAdaptador(MetodoPagamento metodo) {
        return adaptadores.stream()
                .filter(a -> a.suporta(metodo))
                .findFirst()
                .orElseThrow(() -> new PagamentoInvalidoException(
                        "Nenhum gateway de pagamento registrado para a modalidade: " + metodo));
    }
}
