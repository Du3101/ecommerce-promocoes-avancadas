package br.edu.fatec.ecommerce.modulo3.pedido.api;

import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoConsultaPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoOperacoesPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos de listagem e expedição (transições do State após o pagamento).
 *
 * <p>Restritos a {@code ADMIN} e {@code OPERADOR_COMERCIAL} pela configuração de segurança.</p>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/admin/pedidos")
@Tag(name = "Pedidos", description = "Módulo 3 — Consulta, expedição e cancelamento de pedidos (State + Mediator)")
public class PedidoAdminController {

    private final ServicoOperacoesPedido servicoOperacoes;
    private final ServicoConsultaPedido servicoConsulta;

    /**
     * Construtor com serviços administrativos de operação e consulta injetados explicitamente.
     *
     * <p>Objetivo Funcional: Habilitar tanto a visualização da fila geral quanto os comandos de expedição.</p>
     * <p>Papel no Design Pattern: Controlador REST com Injeção de Dependências explícita.</p>
     *
     * @param servicoOperacoes operações de transição do ciclo de vida do pedido
     * @param servicoConsulta serviço de consultas de pedidos
     */
    public PedidoAdminController(
            ServicoOperacoesPedido servicoOperacoes,
            ServicoConsultaPedido servicoConsulta
    ) {
        this.servicoOperacoes = Objects.requireNonNull(servicoOperacoes, "servicoOperacoes é obrigatório");
        this.servicoConsulta = Objects.requireNonNull(servicoConsulta, "servicoConsulta é obrigatório");
    }

    /**
     * Lista todos os pedidos da loja ordenados do mais recente ao mais antigo, com filtro opcional por status.
     *
     * <p>Objetivo Funcional: Alimentar a aba de expedição e monitoramento de pedidos no painel administrativo.</p>
     * <p>Papel no Design Pattern: Fachada REST de leitura administrativa sem bypass de DTO.</p>
     *
     * @param status filtro opcional pelo estado do pedido ({@link StatusPedido})
     * @return lista ordenada de {@link PedidoResponseDTO}
     */
    @GetMapping
    @Operation(summary = "Lista todos os pedidos da loja para gestão e expedição administrativa")
    public List<PedidoResponseDTO> listarTodos(@RequestParam(required = false) StatusPedido status) {
        return servicoConsulta.listarTodosPedidos(status);
    }

    /**
     * Inicia a separação ({@code PAGO -> EM_PREPARACAO}).
     *
     * <p>Objetivo Funcional: Avançar o pedido pago para preparação logística em estoque.</p>
     * <p>Papel no Design Pattern: Dispara transição polimórfica do padrão <b>State</b>.</p>
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
     * <p>Objetivo Funcional: Registrar código de rastreio e marcar o pedido como enviado.</p>
     * <p>Papel no Design Pattern: Dispara transição polimórfica do padrão <b>State</b>.</p>
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
     * <p>Objetivo Funcional: Concluir o ciclo logístico do pedido após entrega confirmada.</p>
     * <p>Papel no Design Pattern: Dispara transição polimórfica terminal do padrão <b>State</b>.</p>
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
