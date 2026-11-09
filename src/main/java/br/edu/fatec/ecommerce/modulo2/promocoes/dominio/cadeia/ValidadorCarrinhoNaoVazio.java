package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia;

/**
 * Elo da Cadeia de Responsabilidade que valida se o carrinho possui itens.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>ConcreteHandler</i> no padrão <i>Chain of Responsibility</i>.
 * Interrompe o pipeline se o carrinho estiver vazio, evitando cálculos desnecessários.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class ValidadorCarrinhoNaoVazio extends ValidadorPromocaoHandler {

    @Override
    public boolean validar(RequisicaoValidacaoPromocao requisicao) {
        if (requisicao == null || requisicao.getCarrinho() == null || requisicao.getCarrinho().getItens().isEmpty()) {
            return false;
        }
        return repassarParaProximo(requisicao);
    }
}

