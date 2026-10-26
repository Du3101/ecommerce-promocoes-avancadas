package br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de contas {@link Usuario}.
 *
 * <p>Responsabilidade na Arquitetura: Abstrair o acesso à tabela {@code usuarios} para os
 * fluxos de autenticação, auto-cadastro de clientes e provisão de operadores (FR-001, FR-002).</p>
 */
@Repository
public interface UsuarioJpaRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca uma conta de usuário pelo endereço de e-mail ignorando diferenças de caixa.
     *
     * <p>Objetivo Funcional: Localizar a credencial de login do usuário durante a autenticação.</p>
     * <p>Papel no Design Pattern: Consulta de repositório de infraestrutura orientada ao domínio.</p>
     *
     * @param email endereço de e-mail a pesquisar
     * @return {@link Optional} contendo o {@link Usuario} se encontrado
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe uma conta cadastrada com o e-mail informado (ignorando caixa).
     *
     * <p>Objetivo Funcional: Prevenir duplicidade de e-mail antes da criação de novas contas.</p>
     * <p>Papel no Design Pattern: Predicado de unicidade na camada de persistência.</p>
     *
     * @param email endereço de e-mail a verificar
     * @return {@code true} se o e-mail já estiver em uso; {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);
}

