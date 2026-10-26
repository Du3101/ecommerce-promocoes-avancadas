package br.edu.fatec.ecommerce.modulo1.configuracao;

import java.time.Duration;

/**
 * Gerenciador centralizado de configurações operacionais do Módulo 1 (Catálogo, Estoque e Identidade).
 *
 * <p><b>Design Pattern Aplicado: Singleton (Padrão Criacional)</b></p>
 * <ul>
 *   <li><b>1. Qual problema de projeto existia?</b> Parâmetros globais do módulo (como o tempo de
 *   expiração TTL de reservas de estoque abandonadas, a profundidade máxima permitida para árvores
 *   recursivas de categorias/combos e o código da moeda padrão) não podiam ficar espalhados como
 *   constantes desconexas nem ter múltiplas instâncias divergentes em memória.</li>
 *   <li><b>2. Por que este padrão foi escolhido?</b> O padrão Singleton garante a existência de uma
 *   única instância controlada e thread-safe em toda a JVM, oferecendo um ponto único de consulta e
 *   ajuste controlado durante testes ou demonstrações.</li>
 *   <li><b>3. O que ficou mais simples de modificar?</b> Alterar o TTL de reserva de estoque (ex.: de
 *   30 minutos para 1 minuto durante a defesa do projeto) reflete imediatamente em todas as novas
 *   reservas criadas pelo sistema sem alterar classes de serviço ou entidades.</li>
 * </ul>
 */
public final class ConfiguracaoModuloCatalogoEstoqueSingleton {

    /**
     * Instância única volátil para garantia de visibilidade entre múltiplas threads (Double-Checked Locking).
     */
    private static volatile ConfiguracaoModuloCatalogoEstoqueSingleton instanciaUnica;

    /**
     * Prazo padrão de validade (TTL) de uma reserva de estoque ativa antes de expirar por abandono.
     */
    private volatile Duration tempoExpiracaoReservaPadrao;

    /**
     * Limite máximo de níveis permitidos em estruturas recursivas (Composite de Categorias e Combos).
     */
    private volatile int profundidadeMaximaArvoreRecursiva;

    /**
     * Código ISO da moeda oficial utilizada no catálogo comercial.
     */
    private final String codigoMoedaPadrao;

    /**
     * Construtor privado explícito para impedir instanciação externa direta com o operador {@code new}.
     *
     * <p>Objetivo Funcional: Inicializar os parâmetros operacionais padrão do Módulo 1
     * (TTL de 30 minutos conforme FR-011, profundidade máxima de 10 níveis e moeda BRL).</p>
     * <p>Papel no Design Pattern: Construtor encapsulado exclusivo do padrão <b>Singleton</b>.</p>
     */
    private ConfiguracaoModuloCatalogoEstoqueSingleton() {
        // Define o TTL padrão de 30 minutos para reservas de estoque de checkout (FR-011)
        this.tempoExpiracaoReservaPadrao = Duration.ofMinutes(30);
        // Define o limite seguro de 10 níveis para árvores Composite de categorias e combos
        this.profundidadeMaximaArvoreRecursiva = 10;
        // Define a moeda oficial do catálogo como Real Brasileiro (BRL)
        this.codigoMoedaPadrao = "BRL";
    }

    /**
     * Obtém a instância única global do gerenciador de configurações do Módulo 1.
     *
     * <p>Objetivo Funcional: Fornecer acesso global e seguro em cenários concorrentes aos
     * parâmetros de catálogo e estoque.</p>
     * <p>Papel no Design Pattern: Método estático de acesso global (<i>getInstance</i>) do padrão
     * <b>Singleton</b> com bloqueio duplamente verificado (<i>Double-Checked Locking</i>).</p>
     *
     * @return a instância única de {@link ConfiguracaoModuloCatalogoEstoqueSingleton}
     */
    public static ConfiguracaoModuloCatalogoEstoqueSingleton obterInstancia() {
        // Primeira verificação sem bloqueio para máxima performance em leituras concorrentes
        if (instanciaUnica == null) {
            synchronized (ConfiguracaoModuloCatalogoEstoqueSingleton.class) {
                // Segunda verificação dentro do bloco sincronizado para garantir criação única
                if (instanciaUnica == null) {
                    instanciaUnica = new ConfiguracaoModuloCatalogoEstoqueSingleton();
                }
            }
        }
        return instanciaUnica;
    }

