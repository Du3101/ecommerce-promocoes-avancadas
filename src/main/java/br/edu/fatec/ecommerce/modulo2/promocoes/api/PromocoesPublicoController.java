package br.edu.fatec.ecommerce.modulo2.promocoes.api;

import br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura.CampanhaPromocionalJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST público para exibição de campanhas promocionais ativas e vigentes na vitrine.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Fronteira HTTP somente leitura para a loja pública (Módulo 2 → Portal Web), filtrando
 * exclusivamente campanhas ativas cuja janela temporal abrange o instante corrente.</p>
 */
@RestController
@RequestMapping("/api/v1/promocoes")
@Tag(name = "Promoções Públicas", description = "Consulta pública de campanhas ativas na vitrine da loja")
public class PromocoesPublicoController {

    private final CampanhaPromocionalJpaRepository campanhaRepository;

    /**
     * Construtor com injeção explícita do repositório de campanhas promocionais.
     *
     * <p>Objetivo Funcional: Prover acesso de leitura às campanhas cadastradas no Módulo 2.</p>
     * <p>Papel no Design Pattern: Controlador REST com Injeção de Dependências por construtor.</p>
     *
     * @param campanhaRepository repositório JPA de campanhas promocionais
     */
    public PromocoesPublicoController(CampanhaPromocionalJpaRepository campanhaRepository) {
        this.campanhaRepository = Objects.requireNonNull(
            campanhaRepository,
            "campanhaRepository é obrigatório em PromocoesPublicoController."
        );
    }

    /**
     * Lista as campanhas promocionais atualmente ativas e dentro do período de vigência,
     * ordenadas por prioridade decrescente.
     *
     * <p>Objetivo Funcional: Alimentar os banners promocionais públicos da página inicial sem exigir login.</p>
     * <p>Papel no Design Pattern: Fachada REST de consulta delegando a verificação de janela temporal
     * para {@link br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional#isVigenteNoMomento(Instant)}.</p>
     *
     * @return HTTP 200 OK com lista ordenada de {@link CampanhaPublicaResponseDTO}
     */
    @GetMapping("/ativas")
    @Operation(summary = "Lista campanhas promocionais ativas e vigentes para exibição pública na vitrine")
    public ResponseEntity<List<CampanhaPublicaResponseDTO>> listarCampanhasAtivas() {
        Instant agora = Instant.now();
        List<CampanhaPublicaResponseDTO> ativas = campanhaRepository.listarCampanhasAtivasPorPrioridade()
            .stream()
            .filter(campanha -> campanha.isVigenteNoMomento(agora))
            .map(CampanhaPublicaResponseDTO::de)
            .toList();
        return ResponseEntity.ok(ativas);
    }
}
