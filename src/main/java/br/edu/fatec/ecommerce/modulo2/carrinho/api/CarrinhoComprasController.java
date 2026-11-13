package br.edu.fatec.ecommerce.modulo2.carrinho.api;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao.ServicoCarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes;
import br.edu.fatec.ecommerce.modulo2.precificacao.aplicacao.PipelineFormacaoPrecoService;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.MemoriaCalculoPrecoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para manipulação do carrinho de compras, mesclagem visitante→cliente
 * e consulta da memória de formação de preço.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Camada de adaptação HTTP REST que expõe os casos de uso do carrinho, coordenando a persistência
 * pelo {@link ServicoCarrinhoCompras} e o cálculo auditável pelo {@link PipelineFormacaoPrecoService}.</p>
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@RestController
@RequestMapping("/api/v1/carrinho")
@Tag(name = "Módulo 2 — Carrinho de Compras", description = "Endpoints para gerenciamento do carrinho, mesclagem e cálculo de promoções")
public class CarrinhoComprasController {

    /** Nome do cabeçalho HTTP dedicado à identificação da sessão anônima de carrinho do portal web. */
    public static final String HEADER_SESSAO_CARRINHO = "X-Sessao-Carrinho";

    private static final String SESSAO_ANONIMA_PADRAO = "sessao-anonima-padrao";

    private final ServicoCarrinhoCompras servicoCarrinho;
    private final PipelineFormacaoPrecoService pipelinePreco;
    private final PortaConsultaCarrinhoPromocoes portaCarrinho;

    /**
     * Construtor com injeção explícita dos serviços de carrinho e precificação.
     *
     * <p>Objetivo Funcional: Conectar endpoints HTTP do carrinho à camada de aplicação e à porta do Módulo 2.</p>
     * <p>Papel no Design Pattern: Injeção de Dependências explícita em Controlador REST.</p>
     *
     * @param servicoCarrinho serviço de ciclo de vida do carrinho
     * @param pipelinePreco pipeline Decorator + Composite de cálculo de descontos
     * @param portaCarrinho porta de consulta ao cliente autenticado
     */
    public CarrinhoComprasController(
            ServicoCarrinhoCompras servicoCarrinho,
            PipelineFormacaoPrecoService pipelinePreco,
            PortaConsultaCarrinhoPromocoes portaCarrinho
    ) {
        this.servicoCarrinho = Objects.requireNonNull(servicoCarrinho, "servicoCarrinho é obrigatório");
        this.pipelinePreco = Objects.requireNonNull(pipelinePreco, "pipelinePreco é obrigatório");
        this.portaCarrinho = Objects.requireNonNull(portaCarrinho, "portaCarrinho é obrigatório");
    }

    /** Payload para inclusão de item no carrinho. */
    public record AdicionarItemRequest(
            @NotBlank(message = "SKU é obrigatório") String sku,
            @Min(value = 1, message = "Quantidade mínima é 1") int quantidade
    ) {}

    /** Payload para atualização de quantidade de SKU no carrinho. */
    public record AtualizarQuantidadeRequest(
            @Min(value = 1, message = "Quantidade mínima é 1") int quantidade
    ) {}

    /** Payload para aplicação de cupom promocional. */
    public record AplicarCupomRequest(
            @NotBlank(message = "Código do cupom é obrigatório") String codigoCupom
    ) {}

    /** Linha resumida retornada na consulta ao carrinho. */
    public record ItemCarrinhoResponse(
            String sku,
            String nome,
            String tipoItem,
            BigDecimal precoUnitario,
            int quantidade,
            BigDecimal subtotal,
            int pesoGramas
    ) {}

    /** Visão completa do carrinho acompanhada da memória auditável de cálculo. */
    public record CarrinhoResponse(
            UUID carrinhoId,
            UUID clienteId,
            String sessaoId,
            int quantidadeTotalItens,
            int pesoTotalGramas,
            List<ItemCarrinhoResponse> itens,
            String cupomCodigo,
            MemoriaCalculoPrecoVO memoriaCalculo
    ) {}

