package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Gera números comerciais legíveis e únicos para os pedidos, no formato {@code PED-AAAAMM-XXXXXX}.
 *
 * <p>O sufixo é aleatório (e não sequencial) para não revelar o volume de vendas da loja e para não depender de
 * uma sequência no banco. A unicidade é verificada no repositório e garantida pela restrição {@code UNIQUE}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class GeradorNumeroPedido {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_SUFIXO = 6;
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyyMM");

    private final SecureRandom aleatorio = new SecureRandom();
    private final PedidoJpaRepository pedidoRepository;

    /**
     * Construtor com o repositório para checagem de unicidade.
     *
     * @param pedidoRepository repositório de pedidos
     */
    public GeradorNumeroPedido(PedidoJpaRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Gera um número de pedido ainda não utilizado.
     *
     * @return número no formato {@code PED-202611-K7Q2MX}
     */
    public String gerar() {
        String prefixo = "PED-" + YearMonth.now(ZoneId.of("America/Sao_Paulo")).format(FORMATO_MES) + "-";
        String candidato;
        do {
            candidato = prefixo + sufixoAleatorio();
        } while (pedidoRepository.existsByNumeroPedido(candidato));
        return candidato;
    }

    /**
     * Sorteia o sufixo com letras e dígitos sem caracteres ambíguos (sem 0/O, 1/I).
     *
     * @return sufixo de 6 caracteres
     */
    private String sufixoAleatorio() {
        StringBuilder sufixo = new StringBuilder(TAMANHO_SUFIXO);
        for (int i = 0; i < TAMANHO_SUFIXO; i++) {
            sufixo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return sufixo.toString();
    }
}
