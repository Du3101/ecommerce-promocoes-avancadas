package br.edu.fatec.ecommerce.modulo3.frete.api;

import br.edu.fatec.ecommerce.modulo3.frete.api.dto.CalculoFreteRequestDTO;
import br.edu.fatec.ecommerce.modulo3.frete.api.dto.CalculoFreteResponseDTO;
import br.edu.fatec.ecommerce.modulo3.frete.aplicacao.ServicoCalculoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de simulação de frete.
 *
 * <p><b>Papel Arquitetural:</b> camada de interface HTTP; delega ao {@link ServicoCalculoFrete} e converte o
 * resultado em DTO.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/frete")
@Tag(name = "Frete", description = "Módulo 3 — Cotação de frete (Adapter/ACL) e política de frete grátis")
public class FreteController {

    private final ServicoCalculoFrete servicoCalculoFrete;

    /**
     * Construtor com injeção do serviço de frete.
     *
     * @param servicoCalculoFrete serviço de cotação
     */
    public FreteController(ServicoCalculoFrete servicoCalculoFrete) {
        this.servicoCalculoFrete = servicoCalculoFrete;
    }

    /**
     * Cota todas as modalidades de frete para um CEP, peso e subtotal.
     *
     * @param requisicao dados da simulação
     * @return opções de frete ordenadas pelo menor valor
     */
    @PostMapping("/calcular")
    @Operation(summary = "Calcula as opções de frete (PAC, SEDEX, Transportadora) com regra de frete grátis")
    public CalculoFreteResponseDTO calcular(@Valid @RequestBody CalculoFreteRequestDTO requisicao) {
        var opcoes = servicoCalculoFrete
                .cotarTodasModalidades(requisicao.cepDestino(), requisicao.pesoTotalGramas(), requisicao.subtotalLiquido())
                .stream()
                .map(CalculoFreteResponseDTO.OpcaoFreteDTO::de)
                .toList();
        return new CalculoFreteResponseDTO(
                RegiaoEntrega.formatarCep(requisicao.cepDestino()),
                RegiaoEntrega.deCep(requisicao.cepDestino()).getDescricao(),
                requisicao.pesoTotalGramas(),
                servicoCalculoFrete.freteGratisDisponivel(requisicao.subtotalLiquido(), requisicao.pesoTotalGramas()),
                opcoes);
    }
}
