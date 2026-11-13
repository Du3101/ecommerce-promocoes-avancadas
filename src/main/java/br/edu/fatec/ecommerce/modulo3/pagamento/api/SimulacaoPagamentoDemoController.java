package br.edu.fatec.ecommerce.modulo3.pagamento.api;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.AcessoNegadoModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoRequestDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.ConfirmacaoPagamentoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.dto.SimulacaoPagamentoDemoRequestDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao.ServicoProcessamentoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.api.PerfisAutenticacao;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoConsultaPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint autenticado de simulação controlada de pagamento para ambiente de demonstração.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como Proxy de Conveniência Segura do lado servidor entre a interface web autenticada e o
 * {@link ServicoProcessamentoPagamento}, permitindo testar transições PIX/Boleto para {@code PAGO} e
 * idempotência em demonstração sem expor {@code X-Gateway-Token} no front-end e respeitando a propriedade
 * do pedido ({@code CLIENTE} dono ou {@code ADMIN}).</p>
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pagamentos", description = "Módulo 3 — Simulação controlada de confirmação de pagamento em demonstração")
public class SimulacaoPagamentoDemoController {

    private final ServicoConsultaPedido servicoConsulta;
    private final ServicoProcessamentoPagamento servicoProcessamentoPagamento;
    private final boolean modoDemonstracaoAtivo;

    /**
     * Construtor com injeção explícita dos serviços de consulta, processamento de pagamento e flag demo.
     *
     * <p>Objetivo Funcional: Controlar e executar simulações seguras de confirmação no modo demonstração.</p>
     * <p>Papel no Design Pattern: Injeção de Dependências de controlador REST isolado do token do webhook.</p>
     *
     * @param servicoConsulta serviço responsável por validar existência e propriedade do pedido
     * @param servicoProcessamentoPagamento serviço idempotente do Módulo 3 que processa aprovações/recusas
     * @param modoDemonstracaoAtivo propriedade {@code ecommerce.modulo3.seed-demo-ativo} (padrão {@code false})
     */
    public SimulacaoPagamentoDemoController(
            ServicoConsultaPedido servicoConsulta,
            ServicoProcessamentoPagamento servicoProcessamentoPagamento,
            @Value("${ecommerce.modulo3.seed-demo-ativo:false}") boolean modoDemonstracaoAtivo
    ) {
        this.servicoConsulta = Objects.requireNonNull(servicoConsulta, "servicoConsulta é obrigatório");
        this.servicoProcessamentoPagamento = Objects.requireNonNull(
            servicoProcessamentoPagamento,
            "servicoProcessamentoPagamento é obrigatório"
        );
        this.modoDemonstracaoAtivo = modoDemonstracaoAtivo;
    }

    /**
     * Simula a confirmação ou recusa do pagamento de um pedido autorizado sem expor segredos no cliente web.
     *
     * <p>Objetivo Funcional: Permitir ao avaliador testar transição para {@code PAGO}, baixa definitiva
     * de estoque, eventos Observer e reexecução idempotente diretamente da tela de confirmação.</p>
     * <p>Papel no Design Pattern: Controlador proxy delegado para o mesmo motor transacional idempotente
     * do webhook oficial ({@link ServicoProcessamentoPagamento}).</p>
     *
     * @param id UUID do pedido
     * @param requisicao decisão simulada ({@code sucesso = true/false} e motivo opcional)
     * @param autenticacao contexto do usuário autenticado ({@code CLIENTE} proprietário ou {@code ADMIN})
     * @return resultado da notificação incluindo sinalizador {@code idempotenteReexecutado}
     * @throws AcessoNegadoModulo3Exception se o modo demonstração estiver desativado ou se o pedido pertencer a outro cliente
     */
    @PostMapping("/{id}/simular-confirmacao-pagamento")
    @Operation(summary = "Simula confirmação/recusa do pagamento do próprio pedido (exclusivo modo demonstração)")
    public ConfirmacaoPagamentoResponseDTO simularConfirmacaoPagamento(
            @PathVariable UUID id,
            @Valid @RequestBody SimulacaoPagamentoDemoRequestDTO requisicao,
            Authentication autenticacao
    ) {
        if (!modoDemonstracaoAtivo) {
            throw new AcessoNegadoModulo3Exception(
                "Simulação controlada de pagamento está disponível apenas com o modo demonstração ativo."
            );
        }

        boolean administrador = PerfisAutenticacao.ehAdministrador(autenticacao);
        PedidoResponseDTO pedido = servicoConsulta.consultarPedido(id, autenticacao.getName(), administrador);

        boolean sucesso = Boolean.TRUE.equals(requisicao.sucesso());
        String idTransacaoGateway = pedido.pagamento() != null
                && pedido.pagamento().identificadorTransacao() != null
                && !pedido.pagamento().identificadorTransacao().isBlank()
                ? pedido.pagamento().identificadorTransacao()
                : "DEMO-SIM-" + pedido.numeroPedido();

        String motivoRecusa = null;
        if (!sucesso) {
            motivoRecusa = requisicao.motivoRecusa() != null && !requisicao.motivoRecusa().isBlank()
                    ? requisicao.motivoRecusa().trim()
                    : "Recusa simulada em demonstração pela interface web.";
        }

        ConfirmacaoPagamentoRequestDTO notificacao = new ConfirmacaoPagamentoRequestDTO(
                pedido.numeroPedido(),
                idTransacaoGateway,
                sucesso,
                sucesso ? pedido.snapshotFinanceiro().valorTotalLiquido() : null,
                motivoRecusa
        );
        return servicoProcessamentoPagamento.processarNotificacaoGateway(notificacao);
    }
}
