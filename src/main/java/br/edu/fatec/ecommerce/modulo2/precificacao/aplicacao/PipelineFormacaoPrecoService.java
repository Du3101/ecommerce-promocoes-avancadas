package br.edu.fatec.ecommerce.modulo2.precificacao.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo1.integracao.VisaoClienteElegibilidadeDTO;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.infraestrutura.CupomPromocionalJpaRepository;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.DescontoCupomDecorator;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.DescontoPromocaoDecorator;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.MemoriaCalculoPrecoVO;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.PrecoBaseCarrinho;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.PrecoCalculadoComponent;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.RequisicaoValidacaoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorCarrinhoNaoVazio;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorConflitoExclusividade;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorElegibilidadeCampanha;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorPromocaoHandler;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.FabricaDinamicaEstrategiaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura.CampanhaPromocionalJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de aplicação que orquestra o pipeline completo de precificação do carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Integra os padrões centrais do Módulo 2:
 * <ul>
 *   <li><b>Chain of Responsibility:</b> encadeia validadores de integridade, elegibilidade e exclusividade;</li>
 *   <li><b>Comparable:</b> arbitra a ordenação e desempate determinístico das campanhas;</li>
 *   <li><b>Dynamic Factory:</b> instancia e resolve as estratégias de desconto polimórficas;</li>
 *   <li><b>Decorator:</b> empilha as camadas sucessivas de cálculo sobre o subtotal base;</li>
 *   <li><b>Operações Matemáticas:</b> assegura arredondamento bancário e piso zero inegociável.</li>
 * </ul>
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Service
@Transactional(readOnly = true)
public class PipelineFormacaoPrecoService {

    private final CampanhaPromocionalJpaRepository campanhaRepository;
    private final CupomPromocionalJpaRepository cupomRepository;
    private final FabricaDinamicaEstrategiaPromocao fabricaEstrategia;
    private final PortaConsultaCatalogoCliente portaConsultaCatalogo;

    /**
     * Construtor com injeção de dependências.
     *
     * @param campanhaRepository Repositório de campanhas.
     * @param cupomRepository Repositório de cupons.
     * @param fabricaEstrategia Fábrica dinâmica de estratégias promocionais.
     * @param portaConsultaCatalogo Porta de integração com o Módulo 1.
     */
    public PipelineFormacaoPrecoService(
            CampanhaPromocionalJpaRepository campanhaRepository,
            CupomPromocionalJpaRepository cupomRepository,
            FabricaDinamicaEstrategiaPromocao fabricaEstrategia,
            PortaConsultaCatalogoCliente portaConsultaCatalogo
    ) {
        this.campanhaRepository = Objects.requireNonNull(campanhaRepository, "campanhaRepository não pode ser nulo");
        this.cupomRepository = Objects.requireNonNull(cupomRepository, "cupomRepository não pode ser nulo");
        this.fabricaEstrategia = Objects.requireNonNull(fabricaEstrategia, "fabricaEstrategia não pode ser nula");
        this.portaConsultaCatalogo = Objects.requireNonNull(portaConsultaCatalogo, "portaConsultaCatalogo não pode ser nula");
    }

