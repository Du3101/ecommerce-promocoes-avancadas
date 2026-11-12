package br.edu.fatec.ecommerce.modulo1.integracao;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de leitura fornecida pelo Módulo 1 (Integrante 1) para consumo desacoplado
 * pelos Módulos 2 (Carrinho/Promoções) e 3 (Checkout/Frete) (FR-013).
 */
public interface PortaConsultaCatalogoCliente {

    /**
     * Consulta os dados consolidados e polimórficos de um item do catálogo
     * (seja {@code ProdutoVariacaoLeaf} ou {@code ComboKitComposite}).
     *
     * <p>Objetivo Funcional: Fornecer preço vigente, peso total em gramas, dimensões físicas e
     * disponibilidade em estoque para o carrinho, promoções e cálculo de frete.</p>
     * <p>Papel no Design Pattern: Porta de fronteira cliente do padrão <b>Composite</b>.</p>
     *
     * @param sku código SKU da variação ou do combo
     * @return DTO {@link VisaoItemCatalogoDTO}
     */
    VisaoItemCatalogoDTO consultarItemPorSku(String sku);

    /**
     * Verifica se determinado SKU pertence à categoria alvo ou a qualquer uma de suas
     * subcategorias descendentes (via padrão <b>Composite</b> de categorias).
     *
     * <p>Objetivo Funcional: Permitir ao motor de promoções (Módulo 2) avaliar regras como
     * "15% de desconto em toda a categoria Vestuário (incluindo subcategorias)".</p>
     * <p>Papel no Design Pattern: Consome {@code CategoriaComp.obterIdsSubarvoreRecursivo()}.</p>
     *
     * @param sku código SKU do item ou combo
     * @param categoriaAlvoId UUID da categoria alvo da promoção
     * @return {@code true} se a categoria principal do item pertencer à subárvore da categoria alvo
     */
    boolean itemPertenceACategoriaOuSubcategoria(String sku, UUID categoriaAlvoId);

    /**
     * Retorna os dados de elegibilidade do cliente e seu endereço selecionado
     * (ou representação segura via {@code EnderecoAusenteNullObject}).
     *
     * <p>Objetivo Funcional: Informar ao Módulo 2 (promoção de 1ª compra ou por região/UF) e ao
     * Módulo 3 (frete por CEP/UF) os dados do cliente sem risco de {@code NullPointerException}.</p>
     * <p>Papel no Design Pattern: Porta de fronteira apoiada pelo padrão <b>Null Object</b>.</p>
     *
     * @param clienteId UUID do cliente
     * @return DTO {@link VisaoClienteElegibilidadeDTO}
     */
    VisaoClienteElegibilidadeDTO consultarClienteParaCompra(UUID clienteId);

    /**
     * Resolve o identificador real do perfil {@code Cliente} a partir do e-mail do usuário autenticado.
     *
     * <p>Objetivo Funcional: O Módulo 3 recebe apenas o e-mail do usuário logado (Spring Security) e precisa
     * do {@code clienteId} persistido para consultar elegibilidade, reservar estoque e gravar o pedido
     * (a tabela {@code pedidos} referencia {@code clientes(id)}).</p>
     * <p>Papel no Design Pattern: Porta de fronteira (Anti-Corruption) que esconde do Módulo 3 as entidades
     * {@code Usuario} e {@code Cliente} do Módulo 1.</p>
     *
     * @param emailUsuario e-mail (username) do usuário autenticado
     * @return UUID do perfil de cliente vinculado ao usuário
     * @throws br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException
     *         se o usuário não existir ou não possuir perfil de cliente
     */
    UUID resolverClienteIdPorEmailUsuario(String emailUsuario);

    /**
     * Retorna o endereço de entrega selecionado do cliente com todos os campos necessários ao snapshot do pedido.
     *
     * <p>Objetivo Funcional: Fornecer ao Módulo 3 logradouro, número, complemento, bairro, cidade, UF e CEP
     * para congelar o endereço no pedido (Princípio IV — Snapshot Comercial Imutável).</p>
     * <p>Papel no Design Pattern: Porta de fronteira que traduz o {@code EnderecoAusenteNullObject} do
     * Módulo 1 em {@link Optional#empty()}, sem expor a entidade {@code Endereco}.</p>
     *
     * @param clienteId UUID do cliente
     * @return endereço completo selecionado, ou {@link Optional#empty()} se não houver endereço válido selecionado
     */
    Optional<VisaoEnderecoEntregaDTO> consultarEnderecoEntregaSelecionado(UUID clienteId);
}

