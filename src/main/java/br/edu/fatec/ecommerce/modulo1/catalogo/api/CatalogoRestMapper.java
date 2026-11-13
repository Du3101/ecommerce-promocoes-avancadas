package br.edu.fatec.ecommerce.modulo1.catalogo.api;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilitário de projeção REST compartilhado para serializar uniformemente itens do catálogo
 * (variações simples e combos promocionais) nos endpoints públicos e administrativos.
 */
final class CatalogoRestMapper {

    /**
     * Construtor privado para impedir instanciação desta classe utilitária estática.
     *
     * <p>Objetivo Funcional: Garantir uso apenas via métodos estáticos de mapeamento.</p>
     * <p>Papel no Design Pattern: Utilitário auxiliar de conversão REST do módulo de catálogo.</p>
     */
    private CatalogoRestMapper() {
    }

    /**
     * Converte polimorficamente qualquer {@link ItemCatalogoComponent} em mapa JSON padronizado.
     *
     * <p>Objetivo Funcional: Expor preço vigente, peso, dimensões, disponibilidade e status ativo
     * de forma idêntica entre vitrine pública e painel administrativo.</p>
     * <p>Papel no Design Pattern: Serialização uniforme do <i>Component</i> no padrão <b>Composite</b>.</p>
     *
     * @param item instância de {@link ItemCatalogoComponent} (folha ou combo)
     * @param servicoConsultaCatalogo serviço de consulta usado para apurar saldo disponível em estoque
     * @return mapa JSON detalhado do item comercializável
     */
    static Map<String, Object> mapearItemUniforme(
        ItemCatalogoComponent item,
        ServicoConsultaCatalogo servicoConsultaCatalogo
    ) {
        DimensoesFisicas dim = item.obterDimensoesFisicas();
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", item.obterId());
        mapa.put("sku", item.obterSku());
        mapa.put("nomeExibicao", item.obterNomeExibicao());
        mapa.put("eComboKit", item.eComboKit());
        mapa.put("ativo", item.estaAtivo());
        mapa.put("precoVigente", item.calcularPrecoVigente());
        mapa.put("pesoTotalGramas", item.obterPesoTotalGramas());
        mapa.put("dimensoesFisicas", Map.of(
            "pesoGramas", dim.obterPesoGramas(),
            "alturaCm", dim.obterAlturaCm(),
            "larguraCm", dim.obterLarguraCm(),
            "profundidadeCm", dim.obterProfundidadeCm(),
            "volumeCubicoCm3", dim.calcularVolumeCubicoCm3()
        ));
        mapa.put("categoriaPrincipalId", item.obterCategoriaPrincipalId());
        mapa.put("disponibilidadeMaximaEstoque", servicoConsultaCatalogo.calcularDisponibilidadeEstoque(item));

        if (item instanceof ProdutoVariacaoLeaf variacao) {
            mapa.put("atributosFlyweight", variacao.obterAtributosFlyweight().stream()
                .map(attr -> Map.of(
                    "id", attr.obterId(),
                    "tipo", attr.obterTipo(),
                    "valor", attr.obterValor(),
                    "codigoApresentacao", attr.obterCodigoApresentacao()
                ))
                .collect(Collectors.toList()));
        } else if (item instanceof ComboKitComposite combo) {
            mapa.put("modoPreco", combo.obterModoPreco());
            mapa.put("percentualDescontoKit", combo.obterPercentualDescontoKit());
            mapa.put("skusFisicosDecompostos", combo.expandirSkusFisicosComMultiplicador());
        }
        return mapa;
    }
}
