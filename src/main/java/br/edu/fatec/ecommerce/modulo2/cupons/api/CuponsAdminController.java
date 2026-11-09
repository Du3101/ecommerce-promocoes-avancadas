package br.edu.fatec.ecommerce.modulo2.cupons.api;

import br.edu.fatec.ecommerce.modulo2.cupons.aplicacao.ServicoCuponsPromocionais;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.TipoDescontoCupom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller administrativo para gerenciamento de cupons de desconto.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Interface REST protegida por RBAC para provisão e acompanhamento de cupons comerciais.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@RestController
@RequestMapping("/api/v1/admin/cupons")
@Tag(name = "Módulo 2 — Gestão Administrativa de Cupons", description = "Endpoints restritos a ADMIN e RESPONSAVEL_PROMOCOES")
@SecurityRequirement(name = "basicAuth")
public class CuponsAdminController {

    private final ServicoCuponsPromocionais servicoCupons;

    public CuponsAdminController(ServicoCuponsPromocionais servicoCupons) {
        this.servicoCupons = servicoCupons;
    }

    public record CriarCupomRequest(
            @NotBlank(message = "Código é obrigatório") String codigo,
            @NotNull(message = "Tipo de desconto é obrigatório") TipoDescontoCupom tipoDesconto,
            @NotNull(message = "Valor de desconto é obrigatório") BigDecimal valorDesconto,
            BigDecimal valorMinimoCarrinho,
            int quantidadeMaximaUsos,
            Instant expiraEm,
            boolean cumulativoComPromocoes
    ) {}

    public record CupomResponse(
            UUID id,
            String codigo,
            TipoDescontoCupom tipoDesconto,
            BigDecimal valorDesconto,
            BigDecimal valorMinimoCarrinho,
            int quantidadeMaximaUsos,
            int quantidadeUsosAtuais,
            Instant expiraEm,
            boolean cumulativoComPromocoes,
            boolean ativo
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL_PROMOCOES')")
    @Operation(summary = "Lista todos os cupons promocionais cadastrados")
    public ResponseEntity<List<CupomResponse>> listarCupons() {
        List<CupomResponse> lista = servicoCupons.listarCupons().stream()
                .map(this::mapearParaResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL_PROMOCOES')")
    @Operation(summary = "Cria um novo cupom promocional com limites e vigência")
    public ResponseEntity<CupomResponse> criarCupom(@Valid @RequestBody CriarCupomRequest request) {
        CupomPromocional cupom = servicoCupons.criarCupom(
                request.codigo(),
                request.tipoDesconto(),
                request.valorDesconto(),
                request.valorMinimoCarrinho() != null ? request.valorMinimoCarrinho() : BigDecimal.ZERO,
                request.quantidadeMaximaUsos(),
                request.expiraEm(),
                request.cumulativoComPromocoes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearParaResponse(cupom));
    }

    private CupomResponse mapearParaResponse(CupomPromocional c) {
        return new CupomResponse(
                c.getId(),
                c.getCodigo(),
                c.getTipoDesconto(),
                c.getValorDesconto(),
                c.getValorMinimoCarrinho(),
                c.getQuantidadeMaximaUsos(),
                c.getQuantidadeUsosAtuais(),
                c.getExpiraEm(),
                c.isCumulativoComPromocoes(),
                c.isAtivo()
        );
    }
}