    /**
     * Consulta o carrinho atual do cliente autenticado ou visitante, retornando a memória de cálculo completa.
     *
     * <p>Objetivo Funcional: Sincronizar o estado do drawer lateral do carrinho com promoções e cupons ativos.</p>
     * <p>Papel no Design Pattern: Orquestração REST de leitura e cálculo em {@link PipelineFormacaoPrecoService}.</p>
     *
     * @param sessaoId parâmetro opcional legado de sessão anônima
     * @param ufEntrega UF opcional para avaliação de elegibilidade regional
     * @param request requisição HTTP contendo eventuais headers {@code X-Sessao-Carrinho} ou {@code HttpSession}
     * @return HTTP 200 OK com {@link CarrinhoResponse}
     */
    @GetMapping
    @Operation(summary = "Consulta o carrinho atual com a memória de cálculo de preço e promoções")
    public ResponseEntity<CarrinhoResponse> consultarCarrinho(
            @RequestParam(required = false) String sessaoId,
            @RequestParam(required = false) String ufEntrega,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.obterOuCriarCarrinho(clienteId, sessaoResolvida);
        return ResponseEntity.ok(construirResposta(carrinho, ufEntrega));
    }

    /**
     * Adiciona uma variação ou combo/kit ativo ao carrinho atual.
     *
     * <p>Objetivo Funcional: Incluir produto no carrinho mantendo atualização imediata da memória de descontos.</p>
     * <p>Papel no Design Pattern: Endpoint de comando REST delegando validação para {@link ServicoCarrinhoCompras}.</p>
     *
     * @param requestDto SKU e quantidade positiva
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP para resolução da sessão visitante via header
     * @return HTTP 201 Created com {@link CarrinhoResponse}
     */
    @PostMapping("/itens")
    @Operation(summary = "Adiciona um item (produto ou combo) ao carrinho")
    public ResponseEntity<CarrinhoResponse> adicionarItem(
            @Valid @RequestBody AdicionarItemRequest requestDto,
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.adicionarItem(clienteId, sessaoResolvida, requestDto.sku(), requestDto.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(construirResposta(carrinho, null));
    }

    /**
     * Atualiza a quantidade exata de um SKU presente no carrinho.
     *
     * <p>Objetivo Funcional: Ajustar quantidades na interface recalculando promoções acumuladas.</p>
     * <p>Papel no Design Pattern: Endpoint de mutação REST.</p>
     *
     * @param sku código SKU alvo
     * @param requestDto nova quantidade desejada
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP
     * @return HTTP 200 OK com {@link CarrinhoResponse} atualizado
     */
    @PutMapping("/itens/{sku}")
    @Operation(summary = "Atualiza a quantidade de um SKU no carrinho")
    public ResponseEntity<CarrinhoResponse> atualizarQuantidade(
            @PathVariable String sku,
            @Valid @RequestBody AtualizarQuantidadeRequest requestDto,
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.atualizarQuantidadeItem(clienteId, sessaoResolvida, sku, requestDto.quantidade());
        return ResponseEntity.ok(construirResposta(carrinho, null));
    }

    /**
     * Remove completamente um SKU do carrinho atual.
     *
     * <p>Objetivo Funcional: Excluir item e reavaliar metas de carrinho/cupom restantes.</p>
     * <p>Papel no Design Pattern: Endpoint de exclusão REST.</p>
     *
     * @param sku SKU a remover
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP
     * @return HTTP 200 OK com {@link CarrinhoResponse} atualizado
     */
    @DeleteMapping("/itens/{sku}")
    @Operation(summary = "Remove um SKU do carrinho")
    public ResponseEntity<CarrinhoResponse> removerItem(
            @PathVariable String sku,
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.removerItem(clienteId, sessaoResolvida, sku);
        return ResponseEntity.ok(construirResposta(carrinho, null));
    }

    /**
     * Esvazia todos os itens e cupons do carrinho atual.
     *
     * <p>Objetivo Funcional: Limpar o carrinho do usuário sem destruir a identidade de sessão.</p>
     * <p>Papel no Design Pattern: Endpoint de comando REST idempotente.</p>
     *
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP
     * @return HTTP 204 No Content
     */
    @DeleteMapping
    @Operation(summary = "Esvazia completamente o carrinho")
    public ResponseEntity<Void> esvaziarCarrinho(
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        servicoCarrinho.esvaziarCarrinho(clienteId, sessaoResolvida);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida e aplica um cupom promocional ao carrinho atual.
     *
     * <p>Objetivo Funcional: Acionar o motor de elegibilidade de cupom retornando a memória recalculada.</p>
     * <p>Papel no Design Pattern: Orquestração REST de desconto cumulativo ou exclusivo.</p>
     *
     * @param requestDto código do cupom
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP
     * @return HTTP 200 OK com {@link CarrinhoResponse} atualizado
     */
    @PostMapping("/cupom")
    @Operation(summary = "Aplica um cupom promocional ao carrinho")
    public ResponseEntity<CarrinhoResponse> aplicarCupom(
            @Valid @RequestBody AplicarCupomRequest requestDto,
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.aplicarCupom(clienteId, sessaoResolvida, requestDto.codigoCupom());
        return ResponseEntity.ok(construirResposta(carrinho, null));
    }

    /**
     * Remove o cupom atualmente associado ao carrinho.
     *
     * <p>Objetivo Funcional: Permitir desistência ou troca do cupom no drawer lateral.</p>
     * <p>Papel no Design Pattern: Endpoint de mutação REST.</p>
     *
     * @param sessaoId parâmetro opcional de sessão anônima
     * @param request requisição HTTP
     * @return HTTP 200 OK com {@link CarrinhoResponse} recalculado
     */
    @DeleteMapping("/cupom")
    @Operation(summary = "Remove o cupom atualmente aplicado ao carrinho")
    public ResponseEntity<CarrinhoResponse> removerCupom(
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        String sessaoResolvida = resolverSessao(clienteId, sessaoId, request);
        CarrinhoCompras carrinho = servicoCarrinho.removerCupom(clienteId, sessaoResolvida);
        return ResponseEntity.ok(construirResposta(carrinho, null));
    }

    /**
     * Mescla o carrinho da sessão anônima ({@code X-Sessao-Carrinho}) ao carrinho persistente do cliente logado.
     *
     * <p>Objetivo Funcional: Garantir transição contínua entre navegação anônima e cliente autenticado no login.</p>
     * <p>Papel no Design Pattern: Endpoint de orquestração REST restrito a usuários {@code CLIENTE}.</p>
     *
     * @param sessaoId parâmetro opcional legado de sessão visitante
     * @param request requisição HTTP contendo o header {@code X-Sessao-Carrinho}
     * @return HTTP 200 OK com {@link CarrinhoResponse} consolidado do cliente
     * @throws RegraNegocioModulo1Exception se o usuário autenticado não possuir cadastro de cliente vinculado
     */
    @PostMapping("/mesclar")
    @Operation(summary = "Mescla aditivamente os itens do carrinho visitante ao carrinho do cliente autenticado")
    public ResponseEntity<CarrinhoResponse> mesclarCarrinhoVisitante(
            @RequestParam(required = false) String sessaoId,
            HttpServletRequest request
    ) {
        UUID clienteId = extrairClienteIdAutenticado();
        if (clienteId == null) {
            throw new RegraNegocioModulo1Exception("É necessário estar autenticado como cliente para mesclar carrinhos.");
        }
        String sessaoVisitante = extrairIdentificadorSessaoVisitante(sessaoId, request);
        CarrinhoCompras consolidado = servicoCarrinho.mesclarCarrinhoVisitante(clienteId, sessaoVisitante);
        return ResponseEntity.ok(construirResposta(consolidado, null));
    }

    /**
     * Monta o DTO {@link CarrinhoResponse} executando o pipeline completo de precificação e promoções.
     *
     * <p>Objetivo Funcional: Centralizar a conversão entre agregado {@link CarrinhoCompras} e contrato JSON.</p>
     * <p>Papel no Design Pattern: Mapper de apresentação acoplado ao <b>Decorator + Composite</b> de preço.</p>
     *
     * @param carrinho agregado do carrinho persistido
     * @param ufEntrega UF de entrega opcional
     * @return resposta pronta para serialização JSON
     */
    private CarrinhoResponse construirResposta(CarrinhoCompras carrinho, String ufEntrega) {
        MemoriaCalculoPrecoVO memoria = pipelinePreco.calcularPrecoCarrinho(carrinho, ufEntrega);
        List<ItemCarrinhoResponse> itensResponse = carrinho.getItens().stream()
                .map(item -> new ItemCarrinhoResponse(
                        item.getSku(),
                        item.getNome(),
                        item instanceof ItemCarrinhoComboKit ? "COMBO_KIT" : "PRODUTO",
                        item.getPrecoUnitario(),
                        item.getQuantidade(),
                        item.calcularSubtotalItem(),
                        item.calcularPesoTotalLinhaGramas()
                ))
                .toList();

        return new CarrinhoResponse(
                carrinho.getId(),
                carrinho.getClienteId(),
                carrinho.getSessaoId(),
                carrinho.calcularQuantidadeTotalItens(),
                carrinho.calcularPesoTotalGramas(),
                itensResponse,
                carrinho.getCupomCodigo(),
                memoria
        );
    }

    /**
     * Resolve o dono do carrinho do usuário autenticado pela regra única da porta do Módulo 2
     * ({@code Cliente.id} real; {@code null} para visitantes ou usuários sem perfil de cliente).
     *
     * <p>Objetivo Funcional: Mapear o principal autenticado para o identificador proprietário do carrinho.</p>
     * <p>Papel no Design Pattern: Adaptador de contexto de segurança sobre {@link PortaConsultaCarrinhoPromocoes}.</p>
     *
     * @return ID do cliente dono do carrinho, ou {@code null} para carrinho de sessão
     */
    private UUID extrairClienteIdAutenticado() {
        return ContextoSegurancaUsuario.obterEmailUsuarioAutenticado()
                .map(portaCarrinho::resolverIdentificadorCarrinhoPorEmail)
                .orElse(null);
    }

    /**
     * Resolve o identificador da sessão de carrinho anônimo priorizando o header HTTP {@code X-Sessao-Carrinho}.
     *
     * <p>Objetivo Funcional: Suportar carrinhos de visitantes sem cookies via {@code localStorage} mantendo
     * retrocompatibilidade total com {@code ?sessaoId=} e {@link jakarta.servlet.http.HttpSession}.</p>
     * <p>Papel no Design Pattern: Estratégia ordenada de resolução de identidade anônima na borda HTTP.</p>
     *
     * @param clienteId identificador do cliente quando já autenticado (anula sessão anônima)
     * @param sessaoIdParam parâmetro query opcional legado
     * @param request requisição HTTP corrente
     * @return identificador de sessão anônima, ou {@code null} quando {@code clienteId} estiver presente
     */
    private String resolverSessao(UUID clienteId, String sessaoIdParam, HttpServletRequest request) {
        if (clienteId != null) {
            return null;
        }
        String sessaoExplicita = extrairIdentificadorSessaoVisitante(sessaoIdParam, request);
        if (sessaoExplicita != null) {
            return sessaoExplicita;
        }
        if (request != null && request.getSession(false) != null) {
            return request.getSession(false).getId();
        }
        return SESSAO_ANONIMA_PADRAO;
    }

    /**
     * Extrai explicitamente o token de sessão anônima do header {@code X-Sessao-Carrinho} ou parâmetro {@code sessaoId}.
     *
     * <p>Objetivo Funcional: Permitir localizar o carrinho visitante tanto na navegação anônima quanto
     * imediatamente após a autenticação no endpoint de mesclagem.</p>
     * <p>Papel no Design Pattern: Auxiliar puro reutilizado por {@code resolverSessao} e {@code mesclarCarrinhoVisitante}.</p>
     *
     * @param sessaoIdParam parâmetro query opcional
     * @param request requisição HTTP corrente
     * @return identificador normalizado ou {@code null} se nenhum foi enviado
     */
    private String extrairIdentificadorSessaoVisitante(String sessaoIdParam, HttpServletRequest request) {
        if (request != null) {
            String headerSessao = request.getHeader(HEADER_SESSAO_CARRINHO);
            if (headerSessao != null && !headerSessao.isBlank()) {
                return headerSessao.trim();
            }
        }
        if (sessaoIdParam != null && !sessaoIdParam.isBlank()) {
            return sessaoIdParam.trim();
        }
        return null;
    }
}
