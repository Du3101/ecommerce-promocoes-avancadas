package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entidade de domínio que representa um endereço físico de entrega cadastrado por um cliente (FR-003).
 *
 * <p>Responsabilidade Comercial: Armazenar a localização completa de entrega e responder ao
 * contrato polimórfico {@link OpcaoEnderecoEntrega} para cálculo de frete e elegibilidade regional.</p>
 */
@Entity
@Table(name = "enderecos_cliente")
public class Endereco implements OpcaoEnderecoEntrega {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "apelido", length = 60, nullable = false)
    private String apelido;

    @Column(name = "cep", length = 8, nullable = false)
    private String cep;

    @Column(name = "logradouro", length = 180, nullable = false)
    private String logradouro;

    @Column(name = "numero", length = 30, nullable = false)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", length = 100, nullable = false)
    private String bairro;

    @Column(name = "cidade", length = 100, nullable = false)
    private String cidade;

    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    @Column(name = "selecionado_para_entrega", nullable = false)
    private boolean selecionadoParaEntrega;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor do {@link EnderecoBuilder} (<b>Builder</b>).</p>
     */
    protected Endereco() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor de pacote invocado exclusivamente pelo {@link EnderecoBuilder}.
     *
     * <p>Objetivo Funcional: Instanciar o endereço já validado pelo Builder.</p>
     * <p>Papel no Design Pattern: Produto concreto construído pelo padrão <b>Builder</b>.</p>
     *
     * @param id identificador único
     * @param clienteId identificador do cliente proprietário
     * @param apelido nome amigável (ex.: "Casa", "Trabalho")
     * @param cep CEP numérico de 8 dígitos
     * @param logradouro rua ou avenida
     * @param numero número do imóvel
     * @param complemento complemento opcional
     * @param bairro bairro
     * @param cidade município
     * @param uf sigla de 2 letras do estado
     * @param selecionadoParaEntrega indicador de seleção ativa para entrega
     */
    Endereco(
        UUID id,
        UUID clienteId,
        String apelido,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        boolean selecionadoParaEntrega
    ) {
        this.id = id;
        this.clienteId = clienteId;
        this.apelido = apelido;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.selecionadoParaEntrega = selecionadoParaEntrega;
    }

    /**
     * Informa que este objeto representa um endereço físico real e válido para entrega.
     *
     * <p>Objetivo Funcional: Autorizar o cálculo de frete e prosseguimento do checkout.</p>
     * <p>Papel no Design Pattern: Implementação real em contrapartida ao {@link EnderecoAusenteNullObject}.</p>
     *
     * @return sempre {@code true}
     */
    @Override
    public boolean possuiEnderecoValido() {
        return true;
    }

    /**
     * Obtém o CEP de 8 dígitos numéricos do endereço.
     *
     * <p>Objetivo Funcional: Fornecer o CEP para cálculo de frete.</p>
     * <p>Papel no Design Pattern: Implementação real da interface {@link OpcaoEnderecoEntrega}.</p>
     *
     * @return CEP de 8 dígitos
     */
    @Override
    public String obterCep() {
        return this.cep;
    }

    /**
     * Obtém a sigla de 2 letras da Unidade Federativa (UF).
     *
     * <p>Objetivo Funcional: Fornecer a UF para elegibilidade regional de promoções e cálculo de frete.</p>
     * <p>Papel no Design Pattern: Implementação real da interface {@link OpcaoEnderecoEntrega}.</p>
     *
     * @return sigla da UF em maiúsculas (ex.: "SP")
     */
    @Override
    public String obterUfRegiao() {
        return this.uf;
    }

    /**
     * Formata uma descrição completa e legível do endereço para exibição no perfil e gravação no pedido.
     *
     * <p>Objetivo Funcional: Apresentar logradouro, número, complemento, bairro, cidade/UF e CEP.</p>
     * <p>Papel no Design Pattern: Implementação concreta da formatação em {@link OpcaoEnderecoEntrega}.</p>
     *
     * @return resumo textual completo do endereço
     */
    @Override
    public String obterResumoFormatado() {
        String comp = (this.complemento != null && !this.complemento.isBlank()) ? " (" + this.complemento + ")" : "";
        return String.format(
            "[%s] %s, %s%s - %s, %s/%s - CEP %s",
            this.apelido,
            this.logradouro,
            this.numero,
            comp,
            this.bairro,
            this.cidade,
            this.uf,
            this.cep
        );
    }

    /**
     * Altera o estado de seleção deste endereço para uso na próxima entrega do cliente.
     *
     * <p>Objetivo Funcional: Marcar ou desmarcar este endereço como ativo para entrega (FR-003).</p>
     * <p>Papel no Design Pattern: Mutação controlada pelo agregado {@link Cliente}.</p>
     *
     * @param selecionado {@code true} se este endereço foi escolhido para entrega
     */
    public void definirSelecionadoParaEntrega(boolean selecionado) {
        this.selecionadoParaEntrega = selecionado;
    }

    /**
     * Obtém o identificador único do endereço.
     *
     * <p>Objetivo Funcional: Identificar o endereço nas rotas de seleção e exclusão.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do endereço
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o identificador do cliente proprietário deste endereço.
     *
     * <p>Objetivo Funcional: Garantir que um cliente só manipule seus próprios endereços.</p>
     * <p>Papel no Design Pattern: Getter explícito de vínculo de propriedade.</p>
     *
     * @return UUID do cliente
     */
    public UUID obterClienteId() {
        return this.clienteId;
    }

    /**
     * Obtém o apelido amigável do endereço (ex.: "Casa", "Trabalho").
     *
     * <p>Objetivo Funcional: Facilitar a escolha do endereço pelo cliente.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return apelido do endereço
     */
    public String obterApelido() {
        return this.apelido;
    }

    /**
     * Obtém o logradouro do endereço.
     *
     * <p>Objetivo Funcional: Informar rua/avenida para entrega.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return logradouro
     */
    public String obterLogradouro() {
        return this.logradouro;
    }

    /**
     * Obtém o número do imóvel.
     *
     * <p>Objetivo Funcional: Informar número predial para entrega.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return número do endereço
     */
    public String obterNumero() {
        return this.numero;
    }

    /**
     * Obtém o complemento opcional do endereço.
     *
     * <p>Objetivo Funcional: Informar apartamento/bloco quando existente.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return complemento ou {@code null}
     */
    public String obterComplemento() {
        return this.complemento;
    }

    /**
     * Obtém o bairro do endereço.
     *
     * <p>Objetivo Funcional: Informar bairro para entrega.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return bairro
     */
    public String obterBairro() {
        return this.bairro;
    }

    /**
     * Obtém a cidade do endereço.
     *
     * <p>Objetivo Funcional: Informar município de destino.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return cidade
     */
    public String obterCidade() {
        return this.cidade;
    }

    /**
     * Indica se este endereço está atualmente marcado como selecionado para entrega.
     *
     * <p>Objetivo Funcional: Identificar qual endereço da lista do cliente é o ativo.</p>
     * <p>Papel no Design Pattern: Consulta de estado de seleção.</p>
     *
     * @return {@code true} se selecionado para entrega
     */
    public boolean estaSelecionadoParaEntrega() {
        return this.selecionadoParaEntrega;
    }
}

