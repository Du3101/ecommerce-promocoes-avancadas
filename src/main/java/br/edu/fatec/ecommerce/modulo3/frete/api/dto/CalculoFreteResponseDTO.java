package br.edu.fatec.ecommerce.modulo3.frete.api.dto;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta da cotação de frete com todas as opções disponíveis.
 *
 * <p><b>Papel Arquitetural:</b> DTO de saída da camada HTTP (formato do contrato
 * {@code contracts/modulo3-api-and-integration-ports.md}).
 *
 * @param cepDestino CEP formatado ({@code 00000-000})
 * @param regiao região de entrega identificada pelo CEP
 * @param pesoTotalGramas peso considerado
 * @param freteGratisDisponivel se o pedido atende à política de frete grátis
 * @param opcoes opções de frete ordenadas pelo menor valor
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record CalculoFreteResponseDTO(
        String cepDestino,
        String regiao,
        int pesoTotalGramas,
        boolean freteGratisDisponivel,
        List<OpcaoFreteDTO> opcoes
) {

    /**
     * Uma opção de frete.
     *
     * @param modalidade identificador da modalidade
     * @param nome nome de exibição
     * @param transportadora empresa responsável
     * @param valorOriginal preço da transportadora
     * @param valorCobrado preço cobrado do cliente
     * @param prazoDiasUteis prazo em dias úteis
     * @param freteGratisAplicado se o frete grátis zerou esta opção
     */
    public record OpcaoFreteDTO(
            ModalidadeFrete modalidade,
            String nome,
            String transportadora,
            BigDecimal valorOriginal,
            BigDecimal valorCobrado,
            int prazoDiasUteis,
            boolean freteGratisAplicado
    ) {

        /**
         * Converte o Value Object interno na representação HTTP.
         *
         * @param cotacao cotação interna
         * @return DTO de saída
         */
        public static OpcaoFreteDTO de(CotacaoFrete cotacao) {
            return new OpcaoFreteDTO(cotacao.modalidade(), cotacao.modalidade().getNomeExibicao(),
                    cotacao.transportadora(), cotacao.valorOriginal(), cotacao.valorCobrado(),
                    cotacao.prazoDiasUteis(), cotacao.freteGratisAplicado());
        }
    }
}
