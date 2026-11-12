package br.edu.fatec.ecommerce.modulo3.pedido.api;

import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.ResultadoCancelamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoConsultaPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.aplicacao.ServicoOperacoesPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de pedidos do cliente: histórico, detalhe e cancelamento.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "Módulo 3 — Consulta e cancelamento de pedidos (State + Mediator)")
public class PedidoClienteController {

    private final ServicoConsultaPedido servicoConsulta;
    private final ServicoOperacoesPedido servicoOperacoes;

    /**
     * Construtor com serviços injetados.
     *
     * @param servicoConsulta consultas de pedidos
     * @param servicoOperacoes operações de ciclo de vida
     */
    public PedidoClienteController(ServicoConsultaPedido servicoConsulta, ServicoOperacoesPedido servicoOperacoes) {
        this.servicoConsulta = servicoConsulta;
        this.servicoOperacoes = servicoOperacoes;
    }

    /**
     * Lista os pedidos do cliente autenticado.
     *
     * @param autenticacao usuário autenticado
     * @return pedidos do mais recente para o mais antigo
     */
    @GetMapping("/me")
    @Operation(summary = "Lista os pedidos do cliente autenticado")
    public List<PedidoResponseDTO> meusPedidos(Authentication autenticacao) {
        return servicoConsulta.listarPedidosDoCliente(autenticacao.getName());
    }

    /**
     * Detalha um pedido (cliente: só os próprios; administrador: qualquer um).
     *
     * @param id UUID do pedido
     * @param autenticacao usuário autenticado
     * @return detalhe do pedido
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalha um pedido (clientes só acessam os próprios)")
    public PedidoResponseDTO detalhar(@PathVariable UUID id, Authentication autenticacao) {
        return servicoConsulta.consultarPedido(id, autenticacao.getName(), PerfisAutenticacao.ehAdministrador(autenticacao));
    }

    /**
     * Cancela um pedido, liberando/devolvendo o estoque e estornando se já estiver pago.
     *
     * @param id UUID do pedido
     * @param motivo motivo do cancelamento (opcional)
     * @param autenticacao usuário autenticado
     * @return resultado do cancelamento
     */
    @DeleteMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela o pedido (bloqueado pelo State se já enviado/entregue)")
    public ResultadoCancelamentoDTO cancelar(@PathVariable UUID id,
                                             @RequestParam(required = false) String motivo,
                                             Authentication autenticacao) {
        return servicoOperacoes.cancelar(id, motivo, autenticacao.getName(), PerfisAutenticacao.ehAdministrador(autenticacao));
    }
}
