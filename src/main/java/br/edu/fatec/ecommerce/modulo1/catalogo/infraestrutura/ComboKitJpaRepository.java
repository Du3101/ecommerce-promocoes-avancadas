package br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de pacotes compostos {@link ComboKitComposite}.
 *
 * <p>Responsabilidade na Arquitetura: Abstrair o acesso às tabelas {@code combos_kits} e
 * {@code combo_itens_componentes} (FR-008).</p>
 */
@Repository
public interface ComboKitJpaRepository extends JpaRepository<ComboKitComposite, UUID> {

    /**
     * Busca um combo/kit promocional pelo seu código SKU ignorando caixa.
     *
     * <p>Objetivo Funcional: Localizar um pacote composto pelo SKU nas consultas do catálogo e carrinho.</p>
     * <p>Papel no Design Pattern: Recuperação do nó <i>Composite</i> por SKU.</p>
     *
     * @param sku código SKU do combo
     * @return {@link Optional} contendo o {@link ComboKitComposite} se encontrado
     */
    Optional<ComboKitComposite> findBySkuIgnoreCase(String sku);

    /**
     * Verifica se já existe um combo/kit cadastrado com o código SKU informado.
     *
     * <p>Objetivo Funcional: Garantir unicidade global de SKU no catálogo.</p>
     * <p>Papel no Design Pattern: Predicado de unicidade na persistência.</p>
     *
     * @param sku código SKU a verificar
     * @return {@code true} se o SKU já pertencer a um combo
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Lista todos os combos/kits vinculados a qualquer uma das categorias informadas.
     *
     * <p>Objetivo Funcional: Incluir combos promocionais nas buscas recursivas por subárvore de categoria.</p>
     * <p>Papel no Design Pattern: Consulta conjunta dos nós <i>Composite</i> por subárvore de categorias.</p>
     *
     * @param categoriaIds coleção de UUIDs de categorias
     * @return lista de combos pertencentes às categorias informadas
     */
    List<ComboKitComposite> findByCategoriaPrincipalIdIn(Collection<UUID> categoriaIds);
}

