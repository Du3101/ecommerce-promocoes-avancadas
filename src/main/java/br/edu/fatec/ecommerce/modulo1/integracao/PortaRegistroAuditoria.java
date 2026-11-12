package br.edu.fatec.ecommerce.modulo1.integracao;

/**
 * Porta de comando fornecida pelo Módulo 1 para que outros módulos gravem registros na trilha de auditoria.
 *
 * <p>Objetivo Funcional: Centralizar a auditoria de operações sensíveis (Princípio V da Constituição) na
 * tabela {@code registros_auditoria} do Módulo 1, mesmo quando a operação acontece em outro módulo —
 * por exemplo, o cancelamento de pedidos do Módulo 3.</p>
 * <p>Papel no Design Pattern: Porta de fronteira (Anti-Corruption Layer). O Módulo 3 não conhece
 * {@code RegistroAuditoria}, {@code Usuario} nem os repositórios do Módulo 1.</p>
 */
public interface PortaRegistroAuditoria {

    /**
     * Registra na trilha de auditoria o cancelamento de um pedido.
     *
     * <p>Objetivo Funcional: Identificar quem cancelou, qual pedido, de qual estado e com qual resultado.</p>
     * <p>Papel no Design Pattern: Comando da porta de auditoria (fronteira entre Módulo 3 e Módulo 1).</p>
     *
     * @param emailSolicitante e-mail do usuário autenticado que solicitou o cancelamento
     * @param numeroPedido número comercial do pedido (ex.: PED-202611-AB12CD)
     * @param estadoAnterior estado do pedido antes do cancelamento
     * @param resumoResultado descrição do resultado (estado final, motivo, estorno, estoque)
     */
    void registrarCancelamentoPedido(
        String emailSolicitante,
        String numeroPedido,
        String estadoAnterior,
        String resumoResultado
    );
}
