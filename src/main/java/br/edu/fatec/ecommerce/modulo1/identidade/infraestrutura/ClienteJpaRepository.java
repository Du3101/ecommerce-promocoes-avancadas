package br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de perfis {@link Cliente}.
 *
 * <p>Responsabilidade na Arquitetura: Abstrair o acesso à tabela {@code clientes} e seus
 * endereços agregados para consultas de perfil e elegibilidade (FR-001, FR-003, FR-004, FR-013).</p>
 */
@Repository
public interface ClienteJpaRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Busca o perfil de cliente associado ao identificador de uma conta de usuário.
     *
     * <p>Objetivo Funcional: Recuperar os dados comerciais do cliente autenticado.</p>
     * <p>Papel no Design Pattern: Consulta de repositório para recuperação do agregado {@link Cliente}.</p>
     *
     * @param usuarioId UUID da conta de usuário
     * @return {@link Optional} contendo o {@link Cliente} correspondente
     */
    Optional<Cliente> findByUsuarioId(UUID usuarioId);

    /**
     * Verifica se já existe um cliente cadastrado com o CPF informado.
     *
     * <p>Objetivo Funcional: Garantir unicidade cadastral por CPF no auto-cadastro de clientes.</p>
     * <p>Papel no Design Pattern: Predicado de unicidade na camada de persistência.</p>
     *
     * @param cpf CPF numérico de 11 dígitos
     * @return {@code true} se o CPF já estiver cadastrado; {@code false} caso contrário
     */
    boolean existsByCpf(String cpf);
}

