package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

/**
 * Interface Component do padrão Composite para avaliação recursiva de condições de elegibilidade.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Component</i> central do padrão <i>Composite</i> (Constituição, Princípio II).
 * Permite que expressões lógicas booleanas complexas (aninhando {@code AND}, {@code OR}, {@code NOT})
 * e predicados atômicos sobre clientes, subtotais e categorias sejam tratados com polimorfismo
 * uniforme através do método {@link #avaliar(ContextoElegibilidade)}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface CondicaoElegibilidadeComponent {

    /**
     * Avalia se a condição é satisfeita frente ao contexto do carrinho e cliente.
     *
     * <p>Objetivo funcional: Executa a validação lógica booleana da regra de negócio.
     * <p>Papel no padrão: Operação polimórfica executada recursivamente por folhas e composições.
     *
     * @param contexto Contexto com os dados do cliente, carrinho e catálogo.
     * @return true se a condição for satisfeita; false caso contrário.
     */
    boolean avaliar(ContextoElegibilidade contexto);

    /**
     * Retorna a descrição legível da condição ou árvore para auditoria e logs.
     *
     * @return String descritiva da regra.
     */
    String descrever();
}