    /**
     * Executa a formação de preço completa de um carrinho, retornando sua memória de cálculo transparente.
     *
     * <p>Objetivo funcional: Determina os descontos legítimos aplicáveis e monta a composição final do valor.
     * <p>Papel no padrão: Orquestrador do pipeline de formação de preço via Decorator.
     *
     * @param carrinho Agregado CarrinhoCompras.
     * @param ufEntregaOverride UF de destino (opcional, sobrescreve endereço do cliente).
     * @return MemoriaCalculoPrecoVO contendo subtotal, descontos discriminados e total líquido.
     */
    public MemoriaCalculoPrecoVO calcularPrecoCarrinho(CarrinhoCompras carrinho, String ufEntregaOverride) {
        if (carrinho == null || carrinho.getItens().isEmpty()) {
            return new MemoriaCalculoPrecoVO(
                    OperacoesMonetarias.ZERO_MONETARIO,
                    OperacoesMonetarias.ZERO_MONETARIO,
                    OperacoesMonetarias.ZERO_MONETARIO,
                    Collections.emptyList()
            );
        }

        // 1. Resolver contexto de cliente para elegibilidade
        boolean primeiraCompra = false;
        String ufEntrega = ufEntregaOverride != null ? ufEntregaOverride : "";

        if (carrinho.getClienteId() != null) {
            VisaoClienteElegibilidadeDTO visaoCliente = portaConsultaCatalogo.consultarClienteParaCompra(carrinho.getClienteId());
            if (visaoCliente != null) {
                primeiraCompra = !visaoCliente.primeiraCompraConcluida();
                if (ufEntrega.isBlank() && visaoCliente.ufDestino() != null) {
                    ufEntrega = visaoCliente.ufDestino();
                }
            }
        }

        ContextoElegibilidade contextoElegibilidade = new ContextoElegibilidade(
                carrinho.getClienteId(),
                primeiraCompra,
                ufEntrega,
                carrinho.calcularSubtotalBruto(),
                carrinho.getItens(),
                portaConsultaCatalogo
        );

        // 2. Buscar campanhas ativas e ordenar deterministicamente via Comparable
        List<CampanhaPromocional> campanhasAtivas = new ArrayList<>(campanhaRepository.listarCampanhasAtivasPorPrioridade());
        Collections.sort(campanhasAtivas); // Ordenação nativa Comparable por prioridade decrescente

        // 3. Montar a Cadeia de Responsabilidade
        ValidadorPromocaoHandler cadeia = new ValidadorCarrinhoNaoVazio();
        ValidadorElegibilidadeCampanha validadorElegibilidade = new ValidadorElegibilidadeCampanha();
        ValidadorConflitoExclusividade validadorExclusividade = new ValidadorConflitoExclusividade();

        cadeia.definirProximo(validadorElegibilidade).definirProximo(validadorExclusividade);

        // 4. Iniciar componente base do Decorator
        PrecoCalculadoComponent componentePreco = new PrecoBaseCarrinho(carrinho.calcularSubtotalBruto());

        // 5. Avaliar e aplicar promoções aprovadas pela cadeia
        List<CampanhaPromocional> promocoesAprovadas = new ArrayList<>();
        Instant agora = Instant.now();

        for (CampanhaPromocional campanha : campanhasAtivas) {
            if (!campanha.isVigenteNoMomento(agora)) {
                continue;
            }

            RequisicaoValidacaoPromocao requisicao = new RequisicaoValidacaoPromocao(
                    carrinho,
                    campanha,
                    contextoElegibilidade,
                    promocoesAprovadas
            );

            if (cadeia.validar(requisicao)) {
                // Instanciar a estratégia dinamicamente via Dynamic Factory
                EstrategiaCalculoPromocao estrategia = fabricaEstrategia.obterEstrategia(campanha.getTipoMecanica());

                ContextoCalculoPromocao contextoCalculo = new ContextoCalculoPromocao(
                        carrinho,
                        campanha,
                        componentePreco.obterTotalLiquido(),
                        portaConsultaCatalogo
                );

                BigDecimal descontoCalculado = estrategia.calcularDesconto(contextoCalculo);

                if (descontoCalculado.compareTo(BigDecimal.ZERO) > 0) {
                    promocoesAprovadas.add(campanha);
                    // Empilha o Decorator de promoção
                    componentePreco = new DescontoPromocaoDecorator(componentePreco, campanha.getNome(), descontoCalculado);
                }
            }
        }

        // 6. Avaliar e empilhar cupom promocional se presente
        if (carrinho.getCupomCodigo() != null && !carrinho.getCupomCodigo().isBlank()) {
            Optional<CupomPromocional> cupomOpt = cupomRepository.findByCodigoIgnoreCase(carrinho.getCupomCodigo().trim());
            if (cupomOpt.isPresent()) {
                CupomPromocional cupom = cupomOpt.get();
                if (cupom.isDisponivelParaUso(agora, carrinho.calcularSubtotalBruto())) {
                    // Se o cupom não for cumulativo com promoções e já houver promoção aprovada, descarta o cupom
                    boolean podeAplicarCupom = cupom.isCumulativoComPromocoes() || promocoesAprovadas.isEmpty();
                    if (podeAplicarCupom) {
                        BigDecimal descontoCupom = cupom.calcularDesconto(componentePreco.obterTotalLiquido());
                        if (descontoCupom.compareTo(BigDecimal.ZERO) > 0) {
                            componentePreco = new DescontoCupomDecorator(componentePreco, cupom.getCodigo(), descontoCupom);
                        }
                    }
                }
            }
        }

        return componentePreco.gerarMemoriaCalculo();
    }
}

