package br.edu.fatec.ecommerce.modulo1.integracao;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de fronteira imutável que expõe os dados consolidados de um item comercializável do catálogo
 * (seja {@code ProdutoVariacaoLeaf} ou {@code ComboKitComposite}) para consumo pelos Módulos 2 e 3 (FR-013).
 *
 * @param id UUID do item ou combo
 * @param sku código SKU único
 * @param nomeExibicao nome comercial completo
 * @param precoVigente preço unitário vigente em BRL
 * @param pesoTotalGramas peso total em gramas para cálculo de frete
 * @param alturaCm altura física em cm
 * @param larguraCm largura física em cm
 * @param profundidadeCm profundidade física em cm
 * @param categoriaPrincipalId UUID da categoria principal
 * @param ativo se o item está ativo para venda
 * @param eComboKit se o item é um combo/kit composto
 * @param disponibilidadeMaximaEstoque quantidade máxima disponível no estoque
 * @param skusFisicosDecompostos mapa de SKUs físicos folha e quantidades por unidade
 */
public record VisaoItemCatalogoDTO(
    UUID id,
    String sku,
    String nomeExibicao,
    BigDecimal precoVigente,
    int pesoTotalGramas,
    BigDecimal alturaCm,
    BigDecimal larguraCm,
    BigDecimal profundidadeCm,
    UUID categoriaPrincipalId,
    boolean ativo,
    boolean eComboKit,
    int disponibilidadeMaximaEstoque,
    Map<String, Integer> skusFisicosDecompostos
) {
}

