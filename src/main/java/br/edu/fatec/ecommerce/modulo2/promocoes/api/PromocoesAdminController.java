package br.edu.fatec.ecommerce.modulo2.promocoes.api;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura.CampanhaPromocionalJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller administrativo para gestão de campanhas promocionais.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Controla os casos de uso de gestão do motor de promoções, protegido por RBAC
 * com as roles {@code ADMIN} e {@code RESPONSAVEL_PROMOCOES}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@RestController
@RequestMapping("/api/v1/admin/promocoes")
@Tag(name = "Módulo 2 — Gestão Administrativa de Promoções", description = "Endpoints restritos a ADMIN e RESPONSAVEL_PROMOCOES")
@SecurityRequirement(name = "basicAuth")
public class PromocoesAdminController {

    private final CampanhaPromocionalJpaRepository campanhaRepository;

    public PromocoesAdminController(CampanhaPromocionalJpaRepository campanhaRepository) {
        this.campanhaRepository = campanhaRepository;
    }

    public record CriarCampanhaRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "Descrição é obrigatória") String descricao,
            @NotNull(message = "Tipo de mecânica é obrigatório") TipoMecanicaPromocao tipoMecanica,
            String parametrosJson,
            int prioridade,
            boolean cumulativa,
            Instant dataInicio,
            Instant dataFim
    ) {}

    public record CampanhaResponse(
            UUID id,
            String nome,
            String descricao,
            TipoMecanicaPromocao tipoMecanica,
            int prioridade,
            boolean cumulativa,
            boolean ativa,
            Instant dataInicio,
            Instant dataFim
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL_PROMOCOES')")
    @Operation(summary = "Lista todas as campanhas promocionais cadastradas")
    public ResponseEntity<List<CampanhaResponse>> listarCampanhas() {
        List<CampanhaResponse> lista = campanhaRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL_PROMOCOES')")
    @Operation(summary = "Cadastra uma nova campanha promocional")
    public ResponseEntity<CampanhaResponse> criarCampanha(@Valid @RequestBody CriarCampanhaRequest request) {
        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(),
                request.nome(),
                request.descricao(),
                request.tipoMecanica(),
                request.parametrosJson(),
                request.prioridade(),
                request.cumulativa(),
                true,
                request.dataInicio(),
                request.dataFim(),
                "{}",
                null
        );

        CampanhaPromocional salva = campanhaRepository.save(campanha);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearParaResponse(salva));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL_PROMOCOES')")
    @Transactional
    @Operation(summary = "Ativa ou desativa uma campanha promocional")
    public ResponseEntity<CampanhaResponse> alterarStatus(@PathVariable UUID id, @RequestParam boolean ativo) {
        CampanhaPromocional campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha com ID '" + id + "' não encontrada."));
        campanha.setAtiva(ativo);
        CampanhaPromocional atualizada = campanhaRepository.save(campanha);
        return ResponseEntity.ok(mapearParaResponse(atualizada));
    }

    private CampanhaResponse mapearParaResponse(CampanhaPromocional c) {
        return new CampanhaResponse(
                c.getId(),
                c.getNome(),
                c.getDescricao(),
                c.getTipoMecanica(),
                c.getPrioridade(),
                c.isCumulativa(),
                c.isAtiva(),
                c.getDataInicio(),
                c.getDataFim()
        );
    }
}

