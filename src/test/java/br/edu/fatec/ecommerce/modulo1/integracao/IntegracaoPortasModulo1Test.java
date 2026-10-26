package br.edu.fatec.ecommerce.modulo1.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoReal;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes de contrato para as portas de integração do Módulo 1 ({@link PortaConsultaCatalogoCliente}
 * e {@link PortaComandoReservaEstoque}) destinadas aos Módulos 2 e 3 (FR-013).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegracaoPortasModulo1Test {

    @Autowired
    private PortaConsultaCatalogoCliente portaConsulta;

    @Autowired
    private PortaComandoReservaEstoque portaComando;

    @Autowired
    private ServicoAdminCatalogoReal servicoAdminCatalogoReal;

    @Autowired
    private ServicoIdentidadeECliente servicoIdentidade;

    /**
     * Verifica que os Módulos 2 e 3 podem consultar elegibilidade do cliente (com Null Object ou
     * endereço selecionado), verificar pertinência recursiva em subcategorias e reservar/confirmar estoque.
     *
     * <p>Objetivo Funcional: Validar FR-013.</p>
     * <p>Papel no Design Pattern: Comprova desacoplamento via portas de integração sustentadas por
     * <b>Composite</b>, <b>Null Object</b> e <b>Static Factory Method</b>.</p>
     */
    @Test
    @DisplayName("Deve atender aos contratos de consulta e reserva para os Módulos 2 e 3")
    void deveAtenderContratosDeIntegracaoParaModulos2E3() {
        Cliente cliente = this.servicoIdentidade.cadastrarClientePublico(
            "Carlos Integrador",
            "carlos.integrador@exemplo.com",
            "39053344705",
            "19991112222",
            "SenhaForte@2026"
        );

        VisaoClienteElegibilidadeDTO visaoSemEndereco =
            this.portaConsulta.consultarClienteParaCompra(cliente.obterId());
        assertFalse(visaoSemEndereco.primeiraCompraConcluida());
        assertFalse(visaoSemEndereco.possuiEnderecoEntregaSelecionado());
        assertEquals("", visaoSemEndereco.cepDestino());

        CategoriaComp catPai = this.servicoAdminCatalogoReal.criarCategoria("Calçados", "calcados", null);
        CategoriaComp catFilha = this.servicoAdminCatalogoReal.criarCategoria(
            "Tênis de Corrida",
            "tenis-corrida",
            catPai.obterId()
        );

        ProdutoBuilder builder = new ProdutoBuilder()
            .comNome("Tênis Maratona Pro")
            .comDescricao("Tênis leve para corrida")
            .comMarca("FatecRun")
            .comPrecoBase(new BigDecimal("499.90"))
            .naCategoria(catFilha.obterId())
            .adicionarVariacao(
                "TEN-MARATONA-42",
                null,
                "Tênis Maratona Pro - 42",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    600,
                    new BigDecimal("14.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("35.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(
            builder,
            Map.of("TEN-MARATONA-42", 8)
        );

        VisaoItemCatalogoDTO visaoItem = this.portaConsulta.consultarItemPorSku("TEN-MARATONA-42");
        assertEquals(new BigDecimal("499.90"), visaoItem.precoVigente());
        assertEquals(600, visaoItem.pesoTotalGramas());
        assertEquals(8, visaoItem.disponibilidadeMaximaEstoque());

        // Pertence tanto à categoria filha quanto à categoria pai recursivamente
        assertTrue(this.portaConsulta.itemPertenceACategoriaOuSubcategoria(
            "TEN-MARATONA-42",
            catFilha.obterId()
        ));
        assertTrue(this.portaConsulta.itemPertenceACategoriaOuSubcategoria(
            "TEN-MARATONA-42",
            catPai.obterId()
        ));

        ResumoReservaEstoqueDTO reservaAtiva = this.portaComando.reservarItensParaCheckout(
            "PEDIDO-MODULO3-001",
            cliente.obterId(),
            Map.of("TEN-MARATONA-42", 2)
        );
        assertEquals(EstadoReservaEstoque.ATIVA, reservaAtiva.estado());

        ResumoReservaEstoqueDTO reservaConfirmada =
            this.portaComando.confirmarBaixaDefinitiva("PEDIDO-MODULO3-001");
        assertEquals(EstadoReservaEstoque.CONFIRMADA, reservaConfirmada.estado());
    }
}