    /**
     * Consulta o prazo atual de validade (TTL) configurado para novas reservas de estoque.
     *
     * <p>Objetivo Funcional: Informar ao serviço de estoque por quanto tempo uma reserva de
     * checkout permanece ativa antes de poder ser expirada automaticamente.</p>
     * <p>Papel no Design Pattern: Operação de leitura de estado encapsulado no <b>Singleton</b>.</p>
     *
     * @return duração do TTL de reserva de estoque (padrão: 30 minutos)
     */
    public Duration obterTempoExpiracaoReservaPadrao() {
        return this.tempoExpiracaoReservaPadrao;
    }

    /**
     * Consulta o prazo atual de validade (TTL) de reservas de estoque expresso em minutos inteiros.
     *
     * <p>Objetivo Funcional: Facilitar o cálculo de {@code expiraEm} na criação de reservas.</p>
     * <p>Papel no Design Pattern: Operação de leitura do estado encapsulado no <b>Singleton</b>.</p>
     *
     * @return TTL de reserva em minutos (padrão: 30)
     */
    public int obterTtlReservaEstoqueMinutos() {
        return (int) this.tempoExpiracaoReservaPadrao.toMinutes();
    }

    /**
     * Atualiza de forma controlada o prazo de validade (TTL) para novas reservas de estoque.
     *
     * <p>Objetivo Funcional: Permitir parametrizar o tempo de expiração de reservas (inclusive
     * para simulações de expiração rápida em testes automatizados e na apresentação).</p>
     * <p>Papel no Design Pattern: Mutação segura e validada do estado global no <b>Singleton</b>.</p>
     *
     * @param novoTempo nova duração positiva para expiração de reservas
     * @throws IllegalArgumentException caso a duração seja nula, zero ou negativa
     */
    public synchronized void atualizarTempoExpiracaoReservaPadrao(Duration novoTempo) {
        // Valida explicitamente se o novo prazo é positivo antes de aplicar a mudança global
        if (novoTempo == null || novoTempo.isZero() || novoTempo.isNegative()) {
            throw new IllegalArgumentException("O tempo de expiração da reserva deve ser estritamente positivo.");
        }
        this.tempoExpiracaoReservaPadrao = novoTempo;
    }

    /**
     * Consulta a profundidade máxima permitida para árvores recursivas de categorias e combos.
     *
     * <p>Objetivo Funcional: Proteger o sistema contra árvores excessivamente profundas na
     * composição recursiva.</p>
     * <p>Papel no Design Pattern: Consulta de diretriz estrutural no <b>Singleton</b>.</p>
     *
     * @return número máximo de níveis hierárquicos permitidos (padrão: 10)
     */
    public int obterProfundidadeMaximaArvoreRecursiva() {
        return this.profundidadeMaximaArvoreRecursiva;
    }

    /**
     * Atualiza a profundidade máxima permitida para árvores recursivas.
     *
     * <p>Objetivo Funcional: Ajustar o limite de aninhamento de subcategorias e subkits.</p>
     * <p>Papel no Design Pattern: Configuração controlada no <b>Singleton</b>.</p>
     *
     * @param novaProfundidade número inteiro maior ou igual a 2
     * @throws IllegalArgumentException caso a profundidade informada seja menor que 2
     */
    public synchronized void atualizarProfundidadeMaximaArvoreRecursiva(int novaProfundidade) {
        if (novaProfundidade < 2) {
            throw new IllegalArgumentException("A profundidade máxima da árvore recursiva deve ser de pelo menos 2 níveis.");
        }
        this.profundidadeMaximaArvoreRecursiva = novaProfundidade;
    }

    /**
     * Consulta o código ISO da moeda oficial configurada para o catálogo.
     *
     * <p>Objetivo Funcional: Padronizar a unidade monetária das ofertas da loja ("BRL").</p>
     * <p>Papel no Design Pattern: Propriedade imutável exposta pelo <b>Singleton</b>.</p>
     *
     * @return string contendo o código ISO da moeda ("BRL")
     */
    public String obterCodigoMoedaPadrao() {
        return this.codigoMoedaPadrao;
    }

    /**
     * Restaura todas as configurações do Singleton para os valores de fábrica originais.
     *
     * <p>Objetivo Funcional: Garantir isolamento entre testes automatizados que alterem
     * temporariamente o TTL ou a profundidade máxima.</p>
     * <p>Papel no Design Pattern: Método explícito de reset de estado do <b>Singleton</b>.</p>
     */
    public synchronized void restaurarValoresPadrao() {
        this.tempoExpiracaoReservaPadrao = Duration.ofMinutes(30);
        this.profundidadeMaximaArvoreRecursiva = 10;
    }
}

