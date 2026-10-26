package br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.TipoOperacaoAuditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta imutável da trilha de auditoria.
 *
 * <p>Responsabilidade Comercial: Armazenar e recuperar eventos administrativos críticos (FR-012)
 * ordenados cronologicamente.</p>
 */
@Repository
public interface RegistroAuditoriaJpaRepository extends JpaRepository<RegistroAuditoria, UUID> {

    /**
     * Lista todos os registros de auditoria em ordem cronológica crescente.
     *
     * <p>Objetivo Funcional: Exibir o histórico completo de operações críticas para o ADMIN.</p>
     * <p>Papel no Design Pattern: Persistência consumida pelos Proxies de auditoria.</p>
     *
     * @return lista ordenada de registros de auditoria
     */
    List<RegistroAuditoria> findAllByOrderByOcorridoEmAsc();

    /**
     * Filtra registros de auditoria pelo tipo de operação administrativa.
     *
     * <p>Objetivo Funcional: Permitir ao administrador auditar especificamente alterações de preço,
     * estoque ou categorias.</p>
     * <p>Papel no Design Pattern: Consulta especializada de auditoria.</p>
     *
     * @param tipoOperacao tipo de operação desejada
     * @return lista de registros correspondentes em ordem cronológica
     */
    List<RegistroAuditoria> findByTipoOperacaoOrderByOcorridoEmAsc(TipoOperacaoAuditoria tipoOperacao);

    /**
     * Filtra registros de auditoria pelo identificador do recurso alvo (ex.: SKU ou slug).
     *
     * <p>Objetivo Funcional: Rastrear todo o histórico de mudanças de um mesmo produto ou categoria.</p>
     * <p>Papel no Design Pattern: Consulta de rastreabilidade por entidade.</p>
     *
     * @param identificadorAlvo SKU ou identificador do recurso
     * @return lista de registros correspondentes em ordem cronológica
     */
    List<RegistroAuditoria> findByIdentificadorAlvoOrderByOcorridoEmAsc(String identificadorAlvo);
}

