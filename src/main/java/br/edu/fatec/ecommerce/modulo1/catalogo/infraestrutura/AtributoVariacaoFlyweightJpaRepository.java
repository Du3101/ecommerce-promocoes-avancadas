package br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.TipoAtributoVariacao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência das tuplas intrínsecas {@link AtributoVariacaoFlyweight}.
 *
 * <p>Responsabilidade na Arquitetura: Sincronizar o pool da {@code FabricaAtributoVariacaoFlyweight}
 * com a tabela {@code atributos_variacao_flyweight} (FR-006).</p>
 */
@Repository
public interface AtributoVariacaoFlyweightJpaRepository extends JpaRepository<AtributoVariacaoFlyweight, UUID> {

    /**
     * Busca um atributo Flyweight existente pelo par {@code (tipo, valor)} ignorando caixa no valor.
     *
     * <p>Objetivo Funcional: Reaproveitar registros já existentes no banco de dados.</p>
     * <p>Papel no Design Pattern: Suporte persistente ao pool da <b>FlyweightFactory</b>.</p>
     *
     * @param tipo tipo do atributo
     * @param valor valor nominal do atributo
     * @return {@link Optional} com a instância persistida se existir
     */
    Optional<AtributoVariacaoFlyweight> findByTipoAndValorIgnoreCase(TipoAtributoVariacao tipo, String valor);
}

