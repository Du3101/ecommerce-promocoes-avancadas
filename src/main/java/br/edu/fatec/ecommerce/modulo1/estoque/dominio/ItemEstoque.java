package br.edu.fatec.ecommerce.modulo1.estoque.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.EstoqueInsuficienteException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;

/**
 * Entidade de domínio responsável pelo controle de saldo disponível e saldo reservado de um SKU físico (FR-009, FR-010, FR-011).
 *
 * <p>Responsabilidade Comercial e Concorrência: Garante que {@code quantidadeDisponivel >= 0} e
 * {@code quantidadeReservada >= 0} em qualquer operação, combinando controle de versão otimista
 * ({@code @Version versaoLock}) com bloqueio pessimista no banco ({@code PESSIMISTIC_WRITE})
 * durante disputas concorrentes pela última unidade em estoque.</p>
 */
@Entity
@Table(name = "itens_estoque")
public class ItemEstoque {

    @Id
    @Column(name = "sku", length = 64, nullable = false, updatable = false)
    private String sku;

    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    @Version
    @Column(name = "versao_lock", nullable = false)
    private long versaoLock;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Preserva a criação controlada da posição de estoque.</p>
     */
    protected ItemEstoque() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor explícito para inicializar o registro de estoque de um SKU físico.
     *
     * <p>Objetivo Funcional: Criar posição inicial de estoque com saldo não negativo.</p>
     * <p>Papel no Design Pattern: Inicialização explícita da entidade de domínio.</p>
     *
     * @param sku código SKU único da variação física
     * @param saldoInicial saldo físico inicial disponível ({@code >= 0})
     */
    public ItemEstoque(String sku, int saldoInicial) {
        if (sku == null || sku.isBlank()) {
            throw new RegraNegocioModulo1Exception("O SKU é obrigatório para inicializar o estoque.");
        }
        if (saldoInicial < 0) {
            throw new RegraNegocioModulo1Exception("O saldo inicial de estoque não pode ser negativo.");
        }
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.quantidadeDisponivel = saldoInicial;
        this.quantidadeReservada = 0;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Reserva atomicamente uma quantidade do saldo disponível, transferindo-a para o saldo reservado (FR-010).
     *
     * <p>Objetivo Funcional: Garantir que unidades em processo de checkout fiquem bloqueadas para outros
     * compradores e impedir que duas compras simultâneas reservem a mesma última unidade.</p>
     * <p>Papel no Design Pattern: Operação de domínio protegida por lock transacional.</p>
     *
     * @param quantidade unidades a reservar ({@code >= 1})
     * @throws EstoqueInsuficienteException se o saldo disponível for menor que a quantidade solicitada
     */
    public void reservarUnidades(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioModulo1Exception("A quantidade a reservar deve ser estritamente maior que zero.");
        }
        if (this.quantidadeDisponivel < quantidade) {
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente para o SKU '" + this.sku + "': disponível = "
                    + this.quantidadeDisponivel + ", solicitado = " + quantidade + "."
            );
        }
        this.quantidadeDisponivel -= quantidade;
        this.quantidadeReservada += quantidade;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Confirma a baixa definitiva de unidades previamente reservadas após aprovação do pagamento (FR-011).
     *
     * <p>Objetivo Funcional: Deduzir o saldo reservado concluindo a saída física da mercadoria.</p>
     * <p>Papel no Design Pattern: Operação de transição de saldo reservado para baixa definitiva.</p>
     *
     * @param quantidade unidades reservadas a baixar definitivamente ({@code >= 1})
     */
    public void confirmarBaixaDeReserva(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioModulo1Exception("A quantidade para baixa definitiva deve ser maior que zero.");
        }
        if (this.quantidadeReservada < quantidade) {
            throw new RegraNegocioModulo1Exception(
                "Saldo reservado insuficiente no SKU '" + this.sku + "' para confirmar baixa."
            );
        }
        this.quantidadeReservada -= quantidade;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Devolve unidades previamente reservadas de volta ao saldo disponível por cancelamento,
     * recusa de pagamento ou expiração de TTL (FR-011).
     *
     * <p>Objetivo Funcional: Reintegrar imediatamente as unidades ao estoque disponível para novos compradores.</p>
     * <p>Papel no Design Pattern: Operação compensatória de liberação de reserva.</p>
     *
     * @param quantidade unidades reservadas a devolver ao saldo disponível ({@code >= 1})
     */
    public void estornarReservaParaDisponivel(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioModulo1Exception("A quantidade a liberar da reserva deve ser maior que zero.");
        }
        if (this.quantidadeReservada < quantidade) {
            throw new RegraNegocioModulo1Exception(
                "Saldo reservado insuficiente no SKU '" + this.sku + "' para liberar reserva."
            );
        }
        this.quantidadeReservada -= quantidade;
        this.quantidadeDisponivel += quantidade;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Aplica um ajuste manual ou entrada de mercadoria sobre o saldo disponível (FR-009).
     *
     * <p>Objetivo Funcional: Permitir reposição de lote (delta positivo) ou baixa por avaria/inventário
     * (delta negativo), impedindo que o saldo disponível resultante fique negativo.</p>
     * <p>Papel no Design Pattern: Operação administrativa protegida e auditada pelo {@code EstoqueAdminSegurancaProxy}.</p>
     *
     * @param deltaQuantidade variação de unidades (positiva para entrada, negativa para saída/ajuste)
     * @throws RegraNegocioModulo1Exception se o delta for zero ou resultar em saldo disponível negativo
     */
    public void aplicarAjusteSaldoDisponivel(int deltaQuantidade) {
        if (deltaQuantidade == 0) {
            throw new RegraNegocioModulo1Exception(
                "A quantidade de ajuste de estoque deve ser diferente de zero."
            );
        }
        int novoSaldo = this.quantidadeDisponivel + deltaQuantidade;
        if (novoSaldo < 0) {
            throw new RegraNegocioModulo1Exception(
                "O ajuste deixaria o saldo disponível do SKU '" + this.sku + "' negativo (" + novoSaldo + ")."
            );
        }
        this.quantidadeDisponivel = novoSaldo;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Obtém o código SKU desta posição de estoque.
     *
     * <p>Objetivo Funcional: Identificar o item de estoque.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return código SKU
     */
    public String obterSku() {
        return this.sku;
    }

    /**
     * Obtém a quantidade atualmente livre para novas vendas/reservas.
     *
     * <p>Objetivo Funcional: Informar disponibilidade imediata ao catálogo e carrinho.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return quantidade disponível ({@code >= 0})
     */
    public int obterQuantidadeDisponivel() {
        return this.quantidadeDisponivel;
    }

    /**
     * Obtém a quantidade atualmente bloqueada em reservas ativas aguardando pagamento.
     *
     * <p>Objetivo Funcional: Rastrear unidades retidas em checkouts em andamento.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return quantidade reservada ({@code >= 0})
     */
    public int obterQuantidadeReservada() {
        return this.quantidadeReservada;
    }

    /**
     * Calcula o saldo físico total no armazém ({@code quantidadeDisponivel + quantidadeReservada}).
     *
     * <p>Objetivo Funcional: Apresentar o total físico ainda não despachado.</p>
     * <p>Papel no Design Pattern: Consulta calculada de domínio.</p>
     *
     * @return soma de unidades disponíveis e reservadas
     */
    public int obterSaldoFisicoTotal() {
        return this.quantidadeDisponivel + this.quantidadeReservada;
    }

    /**
     * Obtém a versão atual do controle de concorrência otimista.
     *
     * <p>Objetivo Funcional: Diagnóstico e rastreabilidade de atualizações concorrentes.</p>
     * <p>Papel no Design Pattern: Getter explícito do campo {@code @Version}.</p>
     *
     * @return versão do lock
     */
    public long obterVersaoLock() {
        return this.versaoLock;
    }

    /**
     * Obtém o instante da última atualização deste item de estoque.
     *
     * <p>Objetivo Funcional: Auditoria temporal do saldo.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC da última modificação
     */
    public Instant obterAtualizadoEm() {
        return this.atualizadoEm;
    }
}

