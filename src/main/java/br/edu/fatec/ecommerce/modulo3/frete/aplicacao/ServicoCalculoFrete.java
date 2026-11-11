package br.edu.fatec.ecommerce.modulo3.frete.aplicacao;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.CalculadoraFretePort;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação para orquestração e cotação de fretes logísticos.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Orquestrador da camada de aplicação que consome os adaptadores polimórficos
 * do padrão <b>Adapter</b>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoCalculoFrete {

    private final List<CalculadoraFretePort> adaptadores;

    /**
     * Construtor com injeção automática de todas as implementações de calculadoras de frete.
     *
     * @param adaptadores Lista de adaptadores gerenciados pelo Spring.
     */
    public ServicoCalculoFrete(List<CalculadoraFretePort> adaptadores) {
        this.adaptadores = adaptadores;
    }

    /**
     * Calcula o frete para uma modalidade específica.
     *
     * @param modalidade Modalidade escolhida.
     * @param cepDestino CEP de destino.
     * @param pesoTotalGramas Peso acumulado.
     * @param subtotalLiquido Subtotal líquido dos itens.
     * @return CotacaoFrete correspondente.
     */
    public CotacaoFrete cotarModalidade(ModalidadeFrete modalidade, String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido) {
        return adaptadores.stream()
                .filter(a -> a.suporta(modalidade))
                .findFirst()
                .map(a -> a.calcularFrete(cepDestino, pesoTotalGramas, subtotalLiquido))
                .orElseThrow(() -> new RegraNegocioModulo3Exception("Adaptador não encontrado para modalidade: " + modalidade));
    }

    /**
     * Retorna todas as opções de frete cotadas para o destino e carga solicitados.
     *
     * @param cepDestino CEP de entrega.
     * @param pesoTotalGramas Peso total dos itens.
     * @param subtotalLiquido Subtotal líquido dos produtos.
     * @return Lista de cotações para todas as modalidades.
     */
    public List<CotacaoFrete> cotarTodasModalidades(String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido) {
        return adaptadores.stream()
                .map(a -> a.calcularFrete(cepDestino, pesoTotalGramas, subtotalLiquido))
                .toList();
    }
}
