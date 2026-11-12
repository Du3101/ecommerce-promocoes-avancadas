package br.edu.fatec.ecommerce.modulo3.pedido.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaRegistroAuditoria;
import br.edu.fatec.ecommerce.modulo3.mediador.CoordenadorCancelamentoMediator;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import org.springframework.stereotype.Component;

/**
 * Cancelamento solicitado por um administrador (atendimento, fraude, falta de produto).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Class</i> do Template Method de cancelamento:
 * o administrador pode cancelar pedidos de qualquer cliente; o perfil já foi verificado pelo Spring Security.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CancelamentoPeloAdministrador extends ServicoCancelamentoPedido {

    /**
     * Construtor com dependências injetadas.
     *
     * @param mediador mediador das compensações
     * @param portaAuditoria porta de auditoria do Módulo 1
     */
    public CancelamentoPeloAdministrador(CoordenadorCancelamentoMediator mediador, PortaRegistroAuditoria portaAuditoria) {
        super(mediador, portaAuditoria);
    }

    /**
     * Origem da solicitação.
     *
     * @return {@code ADMINISTRADOR}
     */
    @Override
    protected String origemSolicitacao() {
        return "ADMINISTRADOR";
    }

    /**
     * Administradores podem cancelar qualquer pedido (o perfil ADMIN é exigido na rota).
     *
     * @param pedido pedido
     * @param emailSolicitante e-mail do administrador
     */
    @Override
    protected void validarAutorizacao(Pedido pedido, String emailSolicitante) {
        // Sem restrição de propriedade: a autorização por perfil é feita pelo Spring Security.
    }
}
