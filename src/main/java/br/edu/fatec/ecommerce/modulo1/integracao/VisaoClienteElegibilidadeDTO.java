package br.edu.fatec.ecommerce.modulo1.integracao;

import java.util.UUID;

/**
 * DTO de fronteira imutável que expõe os dados de elegibilidade do cliente e sua opção de endereço
 * selecionada (real ou via {@code EnderecoAusenteNullObject}) para os Módulos 2 e 3 (FR-004, FR-013).
 *
 * @param clienteId UUID do cliente
 * @param nomeCompleto nome completo do comprador
 * @param cpf CPF numérico de 11 dígitos
 * @param primeiraCompraConcluida {@code true} se já concluiu compra anterior; {@code false} se elegível à 1ª compra
 * @param possuiEnderecoEntregaSelecionado {@code true} se possui endereço válido selecionado; {@code false} se Null Object
 * @param cepDestino CEP de destino (ou string vazia quando Null Object)
 * @param ufDestino sigla da UF de destino (ou string vazia quando Null Object)
 * @param resumoEnderecoFormatado resumo textual do endereço ou mensagem padrão do Null Object
 */
public record VisaoClienteElegibilidadeDTO(
    UUID clienteId,
    String nomeCompleto,
    String cpf,
    boolean primeiraCompraConcluida,
    boolean possuiEnderecoEntregaSelecionado,
    String cepDestino,
    String ufDestino,
    String resumoEnderecoFormatado
) {
}

