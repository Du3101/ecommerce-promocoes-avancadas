package br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo1.integracao.VisaoItemCatalogoDTO;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import br.edu.fatec.ecommerce.modulo2.carrinho.infraestrutura.CarrinhoComprasJpaRepository;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomNulo;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.infraestrutura.CupomPromocionalJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de aplicação para orquestração das mutações e ciclo de vida do carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o serviço de aplicação que coordena o agregado {@link CarrinhoCompras},
 * consumindo a porta de integração desacoplada {@link PortaConsultaCatalogoCliente} do Módulo 1
 * para validar existência, status e dados de catálogo dos itens antes da inclusão no carrinho,
 * e {@link CupomPromocionalJpaRepository} para aplicação de cupons promocionais.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Service
@Transactional
public class ServicoCarrinhoCompras {

    private final CarrinhoComprasJpaRepository carrinhoRepository;
    private final PortaConsultaCatalogoCliente portaConsultaCatalogo;
    private final CupomPromocionalJpaRepository cupomRepository;

    /**
     * Construtor com injeção de dependências do Spring.
     *
     * <p>Objetivo funcional: Inicializa o serviço com os repositórios e a porta de catálogo.
     * <p>Papel no padrão: Injeção de dependência desacoplada da arquitetura em camadas.
     *
     * @param carrinhoRepository Repositório do carrinho.
     * @param portaConsultaCatalogo Porta de integração com o catálogo do Módulo 1.
     * @param cupomRepository Repositório de cupons.
     */
    public ServicoCarrinhoCompras(
            CarrinhoComprasJpaRepository carrinhoRepository,
            PortaConsultaCatalogoCliente portaConsultaCatalogo,
            CupomPromocionalJpaRepository cupomRepository
    ) {
        this.carrinhoRepository = Objects.requireNonNull(carrinhoRepository, "carrinhoRepository não pode ser nulo");
        this.portaConsultaCatalogo = Objects.requireNonNull(portaConsultaCatalogo, "portaConsultaCatalogo não pode ser nula");
        this.cupomRepository = Objects.requireNonNull(cupomRepository, "cupomRepository não pode ser nulo");
    }

    /**
     * Obtém o carrinho ativo do cliente ou da sessão anônima, criando um novo se ainda não existir.
     *
     * <p>Objetivo funcional: Ponto de entrada para consulta e início da sessão de compras.
     * <p>Papel no padrão: Localização de agregado raiz com criação sob demanda.
     *
     * @param clienteId Identificador do cliente autenticado (opcional).
     * @param sessaoId Identificador de sessão de visitante (opcional se clienteId informado).
     * @return Agregado CarrinhoCompras gerenciado.
     */
    public CarrinhoCompras obterOuCriarCarrinho(UUID clienteId, String sessaoId) {
        if (clienteId != null) {
            Optional<CarrinhoCompras> carrinhoExistente = carrinhoRepository.findByClienteId(clienteId);
            if (carrinhoExistente.isPresent()) {
                return carrinhoExistente.get();
            }
        } else if (sessaoId != null && !sessaoId.isBlank()) {
            Optional<CarrinhoCompras> carrinhoExistente = carrinhoRepository.findBySessaoId(sessaoId.trim());
            if (carrinhoExistente.isPresent()) {
                return carrinhoExistente.get();
            }
        }

        CarrinhoCompras novoCarrinho = new CarrinhoCompras(UUID.randomUUID(), clienteId, sessaoId);
        return carrinhoRepository.save(novoCarrinho);
    }

    /**
     * Adiciona um item ao carrinho após validar sua existência e status ativo no catálogo do Módulo 1.
     *
     * <p>Objetivo funcional: Garante que apenas produtos e combos válidos e vendáveis entrem no carrinho.
     * <p>Papel no padrão: Validação de fronteira entre subdomínios via porta desacoplada.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @param sku Código SKU do item a adicionar.
     * @param quantidade Quantidade de unidades (deve ser maior que zero).
     * @return Carrinho atualizado com o item incluído.
     */
    public CarrinhoCompras adicionarItem(UUID clienteId, String sessaoId, String sku, int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioModulo1Exception("A quantidade adicionada ao carrinho deve ser de no mínimo 1 unidade.");
        }
        Objects.requireNonNull(sku, "SKU não pode ser nulo");

