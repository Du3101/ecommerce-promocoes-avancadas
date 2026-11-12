package br.edu.fatec.ecommerce.modulo2.integracao;

import br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao.ServicoCarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.cupons.aplicacao.ServicoCuponsPromocionais;
import br.edu.fatec.ecommerce.modulo2.precificacao.aplicacao.PipelineFormacaoPrecoService;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.MemoriaCalculoPrecoVO;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.ParcelaDescontoVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de integração que implementa a porta {@link PortaConsultaCarrinhoPromocoes}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>Adapter</i> de fronteira (Constituição, Princípio V), traduzindo as chamadas
 * do Módulo 3 para operações coordenadas entre {@link ServicoCarrinhoCompras},
 * {@link PipelineFormacaoPrecoService} e {@link ServicoCuponsPromocionais}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class AdaptadorIntegracaoModulo2 implements PortaConsultaCarrinhoPromocoes {

    private final ServicoCarrinhoCompras servicoCarrinho;
    private final PipelineFormacaoPrecoService pipelinePreco;
    private final ServicoCuponsPromocionais servicoCupons;

    public AdaptadorIntegracaoModulo2(
            ServicoCarrinhoCompras servicoCarrinho,
            PipelineFormacaoPrecoService pipelinePreco,
            ServicoCuponsPromocionais servicoCupons
    ) {
        this.servicoCarrinho = Objects.requireNonNull(servicoCarrinho, "servicoCarrinho não pode ser nulo");
        this.pipelinePreco = Objects.requireNonNull(pipelinePreco, "pipelinePreco não pode ser nulo");
        this.servicoCupons = Objects.requireNonNull(servicoCupons, "servicoCupons não pode ser nulo");
    }

    @Override
    public ResumoCarrinhoCheckoutDTO obterResumoCarrinhoParaCheckout(UUID clienteId, String sessaoId) {
        CarrinhoCompras carrinho = servicoCarrinho.obterOuCriarCarrinho(clienteId, sessaoId);
        MemoriaCalculoPrecoVO memoria = pipelinePreco.calcularPrecoCarrinho(carrinho, null);

        Map<String, Integer> quantidadePorSku = new LinkedHashMap<>();
        List<ItemResumoCarrinhoDTO> itens = new ArrayList<>();
        for (ItemCarrinho item : carrinho.getItens()) {
            quantidadePorSku.put(item.getSku(), item.getQuantidade());
            // Linha detalhada para o snapshot imutável do pedido no Módulo 3
            itens.add(new ItemResumoCarrinhoDTO(
                    item.getSku(),
                    item.getNome(),
                    item instanceof ItemCarrinhoComboKit ? "COMBO_KIT" : "PRODUTO",
                    item.getPrecoUnitario(),
                    item.getQuantidade(),
                    item.calcularPesoTotalLinhaGramas()
            ));
        }

        List<String> promocoesAplicadas = memoria.descontosDiscriminados().stream()
                .filter(d -> "PROMOCAO".equalsIgnoreCase(d.origem()))
                .map(ParcelaDescontoVO::nomeBeneficio)
                .toList();

        return new ResumoCarrinhoCheckoutDTO(
                carrinho.getId(),
                carrinho.getClienteId(),
                carrinho.getSessaoId(),
                quantidadePorSku,
                carrinho.calcularPesoTotalGramas(),
                memoria.subtotalBruto(),
                memoria.totalDescontos(),
                memoria.totalLiquido(),
                carrinho.getCupomCodigo(),
                promocoesAplicadas,
                List.copyOf(itens)
        );
    }

    @Override
    public void esvaziarCarrinhoAposCheckout(UUID clienteId, String sessaoId) {
        servicoCarrinho.esvaziarCarrinho(clienteId, sessaoId);
    }

    @Override
    public void confirmarConsumoDeCupom(String codigoCupom) {
        servicoCupons.registrarUsoEfetivo(codigoCupom);
    }

    /**
     * Resolve o identificador de dono de carrinho para um usuário autenticado.
     *
     * <p>Objetivo Funcional: Reaproveitar exatamente a mesma regra usada pelo {@code CarrinhoComprasController}
     * (UUID determinístico derivado do e-mail), garantindo que o Módulo 3 leia o mesmo carrinho que o cliente montou.</p>
     * <p>Papel no Design Pattern: Método adaptado da porta (Anti-Corruption Layer entre Módulos 2 e 3).</p>
     *
     * @param emailUsuario e-mail do usuário autenticado.
     * @return UUID usado como chave do carrinho no Módulo 2.
     */
    @Override
    public UUID resolverIdentificadorCarrinhoPorEmail(String emailUsuario) {
        return ContextoSegurancaUsuario.derivarUuidDeterministicoPorEmail(emailUsuario);
    }
}

