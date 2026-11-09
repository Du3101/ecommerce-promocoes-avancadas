package br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para gestão e consulta de campanhas promocionais.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Prover acesso persistente às campanhas promocionais cadastradas, com ordenação
 * determinística prévia de prioridade descendente.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Repository
public interface CampanhaPromocionalJpaRepository extends JpaRepository<CampanhaPromocional, UUID> {

    /**
     * Retorna todas as campanhas ativas ordenadas pela prioridade mais alta primeiro.
     *
     * @return Lista de campanhas ativas ordenadas por prioridade decrescente.
     */
    @Query("SELECT c FROM CampanhaPromocional c WHERE c.ativa = true ORDER BY c.prioridade DESC")
    List<CampanhaPromocional> listarCampanhasAtivasPorPrioridade();
}
