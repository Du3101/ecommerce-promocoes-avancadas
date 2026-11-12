package br.edu.fatec.ecommerce.modulo3.pedido.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo1.integracao.PortaRegistroAuditoria;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.AcessoNegadoModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.mediador.CoordenadorCancelamentoMediator;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Cancelamento solicitado pelo próprio cliente.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Class</i> do Template Method de cancelamento:
 * o cliente só pode cancelar pedidos que são dele.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CancelamentoPeloCliente extends ServicoCancelamentoPedido {

    private final PortaConsultaCatalogoCliente portaCliente;

    /**
     * Construtor com dependências injetadas.
     *
     * @param mediador mediador das compensações
     * @param portaAuditoria porta de auditoria do Módulo 1
     * @param portaCliente porta de clientes do Módulo 1
     */
    public CancelamentoPeloCliente(CoordenadorCancelamentoMediator mediador, PortaRegistroAuditoria portaAuditoria,
                                   PortaConsultaCatalogoCliente portaCliente) {
        super(mediador, portaAuditoria);
        this.portaCliente = portaCliente;
    }

    /**
     * Origem da solicitação.
     *
     * @return {@code CLIENTE}
     */
    @Override
    protected String origemSolicitacao() {
        return "CLIENTE";
    }

    /**
     * Garante que o pedido pertence ao cliente autenticado.
     *
     * @param pedido pedido
     * @param emailSolicitante e-mail do cliente autenticado
     * @throws AcessoNegadoModulo3Exception se o pedido for de outro cliente
     */
    @Override
    protected void validarAutorizacao(Pedido pedido, String emailSolicitante) {
        UUID clienteId = portaCliente.resolverClienteIdPorEmailUsuario(emailSolicitante);
        if (!pedido.pertenceAoCliente(clienteId)) {
            throw new AcessoNegadoModulo3Exception("Você só pode cancelar os seus próprios pedidos.");
        }
    }
}
