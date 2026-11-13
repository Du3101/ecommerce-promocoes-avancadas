package br.edu.fatec.ecommerce.modulo3.frete.aplicacao;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.CalculadoraFretePort;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.PoliticaFreteGratis;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação que consolida as cotações das transportadoras e aplica a política de frete grátis.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> cliente dos <b>Adapters</b> de frete, que recebe todos por
 * injeção dinâmica do Spring e os indexa por {@link ModalidadeFrete} (lookup em mapa, sem {@code if/switch}).
 * Também é o ponto que combina a cotação "crua" da transportadora com a regra comercial da loja
 * ({@link PoliticaFreteGratis}).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoCalculoFrete {

    private final Map<ModalidadeFrete, CalculadoraFretePort> adaptadoresPorModalidade = new EnumMap<>(ModalidadeFrete.class);
    private final PoliticaFreteGratis politicaFreteGratis;

    /**
     * Construtor que indexa os adaptadores descobertos pelo Spring.
     *
     * @param adaptadores todos os beans {@link CalculadoraFretePort}
     * @param politicaFreteGratis política de frete grátis
     */
    public ServicoCalculoFrete(List<CalculadoraFretePort> adaptadores, PoliticaFreteGratis politicaFreteGratis) {
        for (CalculadoraFretePort adaptador : adaptadores) {
            this.adaptadoresPorModalidade.put(adaptador.modalidade(), adaptador);
        }
        this.politicaFreteGratis = politicaFreteGratis;
    }

    /**
     * Cota uma modalidade específica (usado no checkout).
     *
     * @param modalidade modalidade escolhida pelo cliente
     * @param cepDestino CEP de destino
     * @param pesoTotalGramas peso total em gramas
     * @param subtotalLiquido subtotal dos produtos já com descontos
     * @return cotação final (com frete grátis aplicado, se elegível)
     * @throws RegraNegocioModulo3Exception se não houver adaptador para a modalidade
     */
    public CotacaoFrete cotarModalidade(ModalidadeFrete modalidade, String cepDestino, int pesoTotalGramas,
                                        BigDecimal subtotalLiquido) {
        CalculadoraFretePort adaptador = this.adaptadoresPorModalidade.get(modalidade);
        if (adaptador == null) {
            throw new RegraNegocioModulo3Exception("Modalidade de frete indisponível: " + modalidade);
        }
        CotacaoFrete cotacaoTransportadora = adaptador.cotar(cepDestino, pesoTotalGramas);
        return this.politicaFreteGratis.aplicar(cotacaoTransportadora, subtotalLiquido, pesoTotalGramas);
    }

    /**
     * Cota todas as modalidades disponíveis, ordenadas do menor para o maior valor cobrado.
     *
     * @param cepDestino CEP de destino
     * @param pesoTotalGramas peso total em gramas
     * @param subtotalLiquido subtotal dos produtos já com descontos
     * @return lista de cotações finais
     */
    public List<CotacaoFrete> cotarTodasModalidades(String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido) {
        return this.adaptadoresPorModalidade.values().stream()
                .map(adaptador -> adaptador.cotar(cepDestino, pesoTotalGramas))
                .map(cotacao -> this.politicaFreteGratis.aplicar(cotacao, subtotalLiquido, pesoTotalGramas))
                .sorted(Comparator.comparing(CotacaoFrete::valorCobrado).thenComparing(CotacaoFrete::prazoDiasUteis))
                .toList();
    }

    /**
     * Indica se o pedido atende à política de frete grátis.
     *
     * @param subtotalLiquido subtotal dos produtos já com descontos
     * @param pesoTotalGramas peso total em gramas
     * @return {@code true} se elegível
     */
    public boolean freteGratisDisponivel(BigDecimal subtotalLiquido, int pesoTotalGramas) {
        return this.politicaFreteGratis.elegivel(subtotalLiquido, pesoTotalGramas);
    }
}
