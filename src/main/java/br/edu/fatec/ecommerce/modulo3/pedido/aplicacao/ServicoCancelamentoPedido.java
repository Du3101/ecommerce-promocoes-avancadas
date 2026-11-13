package br.edu.fatec.ecommerce.modulo3.pedido.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaRegistroAuditoria;
import br.edu.fatec.ecommerce.modulo3.mediador.CoordenadorCancelamentoMediator;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.ResultadoCancelamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Fluxo invariante de cancelamento de pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Template Method</i>. O método
 * {@link #cancelar(Pedido, String, String)} é {@code final} e fixa a ordem: autorizar o solicitante → montar o
 * motivo → delegar as compensações ao <i>Mediator</i> → auditar. As especializações (cliente e administrador) só
 * mudam quem pode cancelar e como o motivo é registrado.
 *
 * <p>Esta classe não abre transação: quem a chama ({@link ServicoOperacoesPedido}) já está em uma transação e já
 * carregou o pedido com lock pessimista.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public abstract class ServicoCancelamentoPedido {

    private final CoordenadorCancelamentoMediator mediador;
    private final PortaRegistroAuditoria portaAuditoria;

    /**
     * Construtor com os colaboradores comuns.
     *
     * @param mediador mediador das compensações
     * @param portaAuditoria porta de auditoria do Módulo 1
     */
    protected ServicoCancelamentoPedido(CoordenadorCancelamentoMediator mediador, PortaRegistroAuditoria portaAuditoria) {
        this.mediador = mediador;
        this.portaAuditoria = portaAuditoria;
    }

    /**
     * MÉTODO TEMPLATE: cancela o pedido na ordem invariante.
     *
     * @param pedido pedido carregado com lock
     * @param motivoInformado motivo digitado pelo solicitante (pode ser vazio)
     * @param emailSolicitante e-mail do usuário autenticado
     * @return resultado do cancelamento
     */
    public final ResultadoCancelamentoDTO cancelar(Pedido pedido, String motivoInformado, String emailSolicitante) {
        // 1. Quem pode cancelar este pedido? (abstrato)
        validarAutorizacao(pedido, emailSolicitante);
        // 2. Motivo padronizado com a origem da solicitação (hook)
        String motivo = montarMotivo(motivoInformado);
        StatusPedido statusAnterior = pedido.getStatus();
        // 3. Compensações coordenadas pelo Mediator (State valida a transição primeiro)
        CoordenadorCancelamentoMediator.ResultadoCompensacao compensacao = mediador.coordenarCancelamento(pedido, motivo);
        // 4. Trilha de auditoria (Princípio V)
        portaAuditoria.registrarCancelamentoPedido(emailSolicitante, pedido.getNumeroPedido(), statusAnterior.name(),
                String.format("status=%s origem=%s motivo=%s estorno=%s estoque=%s", pedido.getStatus(),
                        origemSolicitacao(), motivo, compensacao.estornoRealizado(), compensacao.acaoEstoque()));
        return new ResultadoCancelamentoDTO(pedido.getId(), pedido.getNumeroPedido(), statusAnterior,
                pedido.getStatus(), motivo, compensacao.estornoRealizado(), compensacao.protocoloEstorno(),
                compensacao.acaoEstoque());
    }

    /**
     * Identificação da origem da solicitação, gravada no motivo e na auditoria.
     *
     * <p>Papel no Design Pattern: método abstrato do Template Method.</p>
     *
     * @return ex.: {@code CLIENTE} ou {@code ADMINISTRADOR}
     */
    protected abstract String origemSolicitacao();

    /**
     * Verifica se o solicitante pode cancelar o pedido.
     *
     * <p>Papel no Design Pattern: método abstrato do Template Method.</p>
     *
     * @param pedido pedido
     * @param emailSolicitante e-mail do usuário autenticado
     * @throws br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.AcessoNegadoModulo3Exception se não puder
     */
    protected abstract void validarAutorizacao(Pedido pedido, String emailSolicitante);

    /**
     * Monta o motivo registrado no pedido.
     *
     * <p>Papel no Design Pattern: <b>Hook Method</b> com implementação padrão.</p>
     *
     * @param motivoInformado motivo digitado (pode ser vazio)
     * @return motivo no formato {@code [ORIGEM] texto}
     */
    protected String montarMotivo(String motivoInformado) {
        String texto = motivoInformado == null || motivoInformado.isBlank()
                ? "Cancelamento solicitado"
                : motivoInformado.trim();
        String motivo = "[" + origemSolicitacao() + "] " + texto;
        return motivo.length() > 255 ? motivo.substring(0, 255) : motivo;
    }
}
