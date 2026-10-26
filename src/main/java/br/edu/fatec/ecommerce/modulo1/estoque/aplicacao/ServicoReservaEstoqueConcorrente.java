package br.edu.fatec.ecommerce.modulo1.estoque.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.MovimentacaoEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.MovimentacaoEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ReservaEstoqueJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação responsável pelo controle transacional de reservas de estoque sob alta
 * concorrência, confirmação idempotente de baixa, liberação compensatória e expurgo por TTL (FR-010, FR-011).
 *
 * <p>Orquestra os padrões <b>Composite</b> (expansão recursiva de SKUs físicos de combos),
 * <b>Static Factory Method</b> ({@link ReservaEstoque} e {@link MovimentacaoEstoque}) e
 * <b>Singleton</b> ({@code ConfiguracaoModuloCatalogoEstoqueSingleton} para TTL).</p>
 */
@Service
public class ServicoReservaEstoqueConcorrente {

    private final ItemEstoqueJpaRepository estoqueRepository;
    private final ReservaEstoqueJpaRepository reservaRepository;
    private final MovimentacaoEstoqueJpaRepository movimentacaoRepository;
    private final ServicoConsultaCatalogo servicoConsultaCatalogo;

    /**
     * Construtor com injeção explícita dos repositórios de estoque e serviço de catálogo.
     *
     * <p>Objetivo Funcional: Inicializar o motor transacional de reservas.</p>
     * <p>Papel no Design Pattern: Coordenação de caso de uso utilizando locks pessimistas e padrões de criação.</p>
     *
     * @param estoqueRepository repositório JPA de itens de estoque
     * @param reservaRepository repositório JPA de reservas
     * @param movimentacaoRepository repositório JPA de movimentações
     * @param servicoConsultaCatalogo serviço de consulta polimórfica do catálogo
     */
    public ServicoReservaEstoqueConcorrente(
        ItemEstoqueJpaRepository estoqueRepository,
        ReservaEstoqueJpaRepository reservaRepository,
        MovimentacaoEstoqueJpaRepository movimentacaoRepository,
        ServicoConsultaCatalogo servicoConsultaCatalogo
    ) {
        this.estoqueRepository = estoqueRepository;
        this.reservaRepository = reservaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.servicoConsultaCatalogo = servicoConsultaCatalogo;
    }

    /**
     * Reserva atomicamente todos os SKUs solicitados (expandindo Combos/Kits em seus SKUs físicos folha)
     * com bloqueio pessimista ordenado alfabeticamente para impedir overselling e deadlocks (FR-010).
     *
     * <p>Objetivo Funcional: Garantir semântica tudo-ou-nada: se faltar saldo em qualquer SKU ou componente
     * de combo, toda a transação sofre rollback sem reter parcialmente outros itens.</p>
     * <p>Papel no Design Pattern: Combina a decomposição recursiva do <b>Composite</b>
     * ({@link ItemCatalogoComponent#decomporSkusFisicosParaReserva}) com os <b>Static Factory Methods</b>
     * de {@link ReservaEstoque} e {@link MovimentacaoEstoque}.</p>
     *
     * @param chaveOperacaoPedido chave única de idempotência do checkout/pedido
     * @param clienteId UUID do cliente comprador
     * @param quantidadesPorSkuSolicitado mapa de SKUs do catálogo (folhas ou combos) e quantidades desejadas
     * @return instância persistida de {@link ReservaEstoque} no estado {@code ATIVA}
     */
    @Transactional
    public ReservaEstoque reservarItensAtomicamente(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadesPorSkuSolicitado
    ) {
        if (chaveOperacaoPedido == null || chaveOperacaoPedido.isBlank()) {
            throw new RegraNegocioModulo1Exception("A chave da operação de pedido é obrigatória.");
        }

        Optional<ReservaEstoque> reservaExistente =
            this.reservaRepository.findByChaveOperacaoPedido(chaveOperacaoPedido.trim());
        if (reservaExistente.isPresent()) {
            return reservaExistente.get();
        }

        TreeMap<String, Integer> skusFisicosOrdenados = expandirEOrdenarSkusFisicos(quantidadesPorSkuSolicitado);

        List<ItemEstoque> itensBloqueados =
            this.estoqueRepository.buscarMultiplosPorSkusComLockOrdenado(skusFisicosOrdenados.keySet());

        Map<String, ItemEstoque> mapaPorSku = itensBloqueados.stream()
            .collect(Collectors.toMap(ItemEstoque::obterSku, Function.identity()));

        for (Map.Entry<String, Integer> entrada : skusFisicosOrdenados.entrySet()) {
            String skuFolha = entrada.getKey();
            int qtdNecessaria = entrada.getValue();

            ItemEstoque posicao = mapaPorSku.get(skuFolha);
            if (posicao == null) {
                throw new RecursoNaoEncontradoException(
                    "Posição de estoque não encontrada para o SKU físico: " + skuFolha
                );
            }

            int saldoAnterior = posicao.obterQuantidadeDisponivel();
            posicao.reservarUnidades(qtdNecessaria);
            int saldoPosterior = posicao.obterQuantidadeDisponivel();

            this.estoqueRepository.save(posicao);
            this.movimentacaoRepository.save(
                MovimentacaoEstoque.registrarRetencaoPorReserva(
                    skuFolha,
                    qtdNecessaria,
                    saldoAnterior,
                    saldoPosterior,
                    chaveOperacaoPedido.trim(),
                    clienteId != null ? clienteId.toString() : "CHECKOUT"
                )
            );
        }

        ReservaEstoque novaReserva = ReservaEstoque.criarReservaAtivaComExpiracao(
            chaveOperacaoPedido.trim(),
            clienteId,
            skusFisicosOrdenados
        );
        return this.reservaRepository.save(novaReserva);
    }

