package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura.AtributoVariacaoFlyweightJpaRepository;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fábrica gerenciadora do pool compartilhado de instâncias {@link AtributoVariacaoFlyweight} (FR-006).
 *
 * <p>Aplicação do Padrão GoF: <b>Flyweight</b> — papel <i>FlyweightFactory</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Garante que solicitações repetidas para o mesmo par {@code (tipo, valor)}
 * retornem exatamente a mesma referência de objeto em memória ({@code ==}), evitando proliferação
 * de objetos idênticos entre centenas de variações de produtos.</p>
 */
@Component
public class FabricaAtributoVariacaoFlyweight {

    private final Map<String, AtributoVariacaoFlyweight> poolCompartilhado = new ConcurrentHashMap<>();
    private final AtributoVariacaoFlyweightJpaRepository repositorio;

    /**
     * Construtor padrão para uso em testes unitários puramente em memória (sem banco).
     *
     * <p>Objetivo Funcional: Permitir instanciação direta da fábrica em testes de unidade.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>FlyweightFactory</i> com pool em memória.</p>
     */
    public FabricaAtributoVariacaoFlyweight() {
        this.repositorio = null;
    }

    /**
     * Construtor injetado pelo Spring contendo o repositório JPA para sincronização persistente.
     *
     * <p>Objetivo Funcional: Integrar o pool em memória com a tabela {@code atributos_variacao_flyweight}.</p>
     * <p>Papel no Design Pattern: Hidrata e sincroniza o pool da <i>FlyweightFactory</i>.</p>
     *
     * @param repositorio repositório JPA de atributos Flyweight
     */
    @Autowired
    public FabricaAtributoVariacaoFlyweight(AtributoVariacaoFlyweightJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Obtém uma instância existente do pool compartilhado ou cria e registra uma nova caso ainda não exista.
     *
     * <p>Objetivo Funcional: Compartilhar a mesma instância imutável de {@link AtributoVariacaoFlyweight}
     * entre múltiplas variações de produtos.</p>
     * <p>Papel no Design Pattern: Método central {@code getFlyweight(intrinsicState)} da <b>FlyweightFactory</b>.</p>
     *
     * @param tipo classificação do atributo ({@link TipoAtributoVariacao})
     * @param valor valor nominal do atributo (ex.: "M", "Preto")
     * @param codigoApresentacao código visual complementar (ex.: "TAM-M", "#000000")
     * @return instância compartilhada de {@link AtributoVariacaoFlyweight}
     */
    public synchronized AtributoVariacaoFlyweight obterOuCriar(
        TipoAtributoVariacao tipo,
        String valor,
        String codigoApresentacao
    ) {
        String chaveCanonica = gerarChaveCanonica(tipo, valor);
        AtributoVariacaoFlyweight emCache = this.poolCompartilhado.get(chaveCanonica);
        if (emCache != null) {
            if (this.repositorio != null && !this.repositorio.existsById(emCache.obterId())) {
                this.repositorio.save(emCache);
            }
            return emCache;
        }

        if (this.repositorio != null) {
            Optional<AtributoVariacaoFlyweight> existenteNoBanco =
                this.repositorio.findByTipoAndValorIgnoreCase(tipo, valor.trim());
            if (existenteNoBanco.isPresent()) {
                AtributoVariacaoFlyweight doBanco = existenteNoBanco.get();
                this.poolCompartilhado.put(chaveCanonica, doBanco);
                return doBanco;
            }
        }

        AtributoVariacaoFlyweight novoFlyweight = new AtributoVariacaoFlyweight(
            UUID.randomUUID(),
            tipo,
            valor,
            codigoApresentacao
        );
        if (this.repositorio != null) {
            this.repositorio.save(novoFlyweight);
        }
        this.poolCompartilhado.put(chaveCanonica, novoFlyweight);
        return novoFlyweight;
    }

    /**
     * Retorna a quantidade atual de instâncias únicas mantidas no pool em memória.
     *
     * <p>Objetivo Funcional: Permitir inspeção e auditoria da economia de memória proporcionada pelo Flyweight.</p>
     * <p>Papel no Design Pattern: Métrica de monitoramento do pool da <i>FlyweightFactory</i>.</p>
     *
     * @return número de instâncias únicas no pool
     */
    public int obterTotalInstanciasNoPool() {
        return this.poolCompartilhado.size();
    }

    /**
     * Limpa o cache em memória da fábrica (útil entre cenários isolados de teste).
     *
     * <p>Objetivo Funcional: Reiniciar o estado do pool em memória quando solicitado em testes.</p>
     * <p>Papel no Design Pattern: Gerenciamento de ciclo de vida do pool <b>Flyweight</b>.</p>
     */
    public void limparPoolEmMemoria() {
        this.poolCompartilhado.clear();
    }

    /**
     * Constrói a chave canônica normalizada {@code TIPO:VALOR_MAIUSCULO} para indexação no pool.
     *
     * <p>Objetivo Funcional: Evitar duplicidade por diferenças de letras maiúsculas/minúsculas ou espaços.</p>
     * <p>Papel no Design Pattern: Normalização da chave intrínseca da <i>FlyweightFactory</i>.</p>
     *
     * @param tipo tipo do atributo
     * @param valor valor textual
     * @return chave canônica única
     */
    private String gerarChaveCanonica(TipoAtributoVariacao tipo, String valor) {
        if (tipo == null || valor == null || valor.isBlank()) {
            throw new RegraNegocioModulo1Exception(
                "Tipo e valor são obrigatórios para obter um AtributoVariacaoFlyweight."
            );
        }
        return tipo.name() + ":" + valor.trim().toUpperCase(Locale.ROOT);
    }
}

