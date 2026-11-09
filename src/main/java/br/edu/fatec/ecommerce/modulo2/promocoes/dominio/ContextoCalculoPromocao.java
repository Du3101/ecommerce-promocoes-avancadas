package br.edu.fatec.ecommerce.modulo2.promocoes.dominio;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Contexto imutável de entrada contendo as informações para cálculo do desconto de uma promoção.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Serve como parâmetro desacoplado de entrada para as estratégias do padrão
 * <i>Strategy</i> / <i>Dynamic Factory</i> ({@link br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao}),
 * permitindo que cada estratégia analise o carrinho e seus parâmetros específicos.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class ContextoCalculoPromocao {

    private final CarrinhoCompras carrinho;
    private final CampanhaPromocional campanha;
    private final BigDecimal subtotalBase;
    private final PortaConsultaCatalogoCliente portaConsultaCatalogo;

    /**
     * Construtor com os parâmetros necessários para o cálculo promocional.
     *
     * <p>Objetivo funcional: Reúne os dados do carrinho e os metadados da campanha para cálculo.
     * <p>Papel no padrão: Encapsulamento de contexto para execução da Strategy.
     *
     * @param carrinho Carrinho de compras contendo os itens.
     * @param campanha Configuração da campanha promocional a ser aplicada.
     * @param subtotalBase Valor subtotal monetário vigente antes deste abatimento.
     * @param portaConsultaCatalogo Porta de integração com o catálogo do Módulo 1.
     */
    public ContextoCalculoPromocao(
            CarrinhoCompras carrinho,
            CampanhaPromocional campanha,
            BigDecimal subtotalBase,
            PortaConsultaCatalogoCliente portaConsultaCatalogo
    ) {
        this.carrinho = Objects.requireNonNull(carrinho, "carrinho não pode ser nulo");
        this.campanha = Objects.requireNonNull(campanha, "campanha não pode ser nula");
        this.subtotalBase = OperacoesMonetarias.normalizar(subtotalBase);
        this.portaConsultaCatalogo = portaConsultaCatalogo;
    }

    public CarrinhoCompras getCarrinho() {
        return carrinho;
    }

    public CampanhaPromocional getCampanha() {
        return campanha;
    }

    public BigDecimal getSubtotalBase() {
        return subtotalBase;
    }

    public PortaConsultaCatalogoCliente getPortaConsultaCatalogo() {
        return portaConsultaCatalogo;
    }
}

