package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia;

/**
 * Handler base abstrato da Cadeia de Responsabilidade para validação de promoções.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa o <i>Handler</i> abstrato no padrão <i>Chain of Responsibility</i> (Constituição, Princípio II).
 * Mantém a referência para o próximo elo da cadeia através do método {@link #definirProximo(ValidadorPromocaoHandler)}
 * e fornece o template de repasse ou interrupção através de {@link #validar(RequisicaoValidacaoPromocao)}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public abstract class ValidadorPromocaoHandler {

    private ValidadorPromocaoHandler proximo;

    /**
     * Define o próximo elo na cadeia de responsabilidade.
     *
     * <p>Objetivo funcional: Conecta os elos sequenciais da validação.
     * <p>Papel no padrão: Encadeamento fluente do Chain of Responsibility.
     *
     * @param proximo Próximo handler a ser executado.
     * @return O próximo handler para permitir chamadas encadeadas.
     */
    public ValidadorPromocaoHandler definirProximo(ValidadorPromocaoHandler proximo) {
        this.proximo = proximo;
        return proximo;
    }

    /**
     * Executa a validação específica deste elo.
     *
     * <p>Objetivo funcional: Avalia a regra e decide se aprova e repassa ou se rejeita a campanha.
     * <p>Papel no padrão: Método de processamento do Handler.
     *
     * @param requisicao Objeto contendo o carrinho, a campanha e as promoções já aprovadas.
     * @return true se a promoção passou neste elo e em todos os elos seguintes; false se foi rejeitada.
     */
    public abstract boolean validar(RequisicaoValidacaoPromocao requisicao);

    /**
     * Repassa a requisição para o próximo elo da cadeia, se houver.
     *
     * <p>Objetivo funcional: Continuidade da verificação pelos demais elos.
     * <p>Papel no padrão: Delegação para {@code proximo.validar(requisicao)}.
     *
     * @param requisicao Requisição sendo validada.
     * @return true se o próximo elo aprovou (ou se for o fim da cadeia); false se algum elo subsequente reprovou.
     */
    protected boolean repassarParaProximo(RequisicaoValidacaoPromocao requisicao) {
        if (proximo == null) {
            return true;
        }
        return proximo.validar(requisicao);
    }
}

