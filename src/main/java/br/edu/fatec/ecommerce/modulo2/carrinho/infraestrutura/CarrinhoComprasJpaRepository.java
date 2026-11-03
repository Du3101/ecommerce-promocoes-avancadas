package br.edu.fatec.ecommerce.modulo2.carrinho.infraestrutura;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para persistência e recuperação do agregado {@link CarrinhoCompras}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Camada de infraestrutura que abstrai o armazenamento relacional do agregado do carrinho,
 * suportando busca pelo ID do cliente ou pelo token de sessão temporária anônima.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Repository
public interface CarrinhoComprasJpaRepository extends JpaRepository<CarrinhoCompras, UUID> {

    /**
     * Localiza o carrinho ativo vinculado a um cliente cadastrado.
     *
     * @param clienteId UUID do cliente.
     * @return Optional com o carrinho se existir.
     */
    Optional<CarrinhoCompras> findByClienteId(UUID clienteId);

    /**
     * Localiza o carrinho vinculado a uma sessão anônima.
     *
     * @param sessaoId String da sessão.
     * @return Optional com o carrinho se existir.
     */
    Optional<CarrinhoCompras> findBySessaoId(String sessaoId);
}

