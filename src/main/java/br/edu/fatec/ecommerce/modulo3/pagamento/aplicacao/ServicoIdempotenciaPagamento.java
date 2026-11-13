package br.edu.fatec.ecommerce.modulo3.pagamento.aplicacao;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.RegistroIdempotenciaPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.RegistroIdempotenciaJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Registro e consulta de respostas já processadas, base da idempotência estrita dos pagamentos.
 *
 * <p>Funcionamento: a primeira vez que uma operação com determinada chave termina, a resposta é serializada em
 * JSON e gravada na tabela {@code registro_idempotencia_pagamento} (chave primária = chave da operação). Nas
 * repetições, a resposta gravada é devolvida sem reprocessar nada — sem nova baixa de estoque, sem novo evento.
 *
 * <p><b>Papel Arquitetural:</b> componente de infraestrutura transacional exigido pelo Princípio IV da
 * Constituição. A chave primária também funciona como trava: duas gravações com a mesma chave são impossíveis.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
public class ServicoIdempotenciaPagamento {

    private final RegistroIdempotenciaJpaRepository repositorio;
    private final ObjectMapper objectMapper;

    /**
     * Construtor com dependências injetadas.
     *
     * @param repositorio repositório dos registros de idempotência
     * @param objectMapper serializador JSON do Spring
     */
    public ServicoIdempotenciaPagamento(RegistroIdempotenciaJpaRepository repositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.objectMapper = objectMapper;
    }

    /**
     * Busca a resposta registrada para uma chave.
     *
     * @param chave chave da operação (ex.: {@code WEBHOOK:PED-202611-AB12CD})
     * @param tipo classe da resposta
     * @param <T> tipo da resposta
     * @return resposta original, ou vazio se a operação nunca foi concluída
     */
    public <T> Optional<T> buscarResposta(String chave, Class<T> tipo) {
        return this.repositorio.findById(chave).map(registro -> desserializar(registro.getPayloadRespostaJson(), tipo));
    }

    /**
     * Registra a resposta de uma operação concluída.
     *
     * @param chave chave da operação
     * @param status status resumido da resposta (ex.: {@code PROCESSADO_SUCESSO})
     * @param resposta objeto de resposta (serializado em JSON)
     */
    public void registrar(String chave, String status, Object resposta) {
        this.repositorio.save(new RegistroIdempotenciaPagamento(chave, status, serializar(resposta)));
    }

    /**
     * Serializa a resposta em JSON.
     *
     * @param resposta objeto de resposta
     * @return JSON
     * @throws IllegalStateException se a serialização falhar
     */
    private String serializar(Object resposta) {
        try {
            return this.objectMapper.writeValueAsString(resposta);
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Falha ao serializar resposta idempotente.", erro);
        }
    }

    /**
     * Desserializa a resposta gravada.
     *
     * @param json JSON gravado
     * @param tipo classe da resposta
     * @param <T> tipo da resposta
     * @return objeto de resposta
     * @throws IllegalStateException se a desserialização falhar
     */
    private <T> T desserializar(String json, Class<T> tipo) {
        try {
            return this.objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Falha ao ler resposta idempotente registrada.", erro);
        }
    }
}
