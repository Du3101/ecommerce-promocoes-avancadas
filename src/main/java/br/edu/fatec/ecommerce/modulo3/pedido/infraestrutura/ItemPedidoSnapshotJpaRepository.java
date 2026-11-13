package br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.ItemPedidoSnapshot;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para persistência e relatórios analíticos dos itens de snapshot de pedidos.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Componente de persistência e suporte a agregações analíticas (Relatórios Gerenciais).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Repository
public interface ItemPedidoSnapshotJpaRepository extends JpaRepository<ItemPedidoSnapshot, UUID> {

    /**
     * Retorna a agregação dos produtos mais vendidos considerando apenas pedidos faturados.
     * Retorna array de objetos: [sku, nome, somaQuantidade, somaReceitaLiquida].
     *
     * @param statuses Lista de status faturados.
     * @return Lista de agregações dos produtos mais vendidos.
     */
    @Query("SELECT i.sku, i.nome, SUM(i.quantidade), SUM(i.subtotalLiquido) " +
           "FROM ItemPedidoSnapshot i " +
           "WHERE i.pedido.status IN :statuses " +
           "GROUP BY i.sku, i.nome " +
           "ORDER BY SUM(i.quantidade) DESC")
    List<Object[]> encontrarProdutosMaisVendidos(@Param("statuses") List<StatusPedido> statuses);
}