    /**
     * Confirma de maneira idempotente a baixa definitiva de uma reserva ativa após aprovação do pagamento (FR-011).
     *
     * <p>Objetivo Funcional: Converter o saldo reservado em saída definitiva. Chamadas repetidas para
     * uma reserva já {@code CONFIRMADA} retornam sucesso sem deduzir novamente o estoque.</p>
     * <p>Papel no Design Pattern: Aciona a transição idempotente {@link ReservaEstoque#confirmarBaixaDefinitiva()}
     * e o <b>Static Factory Method</b> {@link MovimentacaoEstoque#registrarBaixaDefinitivaReserva}.</p>
     *
     * @param chaveOperacaoPedido chave única da reserva
     * @return reserva no estado {@code CONFIRMADA}
     */
    @Transactional
    public ReservaEstoque confirmarBaixaDefinitivaReserva(String chaveOperacaoPedido) {
        ReservaEstoque reserva = this.reservaRepository.buscarPorChaveOperacaoComLock(chaveOperacaoPedido)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Reserva de estoque não encontrada para a chave: " + chaveOperacaoPedido
            ));

        boolean transicionouAgora = reserva.confirmarBaixaDefinitiva();
        if (!transicionouAgora) {
            return reserva; // Idempotência: já estava CONFIRMADA
        }

        List<String> skusDaReserva = reserva.obterItens().stream()
            .map(ItemReservaEstoque::obterSku)
            .sorted()
            .collect(Collectors.toList());

        Map<String, ItemEstoque> estoquesBloqueados =
            this.estoqueRepository.buscarMultiplosPorSkusComLockOrdenado(skusDaReserva).stream()
                .collect(Collectors.toMap(ItemEstoque::obterSku, Function.identity()));

        for (ItemReservaEstoque itemReserva : reserva.obterItens()) {
            ItemEstoque posicao = estoquesBloqueados.get(itemReserva.obterSku());
            if (posicao != null) {
                posicao.confirmarBaixaDeReserva(itemReserva.obterQuantidadeReservada());
                this.estoqueRepository.save(posicao);
                this.movimentacaoRepository.save(
                    MovimentacaoEstoque.registrarBaixaDefinitivaReserva(
                        posicao.obterSku(),
                        itemReserva.obterQuantidadeReservada(),
                        posicao.obterQuantidadeDisponivel(),
                        reserva.obterChaveOperacaoPedido(),
                        "CHECKOUT_PAGAMENTO_APROVADO"
                    )
                );
            }
        }

        return this.reservaRepository.save(reserva);
    }

    /**
     * Libera de maneira idempotente uma reserva ativa devolvendo seu saldo ao estoque disponível (FR-011).
     *
     * <p>Objetivo Funcional: Reintegrar unidades retidas quando o pagamento é recusado ou o pedido é
     * cancelado. Chamadas repetidas para reservas já {@code LIBERADA} ou {@code EXPIRADA} são idempotentes.</p>
     * <p>Papel no Design Pattern: Aciona {@link ReservaEstoque#liberarReserva()} e o
     * <b>Static Factory Method</b> {@link MovimentacaoEstoque#registrarEstornoDeReserva}.</p>
     *
     * @param chaveOperacaoPedido chave única da reserva
     * @param motivo justificativa do cancelamento ou recusa
     * @return reserva no estado {@code LIBERADA} (ou {@code EXPIRADA})
     */
    @Transactional
    public ReservaEstoque liberarReservaPorFalhaOuCancelamento(String chaveOperacaoPedido, String motivo) {
        ReservaEstoque reserva = this.reservaRepository.buscarPorChaveOperacaoComLock(chaveOperacaoPedido)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Reserva de estoque não encontrada para a chave: " + chaveOperacaoPedido
            ));

        boolean transicionouAgora = reserva.liberarReserva();
        if (!transicionouAgora) {
            return reserva; // Idempotência: já estava LIBERADA ou EXPIRADA
        }

        devolverItensDaReservaAoDisponivel(
            reserva,
            motivo != null && !motivo.isBlank() ? motivo : "Liberação por cancelamento/falha de pagamento"
        );
        return this.reservaRepository.save(reserva);
    }

    /**
     * Varre todas as reservas {@code ATIVA} cujo TTL já venceu até o instante informado, marcando-as
     * como {@code EXPIRADA} e devolvendo integralmente seus saldos ao estoque disponível (FR-011).
     *
     * <p>Objetivo Funcional: Evitar retenção infinita de estoque por carrinhos abandonados.</p>
     * <p>Papel no Design Pattern: Rotina de expurgo baseada no TTL definido pelo <b>Singleton</b>.</p>
     *
     * @param instanteReferencia instante de corte (ex.: {@code Instant.now()})
     * @return quantidade de reservas expiradas na execução
     */
    @Transactional
    public int expirarReservasVencidas(Instant instanteReferencia) {
        Instant corte = instanteReferencia != null ? instanteReferencia : Instant.now();
        List<ReservaEstoque> vencidas = this.reservaRepository.buscarReservasVencidasComLock(
            EstadoReservaEstoque.ATIVA,
            corte
        );

        int totalExpiradas = 0;
        for (ReservaEstoque reserva : vencidas) {
            if (reserva.expirarPorTtl()) {
                devolverItensDaReservaAoDisponivel(
                    reserva,
                    "Expiração automática por vencimento de TTL: " + reserva.obterChaveOperacaoPedido()
                );
                this.reservaRepository.save(reserva);
                totalExpiradas++;
            }
        }
        return totalExpiradas;
    }

    /**
     * Consulta a posição de estoque de um SKU físico ou calcula a disponibilidade virtual de um Combo.
     *
     * <p>Objetivo Funcional: Responder consultas de estoque tanto para SKUs físicos quanto para Combos.</p>
     * <p>Papel no Design Pattern: Consulta integrada entre Estoque e Catálogo (<b>Composite</b>).</p>
     *
     * @param sku código SKU da variação ou combo
     * @return entidade {@link ItemEstoque} (real ou sintética para combo)
     */
    @Transactional(readOnly = true)
    public ItemEstoque consultarPosicaoEstoqueOuCombo(String sku) {
        Optional<ItemEstoque> posicaoFisica = this.estoqueRepository.findBySkuIgnoreCase(sku);
        if (posicaoFisica.isPresent()) {
            return posicaoFisica.get();
        }

        ItemCatalogoComponent itemCatalogo = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        int dispCombo = this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(itemCatalogo);
        return new ItemEstoque(itemCatalogo.obterSku(), dispCombo);
    }

    /**
     * Expande SKUs do catálogo (incluindo combos aninhados) em SKUs físicos folha ordenados alfabeticamente.
     *
     * <p>Objetivo Funcional: Consolidar quantidades de SKUs físicos e ordenar de A a Z antes de adquirir locks.</p>
     * <p>Papel no Design Pattern: Decomposição polimórfica via {@link ItemCatalogoComponent} (<b>Composite</b>).</p>
     *
     * @param quantidadesPorSkuSolicitado mapa de SKUs e quantidades
     * @return {@link TreeMap} ordenado alfabeticamente com os SKUs físicos e quantidades totais
     */
    private TreeMap<String, Integer> expandirEOrdenarSkusFisicos(
        Map<String, Integer> quantidadesPorSkuSolicitado
    ) {
        if (quantidadesPorSkuSolicitado == null || quantidadesPorSkuSolicitado.isEmpty()) {
            throw new RegraNegocioModulo1Exception("Informe ao menos um item para reserva de estoque.");
        }

        TreeMap<String, Integer> acumuladorOrdenado = new TreeMap<>();
        for (Map.Entry<String, Integer> entrada : quantidadesPorSkuSolicitado.entrySet()) {
            String sku = entrada.getKey();
            Integer qtd = entrada.getValue();
            if (sku == null || sku.isBlank() || qtd == null || qtd <= 0) {
                throw new RegraNegocioModulo1Exception(
                    "SKU válido e quantidade maior que zero são obrigatórios para reserva."
                );
            }

            Optional<ItemCatalogoComponent> itemCatalogoOpt =
                this.servicoConsultaCatalogo.buscarItemComercializavelPorSkuOpcional(sku.trim());
            if (itemCatalogoOpt.isPresent()) {
                ItemCatalogoComponent itemCatalogo = itemCatalogoOpt.get();
                if (!itemCatalogo.estaAtivo()) {
                    throw new RegraNegocioModulo1Exception(
                        "O item '" + itemCatalogo.obterSku() + "' está inativo e não pode ser reservado."
                    );
                }
                Map<String, Integer> folhasDecompostas = itemCatalogo.decomporSkusFisicosParaReserva(qtd);
                for (Map.Entry<String, Integer> folha : folhasDecompostas.entrySet()) {
                    acumuladorOrdenado.merge(folha.getKey(), folha.getValue(), Integer::sum);
                }
            } else {
                // Caso o SKU tenha sido criado diretamente na tabela itens_estoque em teste isolado de estoque
                String skuNormalizado = sku.trim().toUpperCase(Locale.ROOT);
                if (this.estoqueRepository.findBySkuIgnoreCase(skuNormalizado).isPresent()) {
                    acumuladorOrdenado.merge(skuNormalizado, qtd, Integer::sum);
                } else {
                    throw new RecursoNaoEncontradoException(
                        "Item não encontrado no catálogo nem no estoque para o SKU: " + sku
                    );
                }
            }
        }
        return acumuladorOrdenado;
    }

    /**
     * Devolve as quantidades reservadas de uma reserva de volta ao saldo disponível de cada SKU.
     *
     * <p>Objetivo Funcional: Executar o estorno físico com lock ordenado e gravar movimentações.</p>
     * <p>Papel no Design Pattern: Método auxiliar transacional de compensação.</p>
     *
     * @param reserva reserva sendo liberada ou expirada
     * @param motivo motivo do estorno
     */
    private void devolverItensDaReservaAoDisponivel(ReservaEstoque reserva, String motivo) {
        List<String> skusOrdenados = reserva.obterItens().stream()
            .map(ItemReservaEstoque::obterSku)
            .sorted()
            .collect(Collectors.toList());

        Map<String, ItemEstoque> estoquesBloqueados =
            this.estoqueRepository.buscarMultiplosPorSkusComLockOrdenado(skusOrdenados).stream()
                .collect(Collectors.toMap(ItemEstoque::obterSku, Function.identity()));

        for (ItemReservaEstoque itemReserva : reserva.obterItens()) {
            ItemEstoque posicao = estoquesBloqueados.get(itemReserva.obterSku());
            if (posicao != null) {
                int saldoAnterior = posicao.obterQuantidadeDisponivel();
                posicao.estornarReservaParaDisponivel(itemReserva.obterQuantidadeReservada());
                int saldoPosterior = posicao.obterQuantidadeDisponivel();

                this.estoqueRepository.save(posicao);
                this.movimentacaoRepository.save(
                    MovimentacaoEstoque.registrarEstornoDeReserva(
                        posicao.obterSku(),
                        itemReserva.obterQuantidadeReservada(),
                        saldoAnterior,
                        saldoPosterior,
                        motivo,
                        "SISTEMA_RESERVA"
                    )
                );
            }
        }
    }
}

