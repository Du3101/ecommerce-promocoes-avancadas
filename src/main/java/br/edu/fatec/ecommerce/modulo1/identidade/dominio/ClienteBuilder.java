package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import java.util.UUID;

/**
 * Construtor fluente passo a passo para criação validada de instâncias de {@link Cliente}.
 *
 * <p>Aplicação do Padrão GoF: <b>Builder</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: Garante que nenhum perfil de {@link Cliente} seja instanciado sem vínculo
 * com um {@link Usuario}, sem validação de CPF (11 dígitos numéricos não repetidos), nome completo
 * e telefone de contato (FR-001).</p>
 */
public class ClienteBuilder {

    private UUID id;
    private UUID usuarioId;
    private String nomeCompleto;
    private String cpf;
    private String telefone;
    private boolean primeiraCompraConcluida = false;

    /**
     * Construtor padrão do Builder de cliente.
     *
     * <p>Objetivo Funcional: Iniciar a montagem fluente de um novo {@link Cliente}.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>ConcreteBuilder</i> do padrão <b>Builder</b>.</p>
     */
    public ClienteBuilder() {
        // Inicialização padrão do Builder
    }

    /**
     * Define opcionalmente o identificador único do cliente (gerado automaticamente se omitido).
     *
     * <p>Objetivo Funcional: Permitir atribuição explícita de UUID em testes ou migrações.</p>
     * <p>Papel no Design Pattern: Passo fluente de configuração do <b>Builder</b>.</p>
     *
     * @param id UUID do cliente
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder comId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Define o identificador da conta {@link Usuario} associada ao cliente.
     *
     * <p>Objetivo Funcional: Vincular as credenciais de autenticação ao perfil comercial do comprador.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param usuarioId UUID da conta de usuário
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder paraUsuario(UUID usuarioId) {
        this.usuarioId = usuarioId;
        return this;
    }

    /**
     * Define o nome completo do cliente.
     *
     * <p>Objetivo Funcional: Registrar a identificação civil do comprador.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param nomeCompleto nome completo (3 a 150 caracteres)
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder comNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
        return this;
    }

    /**
     * Define o CPF do cliente (com ou sem máscara, sendo higienizado na construção).
     *
     * <p>Objetivo Funcional: Informar o documento fiscal único do comprador.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param cpf CPF contendo 11 dígitos numéricos
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder comCpf(String cpf) {
        this.cpf = cpf;
        return this;
    }

    /**
     * Define o telefone de contato do cliente.
     *
     * <p>Objetivo Funcional: Informar telefone para contato logístico.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param telefone número de telefone (até 20 caracteres)
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder comTelefone(String telefone) {
        this.telefone = telefone;
        return this;
    }

    /**
     * Define se o cliente já concluiu sua primeira compra anteriormente.
     *
     * <p>Objetivo Funcional: Configurar o indicador inicial de elegibilidade para promoção de primeira compra.</p>
     * <p>Papel no Design Pattern: Passo fluente opcional do <b>Builder</b> (padrão {@code false}).</p>
     *
     * @param primeiraCompraConcluida {@code true} se já realizou compra; {@code false} caso contrário
     * @return a própria instância de {@link ClienteBuilder}
     */
    public ClienteBuilder comPrimeiraCompraConcluida(boolean primeiraCompraConcluida) {
        this.primeiraCompraConcluida = primeiraCompraConcluida;
        return this;
    }

    /**
     * Valida todas as regras cadastrais e constrói a instância final de {@link Cliente}.
     *
     * <p>Objetivo Funcional: Assegurar integridade de nome, CPF e telefone antes de criar a entidade.</p>
     * <p>Papel no Design Pattern: Método finalizador ({@code build()}) do padrão <b>Builder</b>.</p>
     *
     * @return instância íntegra e validada de {@link Cliente}
     * @throws RegraNegocioModulo1Exception caso qualquer campo obrigatório seja inválido
     */
    public Cliente construir() {
        if (this.usuarioId == null) {
            throw new RegraNegocioModulo1Exception("O cliente deve estar vinculado a um usuário válido.");
        }
        if (this.nomeCompleto == null || this.nomeCompleto.isBlank()
            || this.nomeCompleto.trim().length() < 3 || this.nomeCompleto.trim().length() > 150) {
            throw new RegraNegocioModulo1Exception(
                "O nome completo do cliente é obrigatório e deve ter entre 3 e 150 caracteres."
            );
        }
        String cpfHigienizado = validarEHigienizarCpf(this.cpf);
        if (this.telefone == null || this.telefone.isBlank() || this.telefone.trim().length() > 20) {
            throw new RegraNegocioModulo1Exception(
                "O telefone do cliente é obrigatório e deve ter no máximo 20 caracteres."
            );
        }

        UUID idFinal = this.id != null ? this.id : UUID.randomUUID();
        return new Cliente(
            idFinal,
            this.usuarioId,
            this.nomeCompleto.trim(),
            cpfHigienizado,
            this.telefone.trim(),
            this.primeiraCompraConcluida
        );
    }

    /**
     * Higieniza e valida o formato e os dígitos do CPF informado.
     *
     * <p>Objetivo Funcional: Rejeitar CPFs nulos, com tamanho diferente de 11 dígitos ou formados
     * por algarismos repetidos (ex.: {@code 11111111111}).</p>
     * <p>Papel no Design Pattern: Validação interna auxiliar do <b>Builder</b>.</p>
     *
     * @param cpfBruto string de CPF recebida
     * @return CPF contendo exatamente 11 dígitos numéricos
     * @throws RegraNegocioModulo1Exception se o CPF for inválido
     */
    private String validarEHigienizarCpf(String cpfBruto) {
        if (cpfBruto == null || cpfBruto.isBlank()) {
            throw new RegraNegocioModulo1Exception("O CPF do cliente é obrigatório.");
        }
        String apenasDigitos = cpfBruto.replaceAll("[.\\-\\s]", "");
        if (!apenasDigitos.matches("^\\d{11}$")) {
            throw new RegraNegocioModulo1Exception("O CPF deve conter exatamente 11 dígitos numéricos.");
        }
        if (apenasDigitos.chars().distinct().count() == 1) {
            throw new RegraNegocioModulo1Exception("O CPF informado é inválido (dígitos repetidos).");
        }
        return apenasDigitos;
    }
}

