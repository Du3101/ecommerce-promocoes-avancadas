package br.edu.fatec.ecommerce.modulo1.auditoria.dominio;

/**
 * Classificação das operações administrativas críticas registradas na trilha de auditoria (FR-012).
 *
 * <p>Responsabilidade Comercial: Tipificar cada evento sensível gravado pelos Proxies de auditoria.</p>
 */
public enum TipoOperacaoAuditoria {

    /**
     * Alteração de preço base ou adicional de variação de um produto do catálogo.
     */
    ALTERACAO_PRECO_PRODUTO,

    /**
     * Alteração de preço ou percentual de desconto de um Combo/Kit comercial.
     */
    ALTERACAO_PRECO_COMBO,

    /**
     * Criação ou modificação de categoria/subcategoria na árvore do catálogo.
     */
    CRIACAO_ALTERACAO_CATEGORIA,

    /**
     * Cadastro de novo produto ou Combo/Kit no catálogo.
     */
    CADASTRO_ITEM_CATALOGO,

    /**
     * Ajuste manual ou entrada de saldo físico em estoque.
     */
    AJUSTE_MANUAL_ESTOQUE,

    /**
     * Ativação ou inativação lógica de produto, variação, combo ou categoria.
     */
    ATIVACAO_INATIVACAO_ITEM,

    /**
     * Criação ou alteração de campanha promocional (integração com Módulo 2).
     */
    ALTERACAO_CAMPANHA,

    /**
     * Cancelamento administrativo de pedido (integração com Módulo 3).
     */
    CANCELAMENTO_PEDIDO_ADMIN
}

