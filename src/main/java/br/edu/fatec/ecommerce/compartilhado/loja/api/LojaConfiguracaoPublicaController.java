package br.edu.fatec.ecommerce.compartilhado.loja.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST público responsável por expor configurações não sensíveis da loja virtual.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Adaptador HTTP somente leitura que parametriza comportamentos condicionais do portal web
 * (como exibição discreta de cartões de acesso rápido e simulação interativa de pagamento em
 * demonstrações acadêmicas) mantendo tokens internos isolados no servidor.</p>
 */
@RestController
@RequestMapping("/api/v1/loja")
@Tag(name = "Configuração Pública da Loja", description = "Metadados públicos para parametrização da vitrine")
public class LojaConfiguracaoPublicaController {

    private final boolean modoDemonstracaoAtivo;
    private final BigDecimal freteGratisValorMinimo;
    private final int freteGratisPesoMaximoGramas;

    /**
     * Construtor explícito com injeção das propriedades públicas de ambiente via {@code @Value}.
     *
     * <p>Objetivo Funcional: Vincular o estado de modo demonstração e limites de frete grátis ao perfil ativo.</p>
     * <p>Papel no Design Pattern: Injeção de configuração imutável no controlador REST público.</p>
     *
     * @param modoDemonstracaoAtivo flag {@code ecommerce.modulo3.seed-demo-ativo} (falsa em teste/produção)
     * @param freteGratisValorMinimo valor líquido mínimo configurado em {@code ecommerce.modulo3.frete-gratis.valor-minimo}
     * @param freteGratisPesoMaximoGramas peso máximo em gramas em {@code ecommerce.modulo3.frete-gratis.peso-maximo-gramas}
     */
    public LojaConfiguracaoPublicaController(
        @Value("${ecommerce.modulo3.seed-demo-ativo:false}") boolean modoDemonstracaoAtivo,
        @Value("${ecommerce.modulo3.frete-gratis.valor-minimo:250.00}") BigDecimal freteGratisValorMinimo,
        @Value("${ecommerce.modulo3.frete-gratis.peso-maximo-gramas:5000}") int freteGratisPesoMaximoGramas
    ) {
        this.modoDemonstracaoAtivo = modoDemonstracaoAtivo;
        this.freteGratisValorMinimo = freteGratisValorMinimo;
        this.freteGratisPesoMaximoGramas = freteGratisPesoMaximoGramas;
    }

    /**
     * Retorna a configuração pública vigente da loja virtual para a interface web.
     *
     * <p>Objetivo Funcional: Informar ao portal se o modo demonstração está habilitado e quais são
     * as regras exibíveis de frete grátis sem expor segredos operacionais.</p>
     * <p>Papel no Design Pattern: Endpoint de leitura pública delegando ao <b>Static Factory Method</b>
     * de {@link ConfiguracaoLojaPublicaDTO}.</p>
     *
     * @return resposta HTTP 200 OK contendo {@link ConfiguracaoLojaPublicaDTO}
     */
    @GetMapping("/configuracao")
    @Operation(summary = "Consulta parâmetros públicos da loja e sinalizador de modo demonstração")
    public ResponseEntity<ConfiguracaoLojaPublicaDTO> consultarConfiguracaoPublica() {
        ConfiguracaoLojaPublicaDTO dto = ConfiguracaoLojaPublicaDTO.criar(
            this.modoDemonstracaoAtivo,
            this.freteGratisValorMinimo,
            this.freteGratisPesoMaximoGramas
        );
        return ResponseEntity.ok(dto);
    }
}
