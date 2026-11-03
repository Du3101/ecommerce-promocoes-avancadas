package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;

/**
 * Elo da Cadeia de Responsabilidade que arbitra conflitos de exclusividade mútua entre promoções.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>ConcreteHandler</i> no padrão <i>Chain of Responsibility</i>.
 * Se a campanha sob avaliação for exclusiva e já houver qualquer promoção aprovada, ou se
 * alguma das promoções já aprovadas for exclusiva, a nova promoção é rejeitada para impedir
 * acumulação indevida de descontos concorrentes.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class ValidadorConflitoExclusividade extends ValidadorPromocaoHandler {

    @Override
    public boolean validar(RequisicaoValidacaoPromocao requisicao) {
        if (requisicao == null || requisicao.getCampanha() == null) {
            return false;
        }

        // Se já existem promoções aprovadas, verifica regras de exclusividade
        if (!requisicao.getPromocoesJaAprovadas().isEmpty()) {
            // Se a nova promoção for exclusiva (não cumulativa), ela não pode entrar
            if (!requisicao.getCampanha().isCumulativa()) {
                return false;
            }
            // Se qualquer uma das promoções já aprovadas for exclusiva, nenhuma nova promoção pode entrar
            for (CampanhaPromocional aprovada : requisicao.getPromocoesJaAprovadas()) {
                if (!aprovada.isCumulativa()) {
                    return false;
                }
            }
        }

        return repassarParaProximo(requisicao);
    }
}