        VisaoItemCatalogoDTO visaoItem = portaConsultaCatalogo.consultarItemPorSku(sku.trim());
        if (visaoItem == null) {
            throw new RecursoNaoEncontradoException("Item de catálogo com SKU '" + sku + "' não foi encontrado.");
        }
        if (!visaoItem.ativo()) {
            throw new RegraNegocioModulo1Exception("O item com SKU '" + sku + "' está inativo no catálogo e não pode ser comercializado.");
        }

        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);

        ItemCarrinho itemCarrinho;
        if (visaoItem.eComboKit()) {
            itemCarrinho = new ItemCarrinhoComboKit(
                    UUID.randomUUID(),
                    carrinho.getId(),
                    visaoItem.sku(),
                    visaoItem.nomeExibicao(),
                    visaoItem.precoVigente(),
                    quantidade,
                    visaoItem.pesoTotalGramas(),
                    visaoItem.categoriaPrincipalId()
            );
        } else {
            itemCarrinho = new ItemCarrinhoProduto(
                    UUID.randomUUID(),
                    carrinho.getId(),
                    visaoItem.sku(),
                    visaoItem.nomeExibicao(),
                    visaoItem.precoVigente(),
                    quantidade,
                    visaoItem.pesoTotalGramas(),
                    visaoItem.categoriaPrincipalId()
            );
        }

        carrinho.adicionarOuIncrementarItem(itemCarrinho);
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Atualiza a quantidade desejada de um SKU existente no carrinho.
     *
     * <p>Objetivo funcional: Ajusta a quantidade informada na interface do usuário.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @param sku SKU do item a atualizar.
     * @param novaQuantidade Nova quantidade desejada.
     * @return Carrinho atualizado.
     */
    public CarrinhoCompras atualizarQuantidadeItem(UUID clienteId, String sessaoId, String sku, int novaQuantidade) {
        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);
        carrinho.atualizarQuantidadeItem(sku, novaQuantidade);
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Remove um item do carrinho pelo SKU.
     *
     * <p>Objetivo funcional: Descarte de item selecionado.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @param sku SKU a ser removido.
     * @return Carrinho atualizado após a exclusão.
     */
    public CarrinhoCompras removerItem(UUID clienteId, String sessaoId, String sku) {
        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);
        boolean removido = carrinho.removerItem(sku);
        if (!removido) {
            throw new RecursoNaoEncontradoException("Item com SKU '" + sku + "' não foi encontrado no carrinho.");
        }
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Esvazia completamente o carrinho.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @return Carrinho limpo.
     */
    public CarrinhoCompras esvaziarCarrinho(UUID clienteId, String sessaoId) {
        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);
        carrinho.esvaziar();
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Aplica um cupom promocional ao carrinho após validar vigência e cota de utilização.
     *
     * <p>Objetivo funcional: Vincula o cupom digitado pelo cliente para abatimento no cálculo de preço.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @param codigoCupom Código alfanumérico digitado.
     * @return Carrinho com o cupom aplicado.
     */
    public CarrinhoCompras aplicarCupom(UUID clienteId, String sessaoId, String codigoCupom) {
        Objects.requireNonNull(codigoCupom, "Código do cupom não pode ser nulo");
        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);

        CupomPromocional cupom = cupomRepository.findByCodigoIgnoreCase(codigoCupom.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupom promocional '" + codigoCupom + "' não foi encontrado."));

        if (!cupom.isDisponivelParaUso(Instant.now(), carrinho.calcularSubtotalBruto())) {
            throw new RegraNegocioModulo1Exception("O cupom '" + codigoCupom + "' não está disponível para uso (expirado, esgotado ou subtotal mínimo não atingido).");
        }

        carrinho.aplicarCupom(cupom.getCodigo());
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Remove o cupom atualmente aplicado ao carrinho.
     *
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador de sessão (opcional).
     * @return Carrinho atualizado sem cupom.
     */
    public CarrinhoCompras removerCupom(UUID clienteId, String sessaoId) {
        CarrinhoCompras carrinho = obterOuCriarCarrinho(clienteId, sessaoId);
        carrinho.removerCupom();
        return carrinhoRepository.save(carrinho);
    }

    /**
     * Recupera o cupom ativo vinculado ao carrinho ou retorna o Null Object caso não haja cupom.
     *
     * <p>Objetivo funcional: Obtenção segura da entidade de cupom para precificação.
     * <p>Papel no padrão: Uso do padrão Null Object {@link CupomNulo}.
     *
     * @param carrinho Carrinho de compras.
     * @return CupomPromocional real ou CupomNulo.obterInstancia().
     */
    @Transactional(readOnly = true)
    public CupomPromocional obterCupomAtivo(CarrinhoCompras carrinho) {
        if (carrinho == null || carrinho.getCupomCodigo() == null || carrinho.getCupomCodigo().isBlank()) {
            return CupomNulo.obterInstancia();
        }
        return cupomRepository.findByCodigoIgnoreCase(carrinho.getCupomCodigo())
                .filter(c -> c.isDisponivelParaUso(Instant.now(), carrinho.calcularSubtotalBruto()))
                .orElse(CupomNulo.obterInstancia());
    }
}

