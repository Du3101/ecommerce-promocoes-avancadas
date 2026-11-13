package br.edu.fatec.ecommerce.modulo3.pedido.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.AcessoNegadoModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.PedidoNaoEncontradoException;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de pedidos com segregação de acesso.
 *
 * <p><b>Papel Arquitetural:</b> serviço de aplicação somente leitura. Clientes veem apenas os próprios pedidos;
 * administradores veem qualquer pedido (Princípio V da Constituição).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoConsultaPedido {

    private final PedidoJpaRepository pedidoRepository;
    private final TransacaoPagamentoJpaRepository transacaoRepository;
    private final PortaConsultaCatalogoCliente portaCliente;

    /**
     * Construtor com dependências injetadas.
     *
     * @param pedidoRepository repositório de pedidos
     * @param transacaoRepository repositório de transações
     * @param portaCliente porta de clientes do Módulo 1
     */
    public ServicoConsultaPedido(PedidoJpaRepository pedidoRepository,
                                 TransacaoPagamentoJpaRepository transacaoRepository,
                                 PortaConsultaCatalogoCliente portaCliente) {
        this.pedidoRepository = pedidoRepository;
        this.transacaoRepository = transacaoRepository;
        this.portaCliente = portaCliente;
    }

    /**
     * Lista os pedidos do cliente autenticado, do mais recente para o mais antigo.
     *
     * @param emailUsuario e-mail do cliente autenticado
     * @return pedidos do cliente
     */
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidosDoCliente(String emailUsuario) {
        UUID clienteId = portaCliente.resolverClienteIdPorEmailUsuario(emailUsuario);
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::paraDto)
                .toList();
    }

    /**
     * Lista todos os pedidos da loja com filtro opcional por status, ordenados do mais recente ao mais antigo.
     *
     * <p>Objetivo Funcional: Fornecer ao painel administrativo e de expedição a fila consolidada de pedidos.</p>
     * <p>Papel no Design Pattern: Consulta de leitura isolada da camada de escrita transacional.</p>
     *
     * @param statusOpcional status para filtragem ou {@code null} para listar todos
     * @return lista ordenada de {@link PedidoResponseDTO}
     */
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodosPedidos(StatusPedido statusOpcional) {
        List<Pedido> pedidos = statusOpcional != null
                ? pedidoRepository.findByStatusOrderByCriadoEmDesc(statusOpcional)
                : pedidoRepository.findAllByOrderByCriadoEmDesc();
        return pedidos.stream()
                .map(this::paraDto)
                .toList();
    }

    /**
     * Carrega a entidade {@link Pedido} validando existência e autorização de acesso pelo dono ou administrador.
     *
     * <p>Objetivo Funcional: Reutilizar a regra de propriedade em operações que exigem leitura segura do pedido.</p>
     * <p>Papel no Design Pattern: Guardião de leitura reutilizado por consultas e controles administrativos.</p>
     *
     * @param pedidoId UUID do pedido
     * @param emailUsuario e-mail do usuário autenticado
     * @param administrador se o usuário possui perfil {@code ADMIN}
     * @return agregado {@link Pedido} autorizado
     * @throws PedidoNaoEncontradoException se não existir pedido com o ID informado
     * @throws AcessoNegadoModulo3Exception se cliente não for o dono do pedido
     */
    @Transactional(readOnly = true)
    public Pedido consultarEntidadeAutorizada(UUID pedidoId, String emailUsuario, boolean administrador) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        if (!administrador && !pedido.pertenceAoCliente(portaCliente.resolverClienteIdPorEmailUsuario(emailUsuario))) {
            throw new AcessoNegadoModulo3Exception("Você só pode consultar os seus próprios pedidos.");
        }
        return pedido;
    }

    /**
     * Consulta o detalhe de um pedido respeitando a propriedade.
     *
     * <p>Objetivo Funcional: Retornar o snapshot imutável completo do pedido para cliente dono ou administrador.</p>
     * <p>Papel no Design Pattern: Fachada de consulta mapeando entidade autorizada em DTO.</p>
     *
     * @param pedidoId UUID do pedido
     * @param emailUsuario e-mail do usuário autenticado
     * @param administrador se o usuário tem perfil administrativo
     * @return detalhe do pedido
     * @throws PedidoNaoEncontradoException se não existir
     * @throws AcessoNegadoModulo3Exception se um cliente tentar ver pedido de outra pessoa
     */
    @Transactional(readOnly = true)
    public PedidoResponseDTO consultarPedido(UUID pedidoId, String emailUsuario, boolean administrador) {
        Pedido pedido = consultarEntidadeAutorizada(pedidoId, emailUsuario, administrador);
        return paraDto(pedido);
    }

    /**
     * Converte o pedido em DTO junto com sua transação financeira.
     *
     * <p>Objetivo Funcional: Montar visão REST unificada com snapshot financeiro e pagamento.</p>
     * <p>Papel no Design Pattern: Mapper apoiado pelo <b>Static Factory Method</b> {@link PedidoResponseDTO#de}.</p>
     *
     * @param pedido pedido
     * @return DTO
     */
    private PedidoResponseDTO paraDto(Pedido pedido) {
        return PedidoResponseDTO.de(pedido,
                transacaoRepository.findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId()).orElse(null));
    }
}
