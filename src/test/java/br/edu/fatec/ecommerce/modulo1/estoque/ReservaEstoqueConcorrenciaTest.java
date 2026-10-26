package br.edu.fatec.ecommerce.modulo1.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.fatec.ecommerce.compartilhado.excecao.TransicaoEstadoReservaInvalidaException;
import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoReservaEstoqueConcorrente;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Suíte de testes automatizados de concorrência, atomicidade, idempotência e expiração por TTL
 * para a User Story 4: Controle Rigoroso de Estoque (FR-009, FR-010, FR-011, SC-002).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservaEstoqueConcorrenciaTest {

    @Autowired
    private ServicoReservaEstoqueConcorrente servicoReservaEstoque;

    @Autowired
    private ItemEstoqueJpaRepository estoqueRepository;

    /**
     * Dispara 50 threads simultâneas disputando exatamente a última unidade disponível de um SKU
     * e comprova que apenas 1 reserva é efetivada e 49 falham, sem jamais gerar saldo negativo (SC-002).
     *
     * <p>Objetivo Funcional: Comprovar ausência de overselling sob concorrência extrema (FR-010).</p>
     * <p>Papel no Design Pattern: Exercita o lock pessimista ordenado conjuntamente com o
     * <b>Static Factory Method</b> de {@link ReservaEstoque} e o <b>Singleton</b> de TTL.</p>
     *
     * @throws InterruptedException caso a espera pelas threads seja interrompida
     */
    @Test
    @DisplayName("Deve permitir apenas 1 reserva entre 50 threads concorrentes disputando a última unidade")
    void devePermitirApenasUmaReservaEntre50ThreadsDisputandoAUltimaUnidade() throws InterruptedException {
        String skuDisputado = "SKU-CONCORRENCIA-ULTIMA-1";
        this.estoqueRepository.saveAndFlush(new ItemEstoque(skuDisputado, 1));

        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch largadaSimultanea = new CountDownLatch(1);
        CountDownLatch conclusaoThreads = new CountDownLatch(totalThreads);

        AtomicInteger reservasBemSucedidas = new AtomicInteger(0);
        AtomicInteger reservasRecusadas = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final int indiceThread = i;
            executor.submit(() -> {
                try {
                    largadaSimultanea.await();
                    this.servicoReservaEstoque.reservarItensAtomicamente(
                        "PEDIDO-CONC-" + indiceThread + "-" + UUID.randomUUID(),
                        UUID.randomUUID(),
                        Map.of(skuDisputado, 1)
                    );
                    reservasBemSucedidas.incrementAndGet();
                } catch (Exception ex) {
                    reservasRecusadas.incrementAndGet();
                } finally {
                    conclusaoThreads.countDown();
                }
            });
        }

        // Libera todas as 50 threads simultaneamente
        largadaSimultanea.countDown();
        conclusaoThreads.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(1, reservasBemSucedidas.get(), "Exatamente 1 thread deve conseguir reservar a última unidade.");
        assertEquals(49, reservasRecusadas.get(), "As outras 49 threads devem receber recusa de estoque insuficiente.");

        ItemEstoque posicaoFinal = this.estoqueRepository.findBySkuIgnoreCase(skuDisputado).orElseThrow();
        assertEquals(0, posicaoFinal.obterQuantidadeDisponivel());
        assertEquals(1, posicaoFinal.obterQuantidadeReservada());
    }

    /**
     * Comprova que chamadas duplicadas de confirmação de baixa ou de liberação de reserva são
     * 100% idempotentes e que transições contraditórias são bloqueadas com exceção.
     *
     * <p>Objetivo Funcional: Validar FR-011 (idempotência e integridade da máquina de estados da reserva).</p>
     * <p>Papel no Design Pattern: Valida as transições protegidas de {@link ReservaEstoque}.</p>
     */
    @Test
    @DisplayName("Deve garantir idempotência em chamadas repetidas de confirmação/liberação e bloquear transição inválida")
    void deveGarantirIdempotenciaEmConfirmacaoELiberacaoDeReserva() {
        String skuTeste = "SKU-IDEMPOTENCIA-TEST";
        this.estoqueRepository.saveAndFlush(new ItemEstoque(skuTeste, 10));

        // 1. Reserva 3 unidades -> disponível = 7, reservado = 3
        ReservaEstoque reservaConfirmar = this.servicoReservaEstoque.reservarItensAtomicamente(
            "CHAVE-CONFIRMAR-IDEMP-01",
            UUID.randomUUID(),
            Map.of(skuTeste, 3)
        );
        assertEquals(EstadoReservaEstoque.ATIVA, reservaConfirmar.obterEstado());

        // 2. Confirma 2 vezes seguidas -> só deve baixar 1 vez
        ReservaEstoque confirmada1 = this.servicoReservaEstoque.confirmarBaixaDefinitivaReserva(
            "CHAVE-CONFIRMAR-IDEMP-01"
        );
        ReservaEstoque confirmada2 = this.servicoReservaEstoque.confirmarBaixaDefinitivaReserva(
            "CHAVE-CONFIRMAR-IDEMP-01"
        );
        assertEquals(EstadoReservaEstoque.CONFIRMADA, confirmada1.obterEstado());
        assertEquals(EstadoReservaEstoque.CONFIRMADA, confirmada2.obterEstado());

        ItemEstoque aposConfirmacao = this.estoqueRepository.findBySkuIgnoreCase(skuTeste).orElseThrow();
        assertEquals(7, aposConfirmacao.obterQuantidadeDisponivel());
        assertEquals(0, aposConfirmacao.obterQuantidadeReservada());

        // Tentar liberar uma reserva já CONFIRMADA deve lançar TransicaoEstadoReservaInvalidaException
        assertThrows(TransicaoEstadoReservaInvalidaException.class, () ->
            this.servicoReservaEstoque.liberarReservaPorFalhaOuCancelamento(
                "CHAVE-CONFIRMAR-IDEMP-01",
                "Tentativa inválida"
            )
        );

        // 3. Reserva mais 2 unidades -> disponível = 5, reservado = 2
        this.servicoReservaEstoque.reservarItensAtomicamente(
            "CHAVE-LIBERAR-IDEMP-02",
            UUID.randomUUID(),
            Map.of(skuTeste, 2)
        );

        // Libera 2 vezes seguidas -> só deve devolver ao disponível 1 vez (voltando para 7)
        ReservaEstoque liberada1 = this.servicoReservaEstoque.liberarReservaPorFalhaOuCancelamento(
            "CHAVE-LIBERAR-IDEMP-02",
            "Pagamento recusado"
        );
        ReservaEstoque liberada2 = this.servicoReservaEstoque.liberarReservaPorFalhaOuCancelamento(
            "CHAVE-LIBERAR-IDEMP-02",
            "Repetição de webhook de cancelamento"
        );
        assertEquals(EstadoReservaEstoque.LIBERADA, liberada1.obterEstado());
        assertEquals(EstadoReservaEstoque.LIBERADA, liberada2.obterEstado());

        ItemEstoque aposLiberacao = this.estoqueRepository.findBySkuIgnoreCase(skuTeste).orElseThrow();
        assertEquals(7, aposLiberacao.obterQuantidadeDisponivel());
        assertEquals(0, aposLiberacao.obterQuantidadeReservada());
    }

    /**
     * Comprova que reservas vencidas por TTL são expiradas e têm suas unidades devolvidas ao saldo disponível.
     *
     * <p>Objetivo Funcional: Validar FR-011 (expurgo por TTL de reserva).</p>
     * <p>Papel no Design Pattern: Valida integração com o TTL configurado no <b>Singleton</b>.</p>
     */
    @Test
    @DisplayName("Deve expirar reservas ativas vencidas por TTL e devolver saldo ao estoque disponível")
    void deveExpirarReservasVencidasPorTtlEDevolverSaldoAoDisponivel() {
        String skuTtl = "SKU-TTL-EXPIRACAO";
        this.estoqueRepository.saveAndFlush(new ItemEstoque(skuTtl, 5));

        this.servicoReservaEstoque.reservarItensAtomicamente(
            "CHAVE-RESERVA-TTL-01",
            UUID.randomUUID(),
            Map.of(skuTtl, 4)
        );

        ItemEstoque duranteReserva = this.estoqueRepository.findBySkuIgnoreCase(skuTtl).orElseThrow();
        assertEquals(1, duranteReserva.obterQuantidadeDisponivel());
        assertEquals(4, duranteReserva.obterQuantidadeReservada());

        // Avança o corte temporal além dos 30 minutos de TTL padrão
        int expiradas = this.servicoReservaEstoque.expirarReservasVencidas(
            Instant.now().plusSeconds(3600)
        );
        assertEquals(1, expiradas);

        ItemEstoque aposExpiracao = this.estoqueRepository.findBySkuIgnoreCase(skuTtl).orElseThrow();
        assertEquals(5, aposExpiracao.obterQuantidadeDisponivel());
        assertEquals(0, aposExpiracao.obterQuantidadeReservada());
    }
}

