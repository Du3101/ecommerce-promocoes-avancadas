package br.edu.fatec.ecommerce.modulo2.integracao;

import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.TipoDescontoCupom;
import br.edu.fatec.ecommerce.modulo2.cupons.infraestrutura.CupomPromocionalJpaRepository;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoPrimeiraCompraCliente;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoValorMinimoCarrinho;
import br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura.CampanhaPromocionalJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Carregador de demonstração do Módulo 2 executado na inicialização da aplicação.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Inicializa de forma idempotente as 4 modalidades distintas de promoções exigidas pela Constituição
 * e os cupons promocionais de teste para validação imediata no Swagger UI e na defesa acadêmica.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class InicializadorDemonstracaoModulo2Runner implements CommandLineRunner {

    private final CampanhaPromocionalJpaRepository campanhaRepository;
    private final CupomPromocionalJpaRepository cupomRepository;

    public InicializadorDemonstracaoModulo2Runner(
            CampanhaPromocionalJpaRepository campanhaRepository,
            CupomPromocionalJpaRepository cupomRepository
    ) {
        this.campanhaRepository = campanhaRepository;
        this.cupomRepository = cupomRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        inicializarCampanhasDemonstracao();
        inicializarCuponsDemonstracao();
    }

    private void inicializarCampanhasDemonstracao() {
        if (campanhaRepository.count() > 0) {
            return;
        }

        Instant agora = Instant.now();
        Instant futuro = agora.plus(365, ChronoUnit.DAYS);

        // 1. Leve 3 Pague 2
        CampanhaPromocional c1 = new CampanhaPromocional(
                UUID.fromString("22222222-0000-0000-0000-000000000001"),
                "Leve 3 e Pague 2 em Periféricos",
                "Na compra de 3 unidades de periféricos, a de menor valor sai grátis",
                TipoMecanicaPromocao.LEVE_X_PAGUE_Y,
                "{\"qtdX\": 3, \"qtdY\": 2}",
                10,
                false, // Exclusiva
                true,
                agora.minus(1, ChronoUnit.DAYS),
                futuro,
                "{}",
                null
        );

        // 2. 15% OFF Primeira Compra
        CampanhaPromocional c2 = new CampanhaPromocional(
                UUID.fromString("22222222-0000-0000-0000-000000000002"),
                "15% OFF Primeira Compra",
                "Concede 15% de desconto exclusivo para novos clientes em sua primeira compra",
                TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{\"percentual\": 15.00}",
                20,
                true, // Cumulativa
                true,
                agora.minus(1, ChronoUnit.DAYS),
                futuro,
                "{\"tipo\": \"PRIMEIRA_COMPRA\"}",
                new CondicaoPrimeiraCompraCliente()
        );

        // 3. R$ 50,00 OFF acima de R$ 300,00
        CampanhaPromocional c3 = new CampanhaPromocional(
                UUID.fromString("22222222-0000-0000-0000-000000000003"),
                "R$ 50 OFF Compras Acima de R$ 300",
                "Concede abatimento fixo de R$ 50,00 para carrinhos com subtotal a partir de R$ 300,00",
                TipoMecanicaPromocao.VALOR_FIXO_META_CARRINHO,
                "{\"metaMinima\": 300.00, \"valorDesconto\": 50.00}",
                5,
                true, // Cumulativa
                true,
                agora.minus(1, ChronoUnit.DAYS),
                futuro,
                "{\"tipo\": \"VALOR_MINIMO\", \"minimo\": 300.00}",
                new CondicaoValorMinimoCarrinho(new BigDecimal("300.00"))
        );

        // 4. Combo Cruzado Teclado + Mouse
        CampanhaPromocional c4 = new CampanhaPromocional(
                UUID.fromString("22222222-0000-0000-0000-000000000004"),
                "Combo Casado Teclado + Mouse (20% OFF)",
                "Compre o Teclado Mecânico e o Mouse Óptico juntos e ganhe 20% de desconto no par",
                TipoMecanicaPromocao.COMBO_CRUZADO_CASADO,
                "{\"skuA\": \"DEMO-TEC-PRO-PRETO\", \"skuB\": \"DEMO-MOU-PREC-PRETO\", \"percentual\": 20.00}",
                15,
                true, // Cumulativa
                true,
                agora.minus(1, ChronoUnit.DAYS),
                futuro,
                "{}",
                null
        );

        campanhaRepository.save(c1);
        campanhaRepository.save(c2);
        campanhaRepository.save(c3);
        campanhaRepository.save(c4);
    }

    private void inicializarCuponsDemonstracao() {
        if (cupomRepository.count() > 0) {
            return;
        }

        Instant agora = Instant.now();
        Instant futuro = agora.plus(180, ChronoUnit.DAYS);

        CupomPromocional cupom1 = new CupomPromocional(
                UUID.fromString("33333333-0000-0000-0000-000000000001"),
                "BEMVINDO10",
                TipoDescontoCupom.PERCENTUAL,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                100,
                futuro,
                true, // Cumulativo com outras promoções
                true
        );

        CupomPromocional cupom2 = new CupomPromocional(
                UUID.fromString("33333333-0000-0000-0000-000000000002"),
                "FATEC50",
                TipoDescontoCupom.VALOR_FIXO,
                new BigDecimal("50.00"),
                new BigDecimal("200.00"),
                50,
                futuro,
                false, // Exclusivo
                true
        );

        cupomRepository.save(cupom1);
        cupomRepository.save(cupom2);
    }
}

