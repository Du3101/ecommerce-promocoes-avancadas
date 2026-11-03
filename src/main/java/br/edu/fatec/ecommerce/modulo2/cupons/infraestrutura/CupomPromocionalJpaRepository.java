package br.edu.fatec.ecommerce.modulo2.cupons.infraestrutura;

import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para busca e persistência de cupons promocionais.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Infraestrutura de persistência para cupons, fornecendo consulta rápida pelo código único.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Repository
public interface CupomPromocionalJpaRepository extends JpaRepository<CupomPromocional, UUID> {

    /**
     * Localiza um cupom pelo código alfanumérico (insensível a maiúsculas/minúsculas).
     *
     * @param codigo Código do cupom informado pelo cliente.
     * @return Optional contendo o cupom se encontrado.
     */
    Optional<CupomPromocional> findByCodigoIgnoreCase(String codigo);
}

