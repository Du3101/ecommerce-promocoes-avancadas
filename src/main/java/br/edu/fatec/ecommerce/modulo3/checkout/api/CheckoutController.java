package br.edu.fatec.ecommerce.modulo3.checkout.api;

import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutRequestDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutResponseDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.aplicacao.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de fechamento de compra do cliente autenticado.
 *
 * <p><b>Papel Arquitetural:</b> camada HTTP fina: só extrai o usuário autenticado e delega à
 * {@link CheckoutFacade} (Facade).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Módulo 3 — Fechamento de compra (Facade + Template Method)")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;

    /**
     * Construtor com a Facade injetada.
     *
     * @param checkoutFacade fachada do checkout
     */
    public CheckoutController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    /**
     * Fecha a compra do carrinho do cliente autenticado.
     *
     * <p>Respostas: 201 (pedido criado; inclui {@code PAGAMENTO_RECUSADO} para cartão recusado), 400 (sem endereço,
     * carrinho vazio, dados inválidos), 409 (estoque insuficiente ou preço alterado).</p>
     *
     * @param usuario usuário autenticado
     * @param requisicao escolhas de pagamento e frete
     * @return pedido criado
     */
    @PostMapping
    @Operation(summary = "Finaliza a compra do carrinho: valida, reserva estoque, cria o pedido e emite a cobrança")
    public ResponseEntity<CheckoutResponseDTO> finalizarCompra(Principal usuario,
                                                               @Valid @RequestBody CheckoutRequestDTO requisicao) {
        CheckoutResponseDTO resposta = checkoutFacade.executarCheckout(usuario.getName(), requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
