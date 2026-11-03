package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoNula;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fábrica dinâmica de estratégias de promoção orientada a injeção de dependência desacoplada.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Implementa o padrão <i>Dynamic Factory</i> e <i>Dynamic Injection</i> (Constituição, Princípio II).
 * Todas as implementações de {@link EstrategiaCalculoPromocao} registradas no contexto do Spring
 * são injetadas automaticamente nesta fábrica e armazenadas em um catálogo indexado por
 * {@link TipoMecanicaPromocao}. Elimina qualquer estrutura condicional ({@code switch} ou {@code if/else})
 * para seleção de estratégias, permitindo adicionar uma 5ª ou 6ª promoção sem alterar esta classe.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class FabricaDinamicaEstrategiaPromocao {

    private final Map<TipoMecanicaPromocao, EstrategiaCalculoPromocao> catalogoEstrategias;

    /**
     * Construtor com injeção dinâmica de todas as estratégias de cálculo pelo Spring.
     *
     * <p>Objetivo funcional: Registra automaticamente cada estratégia promocional existente no catálogo.
     * <p>Papel no padrão: Dynamic Dependency Injection que alimenta a Dynamic Factory.
     *
     * @param estrategiasEncontradas Lista de estratégias descobertas pelo container Spring.
     */
    public FabricaDinamicaEstrategiaPromocao(List<EstrategiaCalculoPromocao> estrategiasEncontradas) {
        Map<TipoMecanicaPromocao, EstrategiaCalculoPromocao> mapa = new EnumMap<>(TipoMecanicaPromocao.class);
        if (estrategiasEncontradas != null) {
            for (EstrategiaCalculoPromocao estrategia : estrategiasEncontradas) {
                if (estrategia.obterTipoMecanica() != null) {
                    mapa.put(estrategia.obterTipoMecanica(), estrategia);
                }
            }
        }
        this.catalogoEstrategias = Collections.unmodifiableMap(mapa);
    }

    /**
     * Obtém dinamicamente a estratégia correspondente à mecânica solicitada.
     *
     * <p>Objetivo funcional: Localiza a estratégia em tempo constante O(1) sem condicionais.
     * <p>Papel no padrão: Método de resolução polimórfica da Dynamic Factory, com retorno
     * do Null Object {@link PromocaoNula} caso a mecânica não esteja registrada.
     *
     * @param tipo Tipo da mecânica promocional.
     * @return EstrategiaCalculoPromocao concreta ou PromocaoNula.obterInstancia().
     */
    public EstrategiaCalculoPromocao obterEstrategia(TipoMecanicaPromocao tipo) {
        if (tipo == null) {
            return PromocaoNula.obterInstancia();
        }
        return catalogoEstrategias.getOrDefault(tipo, PromocaoNula.obterInstancia());
    }

    /**
     * Retorna a quantidade de estratégias dinâmicas registradas.
     *
     * @return Quantidade inteira de mecânicas ativas.
     */
    public int quantidadeEstrategiasRegistradas() {
        return catalogoEstrategias.size();
    }
}
