package br.edu.fatec.ecommerce.modulo1.integracao;

/**
 * DTO imutável de fronteira com o endereço de entrega selecionado de um cliente.
 *
 * <p>Objetivo Funcional: Entregar ao Módulo 3 todos os campos necessários para congelar o endereço
 * no snapshot do pedido, sem expor a entidade JPA {@code Endereco} do Módulo 1.</p>
 * <p>Papel no Design Pattern: Objeto de transferência da porta {@link PortaConsultaCatalogoCliente}
 * (Anti-Corruption Layer entre módulos).</p>
 *
 * @param apelido rótulo do endereço (ex.: Casa, Trabalho)
 * @param cep CEP com 8 dígitos, sem máscara
 * @param logradouro rua, avenida ou travessa
 * @param numero número predial
 * @param complemento complemento (pode ser nulo)
 * @param bairro bairro
 * @param cidade município
 * @param uf sigla da unidade federativa (2 letras)
 * @param resumoFormatado texto pronto para exibição
 */
public record VisaoEnderecoEntregaDTO(
    String apelido,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String uf,
    String resumoFormatado
) {
}
