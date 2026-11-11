package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object imutável que representa o snapshot do endereço de entrega de um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>Value Object</i> e <i>Snapshot Imutável</i> (Constituição, Princípio IV).
 * Garante que alterações cadastrais posteriores no perfil do cliente (Módulo 1)
 * não alterem o endereço histórico registrado no momento do fechamento do pedido.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Embeddable
public class EnderecoEntregaSnapshot {

    private String enderecoApelido;
    private String enderecoCep;
    private String enderecoLogradouro;
    private String enderecoNumero;
    private String enderecoComplemento;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoUf;

    /**
     * Construtor padrão protegido exigido pela especificação JPA.
     */
    protected EnderecoEntregaSnapshot() {
    }

    /**
     * Construtor completo e imutável para criação do snapshot de entrega.
     *
     * @param apelido Rótulo identificador do endereço (ex.: Casa, Trabalho).
     * @param cep Código de endereçamento postal (8 dígitos).
     * @param logradouro Rua, avenida ou travessa.
     * @param numero Número predial.
     * @param complemento Complemento ou ponto de referência (opcional).
     * @param bairro Bairro do destinatário.
     * @param cidade Município de entrega.
     * @param uf Unidade Federativa com 2 caracteres.
     */
    public EnderecoEntregaSnapshot(
            String apelido,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf) {
        this.enderecoApelido = apelido;
        this.enderecoCep = Objects.requireNonNull(cep, "CEP é obrigatório no snapshot de entrega.");
        this.enderecoLogradouro = Objects.requireNonNull(logradouro, "Logradouro é obrigatório no snapshot de entrega.");
        this.enderecoNumero = Objects.requireNonNull(numero, "Número é obrigatório no snapshot de entrega.");
        this.enderecoComplemento = complemento;
        this.enderecoBairro = Objects.requireNonNull(bairro, "Bairro é obrigatório no snapshot de entrega.");
        this.enderecoCidade = Objects.requireNonNull(cidade, "Cidade é obrigatória no snapshot de entrega.");
        this.enderecoUf = Objects.requireNonNull(uf, "UF é obrigatória no snapshot de entrega.");
    }

    /**
     * Retorna o apelido do endereço.
     *
     * @return Apelido descritivo.
     */
    public String getEnderecoApelido() {
        return enderecoApelido;
    }

    /**
     * Retorna o CEP de entrega.
     *
     * @return CEP sem máscara.
     */
    public String getEnderecoCep() {
        return enderecoCep;
    }

    /**
     * Retorna o logradouro.
     *
     * @return Logradouro.
     */
    public String getEnderecoLogradouro() {
        return enderecoLogradouro;
    }

    /**
     * Retorna o número.
     *
     * @return Número predial.
     */
    public String getEnderecoNumero() {
        return enderecoNumero;
    }

    /**
     * Retorna o complemento.
     *
     * @return Complemento ou nulo.
     */
    public String getEnderecoComplemento() {
        return enderecoComplemento;
    }

    /**
     * Retorna o bairro.
     *
     * @return Bairro.
     */
    public String getEnderecoBairro() {
        return enderecoBairro;
    }

    /**
     * Retorna a cidade.
     *
     * @return Município de destino.
     */
    public String getEnderecoCidade() {
        return enderecoCidade;
    }

    /**
     * Retorna a UF.
     *
     * @return Sigla do estado (2 letras).
     */
    public String getEnderecoUf() {
        return enderecoUf;
    }

    /**
     * Gera o resumo textual formatado do endereço para exibição na nota/recibo do pedido.
     *
     * @return String formatada do endereço completo.
     */
    public String getResumoFormatado() {
        return String.format("%s, %s%s - %s, %s/%s - CEP: %s",
                enderecoLogradouro,
                enderecoNumero,
                (enderecoComplemento != null && !enderecoComplemento.isBlank() ? " (" + enderecoComplemento + ")" : ""),
                enderecoBairro,
                enderecoCidade,
                enderecoUf,
                enderecoCep);
    }
}
