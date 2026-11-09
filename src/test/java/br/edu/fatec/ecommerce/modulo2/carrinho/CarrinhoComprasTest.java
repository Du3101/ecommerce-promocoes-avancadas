package br.edu.fatec.ecommerce.modulo2.carrinho;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários do agregado {@link CarrinhoCompras} e elementos de item.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
class CarrinhoComprasTest {

    @Test
    @DisplayName("Deve inicializar carrinho limpo e calcular totais zerados")
    void deveInicializarCarrinhoLimpo() {
        UUID clienteId = UUID.randomUUID();
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), clienteId, null);

        assertNotNull(carrinho.getId());
        assertEquals(clienteId, carrinho.getClienteId());
        assertEquals(0, carrinho.calcularQuantidadeTotalItens());
        assertEquals(0, carrinho.calcularPesoTotalGramas());
        assertEquals(new BigDecimal("0.00"), carrinho.calcularSubtotalBruto());
        assertTrue(carrinho.getItens().isEmpty());
    }

    @Test
    @DisplayName("Deve adicionar itens e calcular subtotal e peso acumulado com precisão")
    void deveAdicionarItensECalcularTotais() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), null, "sessao-123");

        ItemCarrinhoProduto teclado = new ItemCarrinhoProduto(
                UUID.randomUUID(),
                carrinho.getId(),
                "TEC-01",
                "Teclado Mecânico",
                new BigDecimal("300.00"),
                2,
                850,
                UUID.randomUUID()
        );

        ItemCarrinhoComboKit kit = new ItemCarrinhoComboKit(
                UUID.randomUUID(),
                carrinho.getId(),
                "KIT-01",
                "Kit Gamer",
                new BigDecimal("500.00"),
                1,
                1200,
                UUID.randomUUID()
        );

        carrinho.adicionarOuIncrementarItem(teclado);
        carrinho.adicionarOuIncrementarItem(kit);

        assertEquals(2, carrinho.getItens().size());
        assertEquals(3, carrinho.calcularQuantidadeTotalItens());
        assertEquals(2900, carrinho.calcularPesoTotalGramas()); // (850*2) + 1200
        assertEquals(new BigDecimal("1100.00"), carrinho.calcularSubtotalBruto()); // (300*2) + 500
    }

    @Test
    @DisplayName("Deve incrementar quantidade ao adicionar o mesmo SKU repetidamente")
    void deveIncrementarQuantidadeAoAdicionarMesmoSku() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);

        ItemCarrinhoProduto item1 = new ItemCarrinhoProduto(
                UUID.randomUUID(),
                carrinho.getId(),
                "MOU-01",
                "Mouse Óptico",
                new BigDecimal("150.00"),
                1,
                120,
                UUID.randomUUID()
        );

        ItemCarrinhoProduto item2 = new ItemCarrinhoProduto(
                UUID.randomUUID(),
                carrinho.getId(),
                "MOU-01",
                "Mouse Óptico",
                new BigDecimal("150.00"),
                2,
                120,
                UUID.randomUUID()
        );

        carrinho.adicionarOuIncrementarItem(item1);
        carrinho.adicionarOuIncrementarItem(item2);

        assertEquals(1, carrinho.getItens().size());
        assertEquals(3, carrinho.calcularQuantidadeTotalItens());
        assertEquals(new BigDecimal("450.00"), carrinho.calcularSubtotalBruto());
    }

    @Test
    @DisplayName("Deve atualizar quantidade, remover item e esvaziar carrinho com sucesso")
    void deveAtualizarRemoverEEsvaziarCarrinho() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);

        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "PROD-A", "Produto A", new BigDecimal("50.00"), 2, 100, null
        ));
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "PROD-B", "Produto B", new BigDecimal("80.00"), 1, 200, null
        ));

        // Atualizar quantidade
        carrinho.atualizarQuantidadeItem("PROD-A", 4);
        assertEquals(new BigDecimal("280.00"), carrinho.calcularSubtotalBruto()); // (50*4) + 80

        // Remover item
        boolean removido = carrinho.removerItem("PROD-B");
        assertTrue(removido);
        assertEquals(1, carrinho.getItens().size());
        assertEquals(new BigDecimal("200.00"), carrinho.calcularSubtotalBruto());

        // Esvaziar
        carrinho.esvaziar();
        assertEquals(0, carrinho.getItens().size());
        assertEquals(new BigDecimal("0.00"), carrinho.calcularSubtotalBruto());
    }

    @Test
    @DisplayName("Deve rejeitar quantidades não positivas")
    void deveRejeitarQuantidadesInvalidas() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThrows(IllegalArgumentException.class, () -> new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "PROD-X", "Invalido", new BigDecimal("10.00"), 0, 10, null
        ));

        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "PROD-OK", "Valido", new BigDecimal("10.00"), 1, 10, null
        ));

        assertThrows(IllegalArgumentException.class, () -> carrinho.atualizarQuantidadeItem("PROD-OK", 0));
        assertThrows(IllegalArgumentException.class, () -> carrinho.atualizarQuantidadeItem("PROD-OK", -5));
    }
}
