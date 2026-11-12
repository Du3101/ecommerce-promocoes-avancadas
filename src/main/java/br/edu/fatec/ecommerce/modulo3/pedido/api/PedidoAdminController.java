package br.edu.fatec.ecommerce.modulo3.pedido.api;

import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoOperacoesPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos de expedição (transições do State após o pagamento).
 *
 * <p>Restritos a {@code ADMIN} e {@code OPERADOR_COMERCIAL} pela configuração de segurança.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/admin/pedidos")
@Tag(name = "Pedidos", description = "Módulo 3 — Consulta e cancelamento de pedidos (State + Mediator)")
public class PedidoAdminController {

    private final ServicoOperacoesPedido servicoOperacoes;

    /**
     * Construtor com o serviço injetado.
     *
     * @param servicoOperacoes operações de ciclo de vida
     */
    public PedidoAdminController(ServicoOperacoesPedido servicoOperacoes) {
        this.servicoOperacoes = servicoOperacoes;
    }

    /**
     * Inicia a separação ({@code PAGO -> EM_PREPARACAO}).
     *
     * @param id UUID do pedido
     * @return pedido atualizado
     */
    @PatchMapping("/{id}/preparar")
    @Operation(summary = "Inicia a separação do pedido pago (PAGO -> EM_PREPARACAO)")
    public PedidoResponseDTO preparar(@PathVariable UUID id) {
        return servicoOperacoes.iniciarPreparacao(id);
    }

    /**
     * Despacha o pedido ({@code EM_PREPARACAO -> ENVIADO}).
     *
     * @param id UUID do pedido
     * @param codigoRastreio código de rastreio (opcional; gerado se ausente)
     * @return pedido atualizado
     */
    @PatchMapping("/{id}/despachar")
    @Operation(summary = "Despacha o pedido para a transportadora (EM_PREPARACAO -> ENVIADO)")
    public PedidoResponseDTO despachar(@PathVariable UUID id, @RequestParam(required = false) String codigoRastreio) {
        return servicoOperacoes.despachar(id, codigoRastreio);
    }

    /**
     * Registra a entrega ({@code ENVIADO -> ENTREGUE}).
     *
     * @param id UUID do pedido
     * @return pedido atualizado
     */
    @PatchMapping("/{id}/entregar")
    @Operation(summary = "Registra a entrega do pedido (ENVIADO -> ENTREGUE)")
    public PedidoResponseDTO entregar(@PathVariable UUID id) {
        return servicoOperacoes.confirmarEntrega(id);
    }
}
