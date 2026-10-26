package br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Endereco;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para consulta e persistência de entidades {@link Endereco}.
 *
 * <p>Responsabilidade na Arquitetura: Suportar operações de persistência sobre a tabela
 * {@code enderecos_cliente} (FR-003).</p>
 */
@Repository
public interface EnderecoJpaRepository extends JpaRepository<Endereco, UUID> {

    /**
     * Lista todos os endereços pertencentes a um determinado cliente.
     *
     * <p>Objetivo Funcional: Consultar os endereços de entrega vinculados ao comprador.</p>
     * <p>Papel no Design Pattern: Método de consulta de repositório por chave estrangeira.</p>
     *
     * @param clienteId UUID do cliente proprietário dos endereços
     * @return lista de endereços do cliente
     */
    List<Endereco> findByClienteId(UUID clienteId);
}

