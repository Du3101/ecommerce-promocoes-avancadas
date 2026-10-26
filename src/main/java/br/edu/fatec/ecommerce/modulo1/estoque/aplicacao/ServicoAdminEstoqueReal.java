package br.edu.fatec.ecommerce.modulo1.estoque.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.MovimentacaoEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.MovimentacaoEstoqueJpaRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação real das operações administrativas de Estoque (FR-009, FR-011).
 *
 * <p>Aplicação do Padrão GoF: <b>Proxy</b> — papel <i>RealSubject</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Executa a lógica transacional de ajuste de saldo físico e gravação de
 * {@link MovimentacaoEstoque}, sendo protegido e auditado externamente pelo {@code EstoqueAdminSegurancaProxy}.</p>
 */
@Service("servicoAdminEstoqueReal")
public class ServicoAdminEstoqueReal implements ServicoAdminEstoquePort {

    private final ItemEstoqueJpaRepository estoqueRepository;
    private final MovimentacaoEstoqueJpaRepository movimentacaoRepository;
    private final ServicoReservaEstoqueConcorrente servicoReservaEstoque;

    /**
     * Construtor com injeção explícita dos repositórios e serviço de reservas.
     *
     * <p>Objetivo Funcional: Inicializar o sujeito real de administração de estoque.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>RealSubject</i> do padrão <b>Proxy</b>.</p>
     */
    public ServicoAdminEstoqueReal(
        ItemEstoqueJpaRepository estoqueRepository,
        MovimentacaoEstoqueJpaRepository movimentacaoRepository,
        ServicoReservaEstoqueConcorrente servicoReservaEstoque
    ) {
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.servicoReservaEstoque = servicoReservaEstoque;
    }

    /**
     * Aplica entrada ou ajuste manual de saldo disponível em um SKU com lock pessimista de escrita.
     *
     * <p>Objetivo Funcional: Atualizar o saldo físico disponível e registrar {@link MovimentacaoEstoque} (FR-009).</p>
     * <p>Papel no Design Pattern: Execução do <i>RealSubject</i> invocada pelo {@code EstoqueAdminSegurancaProxy}.</p>
     *
     * @param sku código SKU físico
     * @param deltaQuantidade variação positiva ou negativa de unidades
     * @param motivo justificativa obrigatória
     * @return entidade {@link ItemEstoque} atualizada
     */
    @Override
    @Transactional
    public ItemEstoque ajustarSaldoDisponivelManual(String sku, int deltaQuantidade, String motivo) {
        if (sku == null || sku.isBlank()) {
            throw new RegraNegocioModulo1Exception("O SKU é obrigatório para ajuste de estoque.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioModulo1Exception("O motivo do ajuste manual de estoque é obrigatório.");
        }

        String skuNormalizado = sku.trim().toUpperCase(Locale.ROOT);
        ItemEstoque itemEstoque = this.estoqueRepository.buscarPorSkuComLockPessimista(skuNormalizado)
            .orElseGet(() -> new ItemEstoque(skuNormalizado, 0));

        int saldoAnterior = itemEstoque.obterQuantidadeDisponivel();
        itemEstoque.aplicarAjusteSaldoDisponivel(deltaQuantidade);
        int saldoPosterior = itemEstoque.obterQuantidadeDisponivel();

        ItemEstoque salvo = this.estoqueRepository.save(itemEstoque);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.registrarEntradaOuAjusteManual(
            skuNormalizado,
            deltaQuantidade,
            saldoAnterior,
            saldoPosterior,
            motivo.trim(),
            ContextoSegurancaUsuario.obterUsernameOuPadrao("OPERADOR_ESTOQUE")
        );
        this.movimentacaoRepository.save(movimentacao);

        return salvo;
    }

    /**
     * Executa a varredura e expiração de reservas vencidas até o instante atual.
     *
     * <p>Objetivo Funcional: Devolver saldos retidos em reservas vencidas (FR-011).</p>
     * <p>Papel no Design Pattern: Operação real delegada pelo <b>Proxy</b>.</p>
     *
     * @return quantidade de reservas expiradas
     */
    @Override
    @Transactional
    public int acionarExpiracaoDeReservasVencidas() {
        return this.servicoReservaEstoque.expirarReservasVencidas(Instant.now());
    }
}

