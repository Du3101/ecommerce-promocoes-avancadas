package br.edu.fatec.ecommerce.modulo1.auditoria.api;

import br.edu.fatec.ecommerce.modulo1.auditoria.aplicacao.ServicoConsultaAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.TipoOperacaoAuditoria;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST exclusivo para o perfil {@code ADMIN} consultar a trilha imutável
 * de auditoria de operações críticas (FR-012).
 */
@RestController
@RequestMapping("/api/v1/admin/auditoria")
public class AuditoriaAdminController {

    private final ServicoConsultaAuditoria servicoConsultaAuditoria;

    /**
     * Construtor com injeção explícita do serviço de consulta de auditoria.
     *
     * <p>Objetivo Funcional: Expor endpoint de consulta filtrada da trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Controller REST de inspeção dos registros criados pelos <b>Proxies</b>.</p>
     *
     * @param servicoConsultaAuditoria serviço de aplicação de auditoria
     */
    public AuditoriaAdminController(ServicoConsultaAuditoria servicoConsultaAuditoria) {
        this.servicoConsultaAuditoria = servicoConsultaAuditoria;
    }

    /**
     * Lista os registros de auditoria filtrados opcionalmente por usuário, tipo de operação e período.
     *
     * <p>Objetivo Funcional: Permitir rastreabilidade completa de todas as mudanças administrativas (FR-012).</p>
     * <p>Papel no Design Pattern: Endpoint de consulta de auditoria.</p>
     *
     * @param usuarioId UUID opcional do operador responsável
     * @param tipoOperacao tipo opcional da operação ({@link TipoOperacaoAuditoria})
     * @param inicio instante ISO-8601 inicial opcional
     * @param fim instante ISO-8601 final opcional
     * @return resposta HTTP 200 OK com lista cronológica de registros de auditoria
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> consultarRegistrosAuditoria(
        @RequestParam(name = "usuarioId", required = false) UUID usuarioId,
        @RequestParam(name = "tipoOperacao", required = false) TipoOperacaoAuditoria tipoOperacao,
        @RequestParam(name = "inicio", required = false) Instant inicio,
        @RequestParam(name = "fim", required = false) Instant fim
    ) {
        List<RegistroAuditoria> registros = this.servicoConsultaAuditoria.consultarTrilhaAuditoria(
            usuarioId,
            tipoOperacao,
            inicio,
            fim
        );

        List<Map<String, Object>> resposta = registros.stream()
            .map(this::mapearRegistroAuditoria)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Converte uma entidade {@link RegistroAuditoria} em mapa JSON estruturado.
     *
     * <p>Objetivo Funcional: Expor todos os campos imutáveis do evento auditado.</p>
     * <p>Papel no Design Pattern: Serialização de apresentação.</p>
     *
     * @param registro entidade RegistroAuditoria
     * @return mapa JSON com os dados do registro
     */
    private Map<String, Object> mapearRegistroAuditoria(RegistroAuditoria registro) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", registro.obterId());
        mapa.put("usuarioResponsavelId", registro.obterUsuarioResponsavelId());
        mapa.put("emailResponsavel", registro.obterEmailResponsavel());
        mapa.put("perfilResponsavel", registro.obterPerfilResponsavel());
        mapa.put("tipoOperacao", registro.obterTipoOperacao());
        mapa.put("tipoEntidadeAlvo", registro.obterTipoEntidadeAlvo());
        mapa.put("identificadorAlvo", registro.obterIdentificadorAlvo());
        mapa.put("valorAnteriorResumo", registro.obterValorAnteriorResumo());
        mapa.put("valorNovoResumo", registro.obterValorNovoResumo());
        mapa.put("ocorridoEm", registro.obterOcorridoEm().toString());
        return mapa;
    }
}

