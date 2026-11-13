package br.edu.fatec.ecommerce.modulo2.promocoes.api;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Projeção pública de campanhas promocionais vigentes exibidas na vitrine da loja virtual.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * DTO de saída imutável da camada de apresentação pública, expondo metadados comerciais
 * amigáveis ao cliente final sem vazar parâmetros técnicos internos ou regras privadas JSON.</p>
 *
 * @param id identificador único da campanha promocional
 * @param nome título comercial da campanha
 * @param descricao descrição exibida nos banners da vitrine
 * @param tipoMecanica enum da modalidade de promoção do motor
 * @param descricaoMecanica descrição legível associada à modalidade
 * @param prioridade prioridade de avaliação no resolvedor de conflitos
 * @param cumulativa indica se a campanha acumula com outras ou com cupons
 * @param dataInicio instante inicial de vigência
 * @param dataFim instante final de vigência (opcional para campanhas contínuas)
 */
public record CampanhaPublicaResponseDTO(
    UUID id,
    String nome,
    String descricao,
    TipoMecanicaPromocao tipoMecanica,
    String descricaoMecanica,
    int prioridade,
    boolean cumulativa,
    Instant dataInicio,
    Instant dataFim
) {

    /**
     * Converte uma entidade {@link CampanhaPromocional} em sua representação pública de vitrine.
     *
     * <p>Objetivo Funcional: Mapear os atributos públicos da campanha garantindo rótulo legível
     * da mecânica comercial.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> para isolamento entre domínio e REST.</p>
     *
     * @param campanha entidade promocional vigente
     * @return instância imutável de {@link CampanhaPublicaResponseDTO}
     * @throws NullPointerException quando {@code campanha} for nulo
     */
    public static CampanhaPublicaResponseDTO de(CampanhaPromocional campanha) {
        Objects.requireNonNull(campanha, "Campanha promocional não pode ser nula ao gerar DTO público.");
        TipoMecanicaPromocao tipo = campanha.getTipoMecanica();
        String rotuloMecanica = tipo != null ? tipo.getDescricaoExibicao() : "Promoção";
        return new CampanhaPublicaResponseDTO(
            campanha.getId(),
            campanha.getNome(),
            campanha.getDescricao(),
            tipo,
            rotuloMecanica,
            campanha.getPrioridade(),
            campanha.isCumulativa(),
            campanha.getDataInicio(),
            campanha.getDataFim()
        );
    }
}
