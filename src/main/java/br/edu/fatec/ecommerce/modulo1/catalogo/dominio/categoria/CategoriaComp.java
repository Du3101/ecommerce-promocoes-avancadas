package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria;

import br.edu.fatec.ecommerce.compartilhado.excecao.CicloHierarquicoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Entidade composta que representa uma categoria ou subcategoria do catálogo em profundidade arbitrária (FR-007).
 *
 * <p>Aplicação Conjunta dos Padrões GoF: <b>Composite</b> (papel <i>Composite</i>) e
 * <b>Null Object</b> via {@link CategoriaRaizNullObject} (Princípio II da Constituição).</p>
 * <p>Justificativa: Categorias podem conter subcategorias recursivamente (ex.: {@code Vestuário > Camisetas > Esportivas})
 * e precisam responder de forma uniforme a consultas de subárvore de IDs, caminho hierárquico e
 * prevenção de ciclos (auto-referência direta ou indireta).</p>
 */
@Entity
@Table(name = "categorias")
public class CategoriaComp implements ComponenteCategoria {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "slug", length = 120, nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_pai_id")
    private CategoriaComp categoriaPaiPersistencia;

    @OneToMany(mappedBy = "categoriaPaiPersistencia", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CategoriaComp> subcategorias = new ArrayList<>();

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor protegido para uso exclusivo do JPA/Hibernate.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade a partir do banco relacional.</p>
     * <p>Papel no Design Pattern: Preserva o encapsulamento da criação validada.</p>
     */
    protected CategoriaComp() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor público explícito que valida o nome e o slug da categoria.
     *
     * <p>Objetivo Funcional: Criar uma nova categoria (inicialmente raiz até ser vinculada a um pai).</p>
     * <p>Papel no Design Pattern: Inicializa um nó <i>Composite</i> válido na árvore de categorias.</p>
     *
     * @param nome nome comercial da categoria (obrigatório, até 100 caracteres)
     * @param slugOpcional slug opcional (se omitido, é gerado automaticamente a partir do nome)
     * @throws RegraNegocioModulo1Exception se o nome for nulo ou vazio
     */
    public CategoriaComp(String nome, String slugOpcional) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 100) {
            throw new RegraNegocioModulo1Exception(
                "O nome da categoria é obrigatório e deve ter até 100 caracteres."
            );
        }
        this.id = UUID.randomUUID();
        this.nome = nome.trim();
        this.slug = normalizarSlug(slugOpcional != null && !slugOpcional.isBlank() ? slugOpcional : this.nome);
        this.ativa = true;
        this.criadoEm = Instant.now();
    }

    /**
     * Adiciona uma subcategoria filha a esta categoria, bloqueando qualquer formação de ciclo hierárquico (FR-007).
     *
     * <p>Objetivo Funcional: Estruturar a árvore mercadológica impedindo que uma categoria seja filha de si
     * mesma ou de qualquer um de seus descendentes.</p>
     * <p>Papel no Design Pattern: Método estrutural de gestão de filhos (`add`) do padrão <b>Composite</b>
     * com invariante anti-ciclo recursiva.</p>
     *
     * @param subcategoria categoria filha a ser anexada
     * @throws CicloHierarquicoException se a adição gerar ciclo direto ou indireto na árvore
     * @throws RegraNegocioModulo1Exception se a subcategoria for nula
     */
    public void adicionarSubcategoria(CategoriaComp subcategoria) {
        if (subcategoria == null) {
            throw new RegraNegocioModulo1Exception("A subcategoria a ser adicionada não pode ser nula.");
        }
        if (this.id.equals(subcategoria.obterId())
            || subcategoria.contemSubcategoriaNaArvore(this.id)
            || this.possuiAncestralComId(subcategoria.obterId())) {
            throw new CicloHierarquicoException(
                "Operação bloqueada: adicionar a categoria '" + subcategoria.obterNome()
                    + "' dentro de '" + this.nome + "' formaria um ciclo hierárquico."
            );
        }
        subcategoria.definirCategoriaPaiInterno(this);
        if (!this.subcategorias.contains(subcategoria)) {
            this.subcategorias.add(subcategoria);
        }
    }

    /**
     * Verifica recursivamente se o identificador informado existe nesta categoria ou em qualquer descendente.
     *
     * <p>Objetivo Funcional: Detectar ciclos antes de vincular subcategorias.</p>
     * <p>Papel no Design Pattern: Travessia recursiva em profundidade (DFS) no padrão <b>Composite</b>.</p>
     *
     * @param idProcurado UUID da categoria pesquisada
     * @return {@code true} se o ID estiver presente na subárvore; {@code false} caso contrário
     */
    public boolean contemSubcategoriaNaArvore(UUID idProcurado) {
        if (idProcurado == null) {
            return false;
        }
        if (this.id.equals(idProcurado)) {
            return true;
        }
        for (CategoriaComp filha : this.subcategorias) {
            if (filha.contemSubcategoriaNaArvore(idProcurado)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Coleta recursivamente o UUID desta categoria e de todas as suas subcategorias descendentes ativas.
     *
     * <p>Objetivo Funcional: Permitir que uma busca por produtos na categoria pai retorne automaticamente
     * produtos de todas as subcategorias filhas e netas (FR-007).</p>
     * <p>Papel no Design Pattern: Operação composta recursiva do padrão <b>Composite</b>.</p>
     *
     * @return conjunto ordenado de UUIDs de toda a subárvore ativa
     */
    @Override
    public Set<UUID> obterIdsSubarvoreRecursivo() {
        if (!this.ativa) {
            return Collections.emptySet();
        }
        Set<UUID> idsAcumulados = new LinkedHashSet<>();
        idsAcumulados.add(this.id);
        for (CategoriaComp subcategoria : this.subcategorias) {
            idsAcumulados.addAll(subcategoria.obterIdsSubarvoreRecursivo());
        }
        return Collections.unmodifiableSet(idsAcumulados);
    }

    /**
     * Obtém a categoria pai deste nó, retornando {@link CategoriaRaizNullObject} caso seja uma categoria de topo.
     *
     * <p>Objetivo Funcional: Navegar para cima na hierarquia sem risco de {@code NullPointerException}.</p>
     * <p>Papel no Design Pattern: Ponto de integração entre o <b>Composite</b> e o <b>Null Object</b>.</p>
     *
     * @return instância de {@link ComponenteCategoria} representando o pai ou a raiz nula (nunca {@code null})
     */
    public ComponenteCategoria obterCategoriaPai() {
        if (this.categoriaPaiPersistencia == null) {
            return CategoriaRaizNullObject.obterInstancia();
        }
        return this.categoriaPaiPersistencia;
    }

    /**
     * Constrói recursivamente o caminho hierárquico completo desde a raiz até esta categoria.
     *
     * <p>Objetivo Funcional: Gerar trilhas de navegação como {@code "Vestuário > Camisetas > Esportivas"}.</p>
     * <p>Papel no Design Pattern: Delega polimorficamente ao {@link #obterCategoriaPai()} (que pode ser
     * outra {@link CategoriaComp} ou {@link CategoriaRaizNullObject}).</p>
     *
     * @return caminho hierárquico completo separado por {@code " > "}
     */
    @Override
    public String obterCaminhoHierarquico() {
        ComponenteCategoria pai = obterCategoriaPai();
        if (pai.eRaizNula()) {
            return this.nome;
        }
        return pai.obterCaminhoHierarquico() + " > " + this.nome;
    }

    /**
     * Indica que esta instância é uma categoria real do catálogo, e não a raiz nula.
     *
     * <p>Objetivo Funcional: Diferenciar categorias reais do {@link CategoriaRaizNullObject}.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ComponenteCategoria}.</p>
     *
     * @return sempre {@code false}
     */
    @Override
    public boolean eRaizNula() {
        return false;
    }

    /**
     * Obtém o identificador único da categoria.
     *
     * <p>Objetivo Funcional: Identificar a categoria no banco e na API.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ComponenteCategoria}.</p>
     *
     * @return UUID da categoria
     */
    @Override
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o nome comercial da categoria.
     *
     * <p>Objetivo Funcional: Exibir nome da categoria no catálogo.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ComponenteCategoria}.</p>
     *
     * @return nome da categoria
     */
    @Override
    public String obterNome() {
        return this.nome;
    }

    /**
     * Obtém o slug normalizado da categoria.
     *
     * <p>Objetivo Funcional: Identificação textual única da categoria.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ComponenteCategoria}.</p>
     *
     * @return slug em minúsculas separado por hífens
     */
    @Override
    public String obterSlug() {
        return this.slug;
    }

    /**
     * Retorna uma visão imutável das subcategorias filhas diretas desta categoria.
     *
     * <p>Objetivo Funcional: Permitir serialização da árvore completa de categorias.</p>
     * <p>Papel no Design Pattern: Acesso seguro aos filhos (`getChildren`) do padrão <b>Composite</b>.</p>
     *
     * @return lista imutável de {@link CategoriaComp} filhas
     */
    public List<CategoriaComp> obterSubcategorias() {
        return Collections.unmodifiableList(this.subcategorias);
    }

    /**
     * Indica se a categoria está ativa no catálogo.
     *
     * <p>Objetivo Funcional: Controlar visibilidade da categoria e de sua subárvore.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado.</p>
     *
     * @return {@code true} se ativa; {@code false} se inativa
     */
    public boolean estaAtiva() {
        return this.ativa;
    }

    /**
     * Altera o estado ativo/inativo da categoria.
     *
     * <p>Objetivo Funcional: Permitir ativação/inativação lógica sem exclusão física.</p>
     * <p>Papel no Design Pattern: Mutação explícita de estado.</p>
     *
     * @param ativa novo estado de ativação
     */
    public void definirAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    /**
     * Atualiza a referência interna para a categoria pai (chamado por {@link #adicionarSubcategoria}).
     *
     * <p>Objetivo Funcional: Sincronizar o lado N:1 do relacionamento autorreferenciado.</p>
     * <p>Papel no Design Pattern: Manutenção de invariante bidirecional no <b>Composite</b>.</p>
     *
     * @param novoPai categoria pai validada
     */
    private void definirCategoriaPaiInterno(CategoriaComp novoPai) {
        this.categoriaPaiPersistencia = novoPai;
    }

    /**
     * Verifica recursivamente nos ancestrais se algum deles possui o identificador informado.
     *
     * <p>Objetivo Funcional: Reforçar proteção contra ciclos ao subir pela cadeia de pais.</p>
     * <p>Papel no Design Pattern: Verificação ascendente auxiliar no <b>Composite</b>.</p>
     *
     * @param idCandidato UUID a verificar na cadeia de ancestrais
     * @return {@code true} se já for um ancestral desta categoria
     */
    private boolean possuiAncestralComId(UUID idCandidato) {
        CategoriaComp cursor = this.categoriaPaiPersistencia;
        while (cursor != null) {
            if (cursor.obterId().equals(idCandidato)) {
                return true;
            }
            cursor = cursor.categoriaPaiPersistencia;
        }
        return false;
    }

    /**
     * Normaliza um texto em formato slug (minúsculas, sem acentos, separado por hífens).
     *
     * <p>Objetivo Funcional: Garantir formato consistente e único para o campo {@code slug}.</p>
     * <p>Papel no Design Pattern: Utilitário interno de higienização da entidade.</p>
     *
     * @param texto texto base para geração do slug
     * @return slug normalizado
     */
    private static String normalizarSlug(String texto) {
        String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slugGerado = semAcentos.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (slugGerado.isBlank()) {
            throw new RegraNegocioModulo1Exception("Não foi possível gerar um slug válido para a categoria.");
        }
        return slugGerado;
    }
}

