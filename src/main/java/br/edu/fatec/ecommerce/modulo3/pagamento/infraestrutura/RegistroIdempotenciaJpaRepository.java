package br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.RegistroIdempotenciaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para registros de controle de idempotência estrita.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Repositório de persistência do mecanismo de idempotência estrita (Constituição, Princípio IV).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Repository
public interface RegistroIdempotenciaJpaRepository extends JpaRepository<RegistroIdempotenciaPagamento, String> {
}
