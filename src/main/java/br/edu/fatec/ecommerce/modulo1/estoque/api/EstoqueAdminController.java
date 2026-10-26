package br.edu.fatec.ecommerce.modulo1.estoque.api;

import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoAdminEstoquePort;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST administrativo para ajustes manuais de saldo de estoque e expurgo de
 * reservas vencidas, restrito aos perfis {@code ADMIN} e {@code RESPONSAVEL_ESTOQUE} (FR-009, FR-011, FR-012).
 *
 * <p>Delega todas as chamadas à porta {@link ServicoAdminEstoquePort}, sendo interceptado
 * transparentemente pelo {@code EstoqueAdminSegurancaProxy} (padrão <b>Proxy</b>).</p>
 */
@RestController
@RequestMapping("/api/v1/admin/estoque")
public class EstoqueAdminController {

    private final ServicoAdminEstoquePort servicoAdminEstoque;

    /**
     * Construtor com injeção explícita da porta administrativa de estoque (Proxy).
     *
     * <p>Objetivo Funcional: Expor endpoints de gestão de estoque protegidos e auditados.</p>
     * <p>Papel no Design Pattern: Cliente do padrão <b>Proxy</b> ({@link ServicoAdminEstoquePort}).</p>
     *
     * @param servicoAdminEstoque porta administrativa de estoque (injetada via {@code @Primary} Proxy)
     */
    public EstoqueAdminController(ServicoAdminEstoquePort servicoAdminEstoque) {
        this.servicoAdminEstoque = servicoAdminEstoque;
    }

    /**
     * Realiza entrada ou ajuste manual de saldo físico disponível de um SKU (FR-009, FR-012).
     *
     * <p>Objetivo Funcional: Atualizar saldo de inventário gerando movimentação e trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Operação interceptada pelo {@code EstoqueAdminSegurancaProxy} (<b>Proxy</b>).</p>
     *
     * @param sku código SKU da variação física
     * @param requisicao payload com delta de quantidade e motivo obrigatório
     * @return resposta HTTP 200 OK com os saldos atualizados do SKU
     */
    @PostMapping("/{sku}/ajustes")
    public ResponseEntity<Map<String, Object>> ajustarEstoqueManual(
        @PathVariable String sku,
        @Valid @RequestBody RequisicaoAjusteEstoqueDTO requisicao
    ) {
        ItemEstoque atualizado = this.servicoAdminEstoque.ajustarSaldoDisponivelManual(
            sku,
            requisicao.deltaQuantidade(),
            requisicao.motivo()
        );
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("sku", atualizado.obterSku());
        resposta.put("quantidadeDisponivel", atualizado.obterQuantidadeDisponivel());
        resposta.put("quantidadeReservada", atualizado.obterQuantidadeReservada());
        resposta.put("saldoFisicoTotal", atualizado.obterSaldoFisicoTotal());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Aciona a varredura e expiração de todas as reservas ativas cujo TTL já venceu (FR-011).
     *
     * <p>Objetivo Funcional: Devolver saldos retidos em reservas vencidas ao estoque disponível.</p>
     * <p>Papel no Design Pattern: Operação administrativa protegida pelo <b>Proxy</b>.</p>
     *
     * @return resposta HTTP 200 OK informando o total de reservas expiradas
     */
    @PostMapping("/reservas/expirar-vencidas")
    public ResponseEntity<Map<String, Object>> expirarReservasVencidas() {
        int totalExpiradas = this.servicoAdminEstoque.acionarExpiracaoDeReservasVencidas();
        return ResponseEntity.ok(Map.of("reservasExpiradas", totalExpiradas));
    }

    /**
     * DTO de requisição para ajuste manual de saldo de estoque.
     *
     * @param deltaQuantidade variação de unidades (positiva para entrada, negativa para baixa)
     * @param motivo justificativa obrigatória do ajuste
     */
    public record RequisicaoAjusteEstoqueDTO(
        @NotNull(message = "A variação de quantidade (deltaQuantidade) é obrigatória.") Integer deltaQuantidade,
        @NotBlank(message = "O motivo do ajuste é obrigatório.") String motivo
    ) {
    }
}

