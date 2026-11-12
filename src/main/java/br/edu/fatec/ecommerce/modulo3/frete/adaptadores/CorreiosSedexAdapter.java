package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiCorreiosSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import org.springframework.stereotype.Component;

/**
 * Adaptador do serviço SEDEX (expresso) dos Correios.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i> sobre a mesma API do PAC, mudando apenas
 * o código de serviço.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CorreiosSedexAdapter extends AdaptadorCorreiosBase {

    /**
     * Construtor com a API dos Correios injetada.
     *
     * @param apiCorreios cliente da API dos Correios
     */
    public CorreiosSedexAdapter(ApiCorreiosSimulada apiCorreios) {
        super(apiCorreios);
    }

    /**
     * Modalidade atendida.
     *
     * @return {@link ModalidadeFrete#CORREIOS_SEDEX}
     */
    @Override
    public ModalidadeFrete modalidade() {
        return ModalidadeFrete.CORREIOS_SEDEX;
    }

    /**
     * Código do SEDEX na API dos Correios.
     *
     * @return {@code "04014"}
     */
    @Override
    protected String codigoServico() {
        return ApiCorreiosSimulada.SERVICO_SEDEX;
    }
}
