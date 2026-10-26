package br.edu.fatec.ecommerce.modulo1.estoque.api;

import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoReservaEstoqueConcorrente;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ReservaEstoque;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consulta de saldos de estoque e execução das operações transacionais
 * de reserva atômica, confirmação de baixa definitiva e liberação compensatória (FR-009, FR-010, FR-011).
 */
@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueReservaController {

    private final ServicoReservaEstoqueConcorrente servicoReservaEstoque;

    /**
     * Construtor com injeção explícita do serviço de reservas concorrentes.
     *
     * <p>Objetivo Funcional: Expor endpoints de reserva e consulta de estoque.</p>
     * <p>Papel no Design Pattern: Controller REST na fronteira do contexto de Estoque.</p>
     *
     * @param servicoReservaEstoque serviço de reserva e consulta de estoque
     */
    public EstoqueReservaController(ServicoReservaEstoqueConcorrente servicoReservaEstoque) {
        this.servicoReservaEstoque = servicoReservaEstoque;
    }

    /**
     * Consulta a quantidade disponível e reservada de um SKU físico ou a disponibilidade máxima de um Combo.
     *
     * <p>Objetivo Funcional: Informar saldo em estoque em tempo real (FR-009).</p>
     * <p>Papel no Design Pattern: Consulta integrada entre Estoque e <b>Composite</b>.</p>
     *
     * @param sku código SKU da variação ou combo
     * @return resposta HTTP 200 OK com dados da posição de estoque
     */
    @GetMapping("/{sku}")
    public ResponseEntity<Map<String, Object>> consultarEstoquePorSku(@PathVariable String sku) {
        ItemEstoque item = this.servicoReservaEstoque.consultarPosicaoEstoqueOuCombo(sku);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("sku", item.obterSku());
        resposta.put("quantidadeDisponivel", item.obterQuantidadeDisponivel());
        resposta.put("quantidadeReservada", item.obterQuantidadeReservada());
        resposta.put("saldoFisicoTotal", item.obterSaldoFisicoTotal());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Reserva atomicamente múltiplos SKUs (ou componentes de combos) para fechamento de compra (FR-010).
     *
     * <p>Objetivo Funcional: Bloquear unidades em estoque com semântica tudo-ou-nada e TTL configurável.</p>
     * <p>Papel no Design Pattern: Dispara o <b>Static Factory Method</b> de {@link ReservaEstoque}
     * com TTL obtido do <b>Singleton</b>.</p>
     *
     * @param requisicao payload com chave da operação, ID do cliente e mapa de SKUs/quantidades
     * @return resposta HTTP 201 Created com detalhes da reserva ou 409 Conflict se faltar estoque
     */
    @PostMapping("/reservas")
    public ResponseEntity<Map<String, Object>> criarReservaAtomica(
        @Valid @RequestBody RequisicaoCriarReservaDTO requisicao
    ) {
        ReservaEstoque reserva = this.servicoReservaEstoque.reservarItensAtomicamente(
            requisicao.chaveOperacaoPedido(),
            requisicao.clienteId(),
            requisicao.quantidadesPorSku()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearReserva(reserva));
    }

    /**
     * Confirma de forma idempotente a baixa definitiva de uma reserva ativa (FR-011).
     *
     * <p>Objetivo Funcional: Efetivar saída do estoque após aprovação do pagamento.</p>
     * <p>Papel no Design Pattern: Transição de estado idempotente {@code ATIVA -> CONFIRMADA}.</p>
     *
     * @param chaveOperacao chave única da operação de pedido/reserva
     * @return resposta HTTP 200 OK com o estado atualizado da reserva
     */
    @PostMapping("/reservas/{chaveOperacao}/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarReserva(@PathVariable String chaveOperacao) {
        ReservaEstoque reserva = this.servicoReservaEstoque.confirmarBaixaDefinitivaReserva(chaveOperacao);
        return ResponseEntity.ok(mapearReserva(reserva));
    }

    /**
     * Libera de forma idempotente uma reserva ativa devolvendo seu saldo ao estoque disponível (FR-011).
     *
     * <p>Objetivo Funcional: Devolver unidades retidas por cancelamento ou falha de pagamento.</p>
     * <p>Papel no Design Pattern: Transição compensatória idempotente {@code ATIVA -> LIBERADA}.</p>
     *
     * @param chaveOperacao chave única da operação de pedido/reserva
     * @param requisicao payload opcional com o motivo da liberação
     * @return resposta HTTP 200 OK com o estado atualizado da reserva
     */
    @PostMapping("/reservas/{chaveOperacao}/liberar")
    public ResponseEntity<Map<String, Object>> liberarReserva(
        @PathVariable String chaveOperacao,
        @RequestBody(required = false) RequisicaoLiberarReservaDTO requisicao
    ) {
        String motivo = requisicao != null ? requisicao.motivo() : "Cancelamento ou recusa de pagamento";
        ReservaEstoque reserva = this.servicoReservaEstoque.liberarReservaPorFalhaOuCancelamento(
            chaveOperacao,
            motivo
        );
        return ResponseEntity.ok(mapearReserva(reserva));
    }

    /**
     * Converte uma entidade {@link ReservaEstoque} em mapa JSON estruturado.
     *
     * <p>Objetivo Funcional: Formatar dados da reserva para retorno na API REST.</p>
     * <p>Papel no Design Pattern: Método auxiliar de apresentação.</p>
     *
     * @param reserva entidade ReservaEstoque
     * @return mapa JSON da reserva
     */
    private Map<String, Object> mapearReserva(ReservaEstoque reserva) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("reservaId", reserva.obterId());
        mapa.put("chaveOperacaoPedido", reserva.obterChaveOperacaoPedido());
        mapa.put("clienteId", reserva.obterClienteId());
        mapa.put("estado", reserva.obterEstado());
        mapa.put("criadaEm", reserva.obterCriadaEm().toString());
        mapa.put("expiraEm", reserva.obterExpiraEm().toString());
        if (reserva.obterFinalizadaEm() != null) {
            mapa.put("finalizadaEm", reserva.obterFinalizadaEm().toString());
        }
        mapa.put("itensReservados", reserva.obterItens().stream()
            .map(i -> Map.of(
                "sku", i.obterSku(),
                "quantidadeReservada", i.obterQuantidadeReservada()
            ))
            .collect(Collectors.toList()));
        return mapa;
    }

    /**
     * DTO de requisição para criação de reserva atômica de estoque.
     *
     * @param chaveOperacaoPedido chave única da operação
     * @param clienteId UUID do cliente comprador
     * @param quantidadesPorSku mapa associando cada SKU à quantidade solicitada
     */
    public record RequisicaoCriarReservaDTO(
        @NotBlank(message = "A chave da operação é obrigatória.") String chaveOperacaoPedido,
        @NotNull(message = "O clienteId é obrigatório.") UUID clienteId,
        @NotEmpty(message = "Informe ao menos um SKU para reserva.") Map<String, Integer> quantidadesPorSku
    ) {
    }

    /**
     * DTO opcional para informar o motivo de liberação da reserva.
     *
     * @param motivo justificativa do estorno
     */
    public record RequisicaoLiberarReservaDTO(String motivo) {
    }
}

