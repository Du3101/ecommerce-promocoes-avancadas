package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.fabrica;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import org.springframework.stereotype.Component;

/**
 * Criador concreto especializado na fabricação de pacotes compostos {@link ComboKitComposite} (FR-008).
 *
 * <p>Aplicação do Padrão GoF: <b>Factory Method</b> — papel <i>ConcreteCreator</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Sobrescreve o Factory Method de {@link CriadorItemCatalogo} para instanciar um
 * {@link ComboKitComposite} a partir de um {@link ComboKitBuilder}, aplicando as validações universais
 * de preço positivo, peso positivo e categoria obrigatória.</p>
 */
@Component
public class CriadorComboKitPromocional extends CriadorItemCatalogo<ComboKitBuilder> {

    /**
     * Construtor padrão explícito do criador concreto de combos/kits promocionais.
     *
     * <p>Objetivo Funcional: Disponibilizar o componente Spring de fabricação de combos.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>ConcreteCreator</i> do padrão <b>Factory Method</b>.</p>
     */
    public CriadorComboKitPromocional() {
        super();
    }

    /**
     * Implementação concreta do Factory Method que constrói e retorna um {@link ComboKitComposite}.
     *
     * <p>Objetivo Funcional: Instanciar o nó <i>Composite</i> do catálogo a partir do seu builder.</p>
     * <p>Papel no Design Pattern: Sobrescrita concreta do <b>Factory Method</b>.</p>
     *
     * @param builder instância configurada de {@link ComboKitBuilder}
     * @return instância de {@link ComboKitComposite} como {@link ItemCatalogoComponent}
     */
    @Override
    protected ItemCatalogoComponent criarItemCatalogo(ComboKitBuilder builder) {
        return builder.construir();
    }

    /**
     * Fabrica de forma tipada um {@link ComboKitComposite} validado pelas invariantes do {@link CriadorItemCatalogo}.
     *
     * <p>Objetivo Funcional: Facilitar a obtenção direta da referência tipada {@link ComboKitComposite}
     * para persistência JPA.</p>
     * <p>Papel no Design Pattern: Método auxiliar que delega ao fluxo {@link #fabricarItemValidado}.</p>
     *
     * @param builder builder configurado com os componentes do kit
     * @return instância validada de {@link ComboKitComposite}
     */
    public ComboKitComposite fabricarComboKitValidado(ComboKitBuilder builder) {
        return (ComboKitComposite) fabricarItemValidado(builder);
    }
}

