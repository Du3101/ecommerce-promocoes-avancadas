package br.edu.fatec.ecommerce.modulo2.carrinho.api;

import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao.ServicoCarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.precificacao.aplicacao.PipelineFormacaoPrecoService;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.MemoriaCalculoPrecoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para manipulação do carrinho de compras e consulta da formação de preço.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Camada de adaptação HTTP REST que expõe os casos de uso do carrinho, coordenando a persistência
 * pelo {@link ServicoCarrinhoCompras} e o cálculo auditável pelo {@link PipelineFormacaoPrecoService}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@RestController
@RequestMapping("/api/v1/carrinho")
@Tag(name = "Módulo 2 — Carrinho de Compras", description = "Endpoints para gerenciamento do carrinho e cálculo de promoções")
public class CarrinhoComprasController {

    private final ServicoCarrinhoCompras servicoCarrinho;
    private final PipelineFormacaoPrecoService pipelinePreco;

    public CarrinhoComprasController(ServicoCarrinhoCompras servicoCarrinho, PipelineFormacaoPrecoService pipelinePreco) {
        this.servicoCarrinho = servicoCarrinho;
        this.pipelinePreco = pipelinePreco;
    }

    public record AdicionarItemRequest(
            @NotBlank(message = "SKU é obrigatório") String sku,
            @Min(value = 1, message = "Quantidade mínima é 1") int quantidade
    ) {}

    public record AtualizarQuantidadeRequest(
            @Min(value = 1, message = "Quantidade mínima é 1") int quantidade
    ) {}

    public record AplicarCupomRequest(
            @NotBlank(message = "Código do cupom é obrigatório") String codigoCupom
    ) {}

    public record ItemCarrinhoResponse(
            String sku,
            String nome,
            String tipoItem,
            BigDecimal precoUnitario,
            int quantidade,
            BigDecimal subtotal,
            int pesoGramas
    ) {}

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

    private UUID extrairClienteIdAutenticado() {
        return ContextoSegurancaUsuario.obterEmailUsuarioAutenticado()
                .map(ContextoSegurancaUsuario::derivarUuidDeterministicoPorEmail)
                .orElse(null);
    }

    private String resolverSessao(UUID clienteId, String sessaoIdParam, HttpServletRequest request) {
        if (clienteId != null) {
            return null;
        }
        if (sessaoIdParam != null && !sessaoIdParam.isBlank()) {
            return sessaoIdParam.trim();
        }
        if (request != null && request.getSession(false) != null) {
            return request.getSession(false).getId();
        }
        return "sessao-anonima-padrao";
    }
}
