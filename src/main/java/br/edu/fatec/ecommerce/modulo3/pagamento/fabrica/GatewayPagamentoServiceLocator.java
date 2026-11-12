package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PagamentoInvalidoException;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Registro central das famílias de pagamento disponíveis, indexadas por {@link MetodoPagamento}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Service Locator</i> + injeção dinâmica. O Spring descobre
 * todas as {@link FabricaProvedorPagamento} registradas; o locator monta um mapa {@code metodo → fábrica} na
 * inicialização. Em tempo de execução, a resolução é uma consulta ao mapa: sem {@code if/switch} e sem
 * {@code new} de classes concretas no código de negócio.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class GatewayPagamentoServiceLocator {

    private final Map<MetodoPagamento, FabricaProvedorPagamento> fabricasPorMetodo = new EnumMap<>(MetodoPagamento.class);

    /**
     * Indexa as fábricas descobertas pelo Spring.
     *
     * @param fabricas todas as famílias de pagamento registradas
     */
    public GatewayPagamentoServiceLocator(List<FabricaProvedorPagamento> fabricas) {
        for (FabricaProvedorPagamento fabrica : fabricas) {
            this.fabricasPorMetodo.put(fabrica.metodo(), fabrica);
        }
    }

    /**
     * Localiza a família (Abstract Factory) de um meio de pagamento.
     *
     * @param metodo meio de pagamento
     * @return fábrica da família
     * @throws PagamentoInvalidoException se o meio não estiver registrado
     */
    public FabricaProvedorPagamento obterFabrica(MetodoPagamento metodo) {
        FabricaProvedorPagamento fabrica = this.fabricasPorMetodo.get(metodo);
        if (fabrica == null) {
            throw new PagamentoInvalidoException("Nenhum provedor de pagamento registrado para: " + metodo);
        }
        return fabrica;
    }

    /**
     * Atalho para obter diretamente o adaptador do provedor.
     *
     * @param metodo meio de pagamento
     * @return gateway da família
     * @throws PagamentoInvalidoException se o meio não estiver registrado
     */
    public GatewayPagamentoPort obterGateway(MetodoPagamento metodo) {
        return obterFabrica(metodo).criarGateway();
    }
}
