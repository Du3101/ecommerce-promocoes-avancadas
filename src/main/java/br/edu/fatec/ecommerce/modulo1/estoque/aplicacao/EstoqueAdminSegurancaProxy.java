package br.edu.fatec.ecommerce.modulo1.estoque.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.AcessoNegadoModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import br.edu.fatec.ecommerce.modulo1.identidade.infraestrutura.UsuarioJpaRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proxy de Proteção (RBAC) e Auditoria Automática para operações administrativas de Estoque (FR-002, FR-009, FR-012).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papéis <i>Protection Proxy</i> e <i>Audit Proxy</i>
 * (Princípio II da Constituição).</p>
 * <p>Justificativa: Implementa {@link ServicoAdminEstoquePort} e envolve o {@link ServicoAdminEstoqueReal}
 * como {@code @Primary}. Bloqueia qualquer usuário que não possua perfil {@code ADMIN} ou
 * {@code RESPONSAVEL_ESTOQUE} e persiste automaticamente um {@link RegistroAuditoria} com o saldo
 * anterior, o saldo novo e o motivo do ajuste.</p>
 */
@Service
@Primary
public class EstoqueAdminSegurancaProxy implements ServicoAdminEstoquePort {

    private final ServicoAdminEstoquePort servicoReal;
    private final ItemEstoqueJpaRepository estoqueRepository;
    private final RegistroAuditoriaJpaRepository auditoriaRepository;
    private final UsuarioJpaRepository usuarioRepository;

    /**
     * Construtor com injeção explícita do sujeito real qualificado e dos repositórios necessários.
     *
     * <p>Objetivo Funcional: Encapsular o {@link ServicoAdminEstoqueReal} com segurança e auditoria.</p>
     * <p>Papel no Design Pattern: Inicializa o <b>Proxy</b> mantendo referência ao <i>RealSubject</i>.</p>
     *
     * @param servicoReal instância de {@link ServicoAdminEstoqueReal}
     * @param estoqueRepository repositório de estoque para leitura do saldo anterior
     * @param auditoriaRepository repositório de auditoria
     * @param usuarioRepository repositório de usuários
     */
    public EstoqueAdminSegurancaProxy(
        @Qualifier("servicoAdminEstoqueReal") ServicoAdminEstoquePort servicoReal,
        ItemEstoqueJpaRepository estoqueRepository,
        RegistroAuditoriaJpaRepository auditoriaRepository,
        UsuarioJpaRepository usuarioRepository
    ) {
        this.servicoReal = servicoReal;
        this.estoqueRepository = estoqueRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Intercepta o ajuste manual de saldo de estoque validando perfil {@code ADMIN}/{@code RESPONSAVEL_ESTOQUE}
     * e gravando registro imutável na trilha de auditoria (FR-009, FR-012).
     *
     * <p>Objetivo Funcional: Proteger ajustes de inventário contra acessos indevidos e auditar o saldo
     * disponível anterior e posterior.</p>
     * <p>Papel no Design Pattern: Operação interceptora do <b>Proxy</b> que delega ao <i>RealSubject</i>
     * e aciona o <b>Static Factory Method</b> {@link RegistroAuditoria#registrarAjusteEstoque}.</p>
     *
     * @param sku código SKU da variação física
     * @param deltaQuantidade variação positiva ou negativa de unidades
     * @param motivo justificativa obrigatória do ajuste
     * @return posição de {@link ItemEstoque} atualizada pelo sujeito real
     */
    @Override
    @Transactional
    public ItemEstoque ajustarSaldoDisponivelManual(String sku, int deltaQuantidade, String motivo) {
        exigirPerfilEstoqueOuAdmin("ajuste manual de saldo de estoque");

        int saldoAnterior = (sku != null && !sku.isBlank())
            ? this.estoqueRepository.findBySkuIgnoreCase(sku.trim())
                .map(ItemEstoque::obterQuantidadeDisponivel)
                .orElse(0)
            : 0;

        ItemEstoque atualizado = this.servicoReal.ajustarSaldoDisponivelManual(sku, deltaQuantidade, motivo);

        String email = ContextoSegurancaUsuario.obterUsernameOuPadrao("estoque@ecommerce.fatec.edu.br");
        PerfilUsuario perfil = ContextoSegurancaUsuario.obterPerfilPrincipalOuPadrao(
            PerfilUsuario.RESPONSAVEL_ESTOQUE
        );
        UUID usuarioId = this.usuarioRepository.findByEmailIgnoreCase(email)
            .map(Usuario::obterId)
            .orElseGet(() -> ContextoSegurancaUsuario.derivarUuidDeterministicoPorEmail(email));

        RegistroAuditoria registro = RegistroAuditoria.registrarAjusteEstoque(
            usuarioId,
            email,
            perfil,
            atualizado.obterSku(),
            saldoAnterior,
            atualizado.obterQuantidadeDisponivel(),
            motivo
        );
        this.auditoriaRepository.save(registro);

        return atualizado;
    }

    /**
     * Intercepta o acionamento manual de expiração de reservas vencidas exigindo perfil autorizado.
     *
     * <p>Objetivo Funcional: Garantir que apenas {@code ADMIN} ou {@code RESPONSAVEL_ESTOQUE} disparem
     * o expurgo administrativo manual.</p>
     * <p>Papel no Design Pattern: Guarda de segurança do <i>Protection Proxy</i>.</p>
     *
     * @return quantidade de reservas vencidas expiradas
     */
    @Override
    @Transactional
    public int acionarExpiracaoDeReservasVencidas() {
        exigirPerfilEstoqueOuAdmin("expiração administrativa de reservas vencidas");
        return this.servicoReal.acionarExpiracaoDeReservasVencidas();
    }

    /**
     * Verifica se o usuário autenticado possui perfil {@code ADMIN} ou {@code RESPONSAVEL_ESTOQUE}.
     *
     * <p>Objetivo Funcional: Bloquear operadores comerciais ou clientes de alterarem saldos físicos de estoque.</p>
     * <p>Papel no Design Pattern: Verificação RBAC do <i>Protection Proxy</i>.</p>
     *
     * @param descricaoOperacao nome da operação protegida
     * @throws AcessoNegadoModulo1Exception se o usuário não tiver permissão
     */
    private void exigirPerfilEstoqueOuAdmin(String descricaoOperacao) {
        if (!ContextoSegurancaUsuario.possuiAlgumPerfil(
            PerfilUsuario.ADMIN,
            PerfilUsuario.RESPONSAVEL_ESTOQUE
        )) {
            throw new AcessoNegadoModulo1Exception(
                "Acesso negado para " + descricaoOperacao
                    + ": requer perfil ADMIN ou RESPONSAVEL_ESTOQUE."
            );
        }
    }
}

