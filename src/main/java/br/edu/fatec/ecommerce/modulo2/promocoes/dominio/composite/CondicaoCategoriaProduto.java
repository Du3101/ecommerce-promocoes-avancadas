package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;

import java.util.Objects;
import java.util.UUID;

/**
 * Condição folha (Leaf) que valida se o carrinho contém pelo menos um item pertencente
 * à categoria alvo ou a qualquer uma de suas subcategorias recursivas.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Leaf</i> no padrão <i>Composite</i>, integrando-se via
 * {@link PortaConsultaCatalogoCliente} para validação polimórfica da árvore hierárquica
 * de categorias do Módulo 1.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoCategoriaProduto implements CondicaoElegibilidadeComponent {

    private final UUID categoriaId;

    /**
     * Construtor da condição de categoria.
     *
     * @param categoriaId UUID da categoria alvo.
     */
    public CondicaoCategoriaProduto(UUID categoriaId) {
        this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId não pode ser nulo");
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (contexto == null || contexto.getItens().isEmpty()) {
            return false;
        }
        PortaConsultaCatalogoCliente porta = contexto.getPortaConsultaCatalogo();
        for (ItemCarrinho item : contexto.getItens()) {
            // Checagem direta caso a categoriaId coincida no item
            if (this.categoriaId.equals(item.getCategoriaId())) {
                return true;
            }
            // Checagem recursiva através da porta de catálogo do Módulo 1
            if (porta != null && porta.itemPertenceACategoriaOuSubcategoria(item.getSku(), this.categoriaId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String descrever() {
        return "Itens pertencentes à Categoria " + categoriaId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }
}

