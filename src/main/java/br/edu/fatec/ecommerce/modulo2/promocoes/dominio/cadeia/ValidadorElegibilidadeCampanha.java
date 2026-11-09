package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia;

/**
 * Elo da Cadeia de Responsabilidade que avalia a árvore de elegibilidade da campanha (Composite).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>ConcreteHandler</i> no padrão <i>Chain of Responsibility</i>,
 * integrando-se ao padrão <i>Composite</i> ao delegar a checagem lógica para
 * {@link br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional#isElegivel(br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade)}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class ValidadorElegibilidadeCampanha extends ValidadorPromocaoHandler {

    @Override
    public boolean validar(RequisicaoValidacaoPromocao requisicao) {
        if (requisicao == null || requisicao.getCampanha() == null) {
            return false;
        }
        if (!requisicao.getCampanha().isElegivel(requisicao.getContextoElegibilidade())) {
            return false;
        }
        return repassarParaProximo(requisicao);
    }
}

