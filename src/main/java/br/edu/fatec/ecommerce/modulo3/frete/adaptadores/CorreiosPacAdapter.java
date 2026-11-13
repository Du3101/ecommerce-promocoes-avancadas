package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiCorreiosSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import org.springframework.stereotype.Component;

/**
 * Adaptador do serviço PAC (econômico) dos Correios.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i>. A regra de frete grátis <b>não</b>
 * fica aqui (ela é da loja, não dos Correios): está em {@code PoliticaFreteGratis}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CorreiosPacAdapter extends AdaptadorCorreiosBase {

    /**
     * Construtor com a API dos Correios injetada.
     *
     * @param apiCorreios cliente da API dos Correios
     */
    public CorreiosPacAdapter(ApiCorreiosSimulada apiCorreios) {
        super(apiCorreios);
    }

    /**
     * Modalidade atendida.
     *
     * @return {@link ModalidadeFrete#CORREIOS_PAC}
     */
    @Override
    public ModalidadeFrete modalidade() {
        return ModalidadeFrete.CORREIOS_PAC;
    }

    /**
     * Código do PAC na API dos Correios.
     *
     * @return {@code "04510"}
     */
    @Override
    protected String codigoServico() {
        return ApiCorreiosSimulada.SERVICO_PAC;
    }
}
