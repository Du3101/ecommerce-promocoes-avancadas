package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Contexto de execução e dados imutáveis fornecidos para avaliação da árvore de elegibilidade.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Context</i> compartilhado do padrão <i>Composite</i>
 * ({@link CondicaoElegibilidadeComponent}), transportando de forma desacoplada as informações
 * do cliente, carrinho e a porta de consulta do catálogo para as condições avaliarem suas regras.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class ContextoElegibilidade {

    private final UUID clienteId;
    private final boolean primeiraCompraCliente;
    private final String ufEntrega;
    private final BigDecimal subtotalCarrinho;
    private final List<ItemCarrinho> itens;
    private final PortaConsultaCatalogoCliente portaConsultaCatalogo;

    /**
     * Construtor completo do contexto de elegibilidade.
     *
     * <p>Objetivo funcional: Cria o snapshot imutável para verificação das regras da promoção.
     * <p>Papel no padrão: Inicialização dos parâmetros de contexto do Composite.
     *
     * @param clienteId Identificador do cliente (pode ser nulo para clientes anônimos).
     * @param primeiraCompraCliente Flag informando se o cliente nunca concluiu compra.
     * @param ufEntrega Sigla da Unidade Federativa para entrega (ex.: "SP").
     * @param subtotalCarrinho Subtotal bruto atual do carrinho.
     * @param itens Lista de itens presentes no carrinho.
     * @param portaConsultaCatalogo Porta do Módulo 1 para verificação recursiva de categorias.
     */
    public ContextoElegibilidade(
            UUID clienteId,
            boolean primeiraCompraCliente,
            String ufEntrega,
            BigDecimal subtotalCarrinho,
            List<ItemCarrinho> itens,
            PortaConsultaCatalogoCliente portaConsultaCatalogo
    ) {
        this.clienteId = clienteId;
        this.primeiraCompraCliente = primeiraCompraCliente;
        this.ufEntrega = ufEntrega != null ? ufEntrega.toUpperCase().trim() : "";
        this.subtotalCarrinho = subtotalCarrinho != null ? subtotalCarrinho : BigDecimal.ZERO;
        this.itens = itens != null ? List.copyOf(itens) : Collections.emptyList();
        this.portaConsultaCatalogo = portaConsultaCatalogo;
    }

    /**
     * Retorna o ID do cliente.
     *
     * @return UUID do cliente ou null se anônimo.
     */
    public UUID getClienteId() {
        return clienteId;
    }

    /**
     * Informa se é a primeira compra do cliente.
     *
     * @return true se primeira compra, false caso contrário.
     */
    public boolean isPrimeiraCompraCliente() {
        return primeiraCompraCliente;
    }

    /**
     * Retorna a UF de entrega.
     *
     * @return String contendo a sigla do estado.
     */
    public String getUfEntrega() {
        return ufEntrega;
    }

    /**
     * Retorna o subtotal bruto do carrinho.
     *
     * @return BigDecimal do subtotal.
     */
    public BigDecimal getSubtotalCarrinho() {
        return subtotalCarrinho;
    }

    /**
     * Retorna a lista de itens do carrinho.
     *
     * @return Lista imutável de itens.
     */
    public List<ItemCarrinho> getItens() {
        return itens;
    }

    /**
     * Retorna a porta de integração do catálogo do Módulo 1.
     *
     * @return PortaConsultaCatalogoCliente ou null se não disponível.
     */
    public PortaConsultaCatalogoCliente getPortaConsultaCatalogo() {
        return portaConsultaCatalogo;
    }
}
