package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import java.math.BigDecimal;

/**
 * Produto da família de pagamento que transforma a resposta do gateway em instruções legíveis para o cliente.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> segundo "produto abstrato" da <b>Abstract Factory</b>
 * ({@link FabricaProvedorPagamento}). O texto do PIX ("copie a chave, válida por 30 minutos") é muito diferente
 * do texto do boleto ("pague até o vencimento"), e os dois precisam ser sempre usados junto com o gateway da mesma
 * família — por isso são criados pela mesma fábrica.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@FunctionalInterface
public interface FormatadorInstrucoesPagamento {

    /**
     * Gera as instruções de pagamento.
     *
     * @param resposta resposta do gateway
     * @param valor valor da cobrança
     * @return texto para e-mail/tela
     */
    String formatar(RespostaPagamentoDTO resposta, BigDecimal valor);
}
