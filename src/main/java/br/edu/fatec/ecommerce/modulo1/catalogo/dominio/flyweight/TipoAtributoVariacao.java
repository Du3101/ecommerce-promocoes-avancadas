package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight;

/**
 * Classificação dos tipos de atributos intrínsecos compartilháveis entre variações de produtos (FR-006).
 *
 * <p>Papel no Design Pattern: Faz parte da chave intrínseca do padrão <b>Flyweight</b>.</p>
 */
public enum TipoAtributoVariacao {

    /** Tamanho físico ou de vestuário (ex.: P, M, G, GG, 42). */
    TAMANHO,

    /** Cor ou acabamento visual (ex.: Preto, Branco, Azul Marinho). */
    COR,

    /** Capacidade de armazenamento ou volume (ex.: 256GB, 500ml, 1TB). */
    CAPACIDADE,

    /** Tensão elétrica nominal (ex.: 110V, 220V, Bivolt). */
    VOLTAGEM,

    /** Composição ou material predominante (ex.: Algodão, Inox, Couro). */
    MATERIAL,

    /** Outra especificação técnica padronizada (ex.: Switch Red, Layout ABNT2). */
    ESPECIFICACAO
}

