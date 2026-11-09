package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

/**
 * Condição folha (Leaf) que valida se a compra atual é a primeira compra do cliente.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Leaf</i> no padrão <i>Composite</i>.
 * Verifica o histórico de compras do cliente no contexto.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoPrimeiraCompraCliente implements CondicaoElegibilidadeComponent {

    /**
     * Construtor padrão da condição de primeira compra.
     */
    public CondicaoPrimeiraCompraCliente() {
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (contexto == null) {
            return false;
        }
        return contexto.isPrimeiraCompraCliente();
    }

    @Override
    public String descrever() {
        return "Cliente em Primeira Compra";
    }
}

