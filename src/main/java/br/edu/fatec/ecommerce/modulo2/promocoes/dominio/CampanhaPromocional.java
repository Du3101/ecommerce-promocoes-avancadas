package br.edu.fatec.ecommerce.modulo2.promocoes.dominio;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoElegibilidadeComponent;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que representa uma campanha promocional ativa ou histórica.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a campanha comercial que implementa {@link Comparable} para ordenação
 * determinística de prioridade (Constituição, Princípio II) e encapsula a raiz da árvore
 * de elegibilidade do padrão <i>Composite</i> ({@link CondicaoElegibilidadeComponent}).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@Table(name = "campanhas_promocionais")
public class CampanhaPromocional implements Comparable<CampanhaPromocional> {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 160)
    private String nome;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mecanica", nullable = false, length = 60)
    private TipoMecanicaPromocao tipoMecanica;

    @Column(name = "parametros_json", nullable = false, columnDefinition = "TEXT")
    private String parametrosJson;

    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Column(name = "cumulativa", nullable = false)
    private boolean cumulativa;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "data_inicio")
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "arvore_elegibilidade_json", nullable = false, columnDefinition = "TEXT")
    private String arvoreElegibilidadeJson;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Transient
    private CondicaoElegibilidadeComponent condicaoElegibilidade;

    /**
     * Construtor protegido exigido pelo JPA.
     */
    protected CampanhaPromocional() {
    }

    /**
     * Construtor completo da campanha promocional.
     *
     * <p>Objetivo funcional: Criação de nova regra promocional.
     * <p>Papel no padrão: Inicialização com definição de prioridade e elegibilidade.
     *
     * @param id Identificador da campanha.
     * @param nome Nome comercial.
     * @param descricao Detalhes da regra.
     * @param tipoMecanica Modalidade de desconto.
     * @param parametrosJson Parâmetros específicos em formato serializado.
     * @param prioridade Grau de prioridade (números maiores prevalecem).
     * @param cumulativa Se pode acumular com outras promoções.
     * @param ativa Se está ativa.
     * @param dataInicio Início da vigência.
     * @param dataFim Término da vigência.
     * @param arvoreElegibilidadeJson Representação serializada da árvore.
     * @param condicaoElegibilidade Raiz instanciada do padrão Composite.
     */
    public CampanhaPromocional(
            UUID id,
            String nome,
            String descricao,
            TipoMecanicaPromocao tipoMecanica,
            String parametrosJson,
            int prioridade,
            boolean cumulativa,
            boolean ativa,
            Instant dataInicio,
            Instant dataFim,
            String arvoreElegibilidadeJson,
            CondicaoElegibilidadeComponent condicaoElegibilidade
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.nome = Objects.requireNonNull(nome, "Nome da campanha não pode ser nulo").trim();
        this.descricao = Objects.requireNonNull(descricao, "Descrição não pode ser nula").trim();
        this.tipoMecanica = Objects.requireNonNull(tipoMecanica, "Tipo de mecânica não pode ser nulo");
        this.parametrosJson = parametrosJson != null ? parametrosJson : "{}";
        this.prioridade = prioridade;
        this.cumulativa = cumulativa;
        this.ativa = ativa;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.arvoreElegibilidadeJson = arvoreElegibilidadeJson != null ? arvoreElegibilidadeJson : "{}";
        this.condicaoElegibilidade = condicaoElegibilidade;
        this.criadoEm = Instant.now();
    }

    /**
     * Verifica se a campanha está dentro do período temporal de vigência.
     *
     * @param agora Instante atual.
     * @return true se vigente; false se antes do início ou expirada.
     */
    public boolean isVigenteNoMomento(Instant agora) {
        if (!ativa) {
            return false;
        }
        if (dataInicio != null && agora.isBefore(dataInicio)) {
            return false;
        }
        if (dataFim != null && agora.isAfter(dataFim)) {
            return false;
        }
        return true;
    }

    /**
     * Avalia a elegibilidade do contexto frente à árvore do padrão Composite.
     *
     * <p>Objetivo funcional: Verifica se cliente e carrinho satisfazem todos os predicados.
     * <p>Papel no padrão: Delegação polimórfica para a raiz do Composite.
     *
     * @param contexto Dados do carrinho e cliente.
     * @return true se elegível; false caso contrário.
     */
    public boolean isElegivel(ContextoElegibilidade contexto) {
        if (!ativa) {
            return false;
        }
        if (condicaoElegibilidade == null) {
            return true; // Sem restrições específicas
        }
        return condicaoElegibilidade.avaliar(contexto);
    }

    /**
     * Implementação determinística de ordenação para resolução de conflitos entre promoções.
     *
     * <p>Objetivo funcional: Define a ordem em que as promoções disputam a aplicação no carrinho.
     * <p>Papel no padrão: Implementação da interface {@link Comparable} exigida pela Constituição.
     * Promoções com maior prioridade numérica são posicionadas primeiro. Em caso de empate,
     * utiliza critério determinístico alfabético por nome e desempate por UUID.
     *
     * @param outra Outra campanha promocional a ser comparada.
     * @return Inteiro negativo se esta tiver maior prioridade, positivo se menor, ou 0 se idêntica.
     */
    @Override
    public int compareTo(CampanhaPromocional outra) {
        if (outra == null) {
            return -1;
        }
        // Maior prioridade numérica primeiro (ordem decrescente)
        int cmpPrioridade = Integer.compare(outra.prioridade, this.prioridade);
        if (cmpPrioridade != 0) {
            return cmpPrioridade;
        }
        int cmpNome = this.nome.compareToIgnoreCase(outra.nome);
        if (cmpNome != 0) {
            return cmpNome;
        }
        return this.id.compareTo(outra.id);
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoMecanicaPromocao getTipoMecanica() {
        return tipoMecanica;
    }

    public String getParametrosJson() {
        return parametrosJson;
    }

    public void setParametrosJson(String parametrosJson) {
        this.parametrosJson = parametrosJson;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public boolean isCumulativa() {
        return cumulativa;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Instant getDataInicio() {
        return dataInicio;
    }

    public Instant getDataFim() {
        return dataFim;
    }

    public String getArvoreElegibilidadeJson() {
        return arvoreElegibilidadeJson;
    }

    public void setArvoreElegibilidadeJson(String arvoreElegibilidadeJson) {
        this.arvoreElegibilidadeJson = arvoreElegibilidadeJson;
    }

    public CondicaoElegibilidadeComponent getCondicaoElegibilidade() {
        return condicaoElegibilidade;
    }

    public void setCondicaoElegibilidade(CondicaoElegibilidadeComponent condicaoElegibilidade) {
        this.condicaoElegibilidade = condicaoElegibilidade;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}

