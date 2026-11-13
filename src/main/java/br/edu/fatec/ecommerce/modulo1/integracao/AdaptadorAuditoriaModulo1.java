package br.edu.fatec.ecommerce.modulo1.integracao;

import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.UsuarioJpaRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação da {@link PortaRegistroAuditoria} sobre a trilha de auditoria do Módulo 1.
 *
 * <p>Objetivo Funcional: Traduzir o pedido de auditoria vindo de outro módulo em um {@link RegistroAuditoria}
 * persistido, resolvendo o usuário responsável e seu perfil a partir do contexto de segurança.</p>
 * <p>Papel no Design Pattern: <b>Adapter</b> de fronteira (implementa a porta e delega para o domínio
 * de auditoria do Módulo 1), no mesmo estilo dos proxies de auditoria de catálogo e estoque.</p>
 */
@Service
public class AdaptadorAuditoriaModulo1 implements PortaRegistroAuditoria {

    private final RegistroAuditoriaJpaRepository auditoriaRepository;
    private final UsuarioJpaRepository usuarioRepository;

    /**
     * Construtor com injeção explícita de dependências.
     *
     * @param auditoriaRepository repositório da trilha de auditoria
     * @param usuarioRepository repositório de usuários (resolução do ID do responsável)
     */
    public AdaptadorAuditoriaModulo1(
        RegistroAuditoriaJpaRepository auditoriaRepository,
        UsuarioJpaRepository usuarioRepository
    ) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Persiste o registro de auditoria do cancelamento de um pedido.
     *
     * <p>Objetivo Funcional: Gravar responsável, perfil, pedido, estado anterior, resultado e instante.</p>
     * <p>Papel no Design Pattern: Método adaptado da porta — traduz a chamada do Módulo 3 para o
     * <b>Static Factory Method</b> {@link RegistroAuditoria#registrarCancelamentoPedido}.</p>
     *
     * @param emailSolicitante e-mail do solicitante (se nulo, usa o usuário do contexto de segurança)
     * @param numeroPedido número comercial do pedido
     * @param estadoAnterior estado anterior do pedido
     * @param resumoResultado descrição do resultado do cancelamento
     */
    @Override
    @Transactional
    public void registrarCancelamentoPedido(
        String emailSolicitante,
        String numeroPedido,
        String estadoAnterior,
        String resumoResultado
    ) {
        // 1. Resolve o e-mail efetivo (parâmetro explícito ou usuário autenticado)
        String email = emailSolicitante != null && !emailSolicitante.isBlank()
            ? emailSolicitante
            : ContextoSegurancaUsuario.obterUsernameOuPadrao("sistema@ecommerce.fatec.edu.br");

        // 2. Resolve o perfil principal do solicitante (cliente ou administrativo)
        PerfilUsuario perfil = ContextoSegurancaUsuario.obterPerfilPrincipalOuPadrao(PerfilUsuario.CLIENTE);

        // 3. Resolve o ID real do usuário (ou um UUID determinístico para chamadas de sistema)
        UUID usuarioId = this.usuarioRepository.findByEmailIgnoreCase(email)
            .map(Usuario::obterId)
            .orElseGet(() -> ContextoSegurancaUsuario.derivarUuidDeterministicoPorEmail(email));

        // 4. Cria o registro imutável pelo Static Factory Method e persiste
        RegistroAuditoria registro = RegistroAuditoria.registrarCancelamentoPedido(
            usuarioId,
            email,
            perfil,
            numeroPedido,
            estadoAnterior,
            resumoResultado
        );
        this.auditoriaRepository.save(registro);
    }
}
