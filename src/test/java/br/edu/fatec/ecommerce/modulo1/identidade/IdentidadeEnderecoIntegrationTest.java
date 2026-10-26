package br.edu.fatec.ecommerce.modulo1.identidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.compartilhado.excecao.ConflitoDeEstadoOuDuplicidadeException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Endereco;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.EnderecoAusenteNullObject;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.EnderecoBuilder;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.OpcaoEnderecoEntrega;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Usuario;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes automatizados para a User Story 1: Identidade, Clientes, Perfis de Acesso
 * e Endereços de Entrega (padrões <b>Static Factory Method</b>, <b>Builder</b> e <b>Null Object</b>).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentidadeEnderecoIntegrationTest {

    @Autowired
    private ServicoIdentidadeECliente servicoIdentidade;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Verifica que o auto-cadastro público cria conta exclusivamente CLIENTE com hash BCrypt
     * e retorna {@link EnderecoAusenteNullObject} enquanto nenhum endereço é selecionado.
     *
     * <p>Objetivo Funcional: Validar FR-001 e FR-004 (padrões Static Factory Method, Builder e Null Object).</p>
     * <p>Papel no Design Pattern: Comprova que {@code obterEnderecoSelecionado()} nunca retorna {@code null}.</p>
     */
    @Test
    @DisplayName("Deve cadastrar cliente público com hash BCrypt e retornar EnderecoAusenteNullObject inicialmente")
    void deveCadastrarClientePublicoERetornarEnderecoNullObjectInicialmente() {
        Cliente cliente = this.servicoIdentidade.cadastrarClientePublico(
            "Maria Aparecida Silva",
            "maria.silva@exemplo.com",
            "12345678909",
            "19999998888",
            "SenhaForte@2026"
        );

        assertNotNull(cliente.obterId());
        assertFalse(cliente.jaConcluiuPrimeiraCompra());

        Usuario usuario = this.servicoIdentidade.obterUsuarioPorEmail("maria.silva@exemplo.com");
        assertNotEquals("SenhaForte@2026", usuario.obterSenhaHash());
        assertTrue(this.passwordEncoder.matches("SenhaForte@2026", usuario.obterSenhaHash()));
        assertEquals(Set.of(PerfilUsuario.CLIENTE), usuario.obterPerfis());

        OpcaoEnderecoEntrega opcaoInicial = cliente.obterEnderecoSelecionado();
        assertNotNull(opcaoInicial);
        assertSame(EnderecoAusenteNullObject.obterInstancia(), opcaoInicial);
        assertFalse(opcaoInicial.possuiEnderecoValido());
        assertEquals("", opcaoInicial.obterCepDestino());
        assertEquals("Nenhum endereço de entrega selecionado", opcaoInicial.obterResumoFormatado());
    }

    /**
     * Verifica o ciclo completo de endereços do cliente: adição via {@link EnderecoBuilder},
     * seleção de endereço ativo e retorno ao {@link EnderecoAusenteNullObject} após exclusão.
     *
     * <p>Objetivo Funcional: Validar FR-003 e FR-004.</p>
     * <p>Papel no Design Pattern: Exercita a transição entre {@link Endereco} real e <b>Null Object</b>.</p>
     */
    @Test
    @DisplayName("Deve gerenciar múltiplos endereços via Builder e voltar ao Null Object ao remover o selecionado")
    void deveGerenciarEnderecosViaBuilderEVoltarAoNullObjectAoRemover() {
        this.servicoIdentidade.cadastrarClientePublico(
            "João Carlos Souza",
            "joao.souza@exemplo.com",
            "98765432100",
            "11988887777",
            "SenhaForte@2026"
        );

        Endereco endCasa = this.servicoIdentidade.adicionarEnderecoAoCliente(
            "joao.souza@exemplo.com",
            "Casa",
            "13010-111",
            "Rua Barão de Jaguara",
            "100",
            "Apto 42",
            "Centro",
            "Campinas",
            "sp",
            false
        );

        Endereco endTrabalho = this.servicoIdentidade.adicionarEnderecoAoCliente(
            "joao.souza@exemplo.com",
            "Trabalho",
            "01310200",
            "Avenida Paulista",
            "1500",
            "Conjunto 10",
            "Bela Vista",
            "São Paulo",
            "SP",
            true
        );

        Cliente clienteComTrabalhoSelecionado = this.servicoIdentidade.obterClientePorEmailUsuario(
            "joao.souza@exemplo.com"
        );
        OpcaoEnderecoEntrega selecionadoAtual = clienteComTrabalhoSelecionado.obterEnderecoSelecionado();
        assertTrue(selecionadoAtual.possuiEnderecoValido());
        assertEquals("01310200", selecionadoAtual.obterCepDestino());
        assertEquals("SP", selecionadoAtual.obterUfDestino());

        // Seleciona o endereço de Casa
        Cliente clienteComCasaSelecionada = this.servicoIdentidade.selecionarEnderecoDoCliente(
            "joao.souza@exemplo.com",
            endCasa.obterId()
        );
        assertEquals("13010111", clienteComCasaSelecionada.obterEnderecoSelecionado().obterCepDestino());

        // Remove o endereço de Casa (que estava selecionado) -> deve voltar ao EnderecoAusenteNullObject
        this.servicoIdentidade.removerEnderecoDoCliente("joao.souza@exemplo.com", endCasa.obterId());
        Cliente clienteSemSelecionado = this.servicoIdentidade.obterClientePorEmailUsuario("joao.souza@exemplo.com");
        assertFalse(clienteSemSelecionado.obterEnderecoSelecionado().possuiEnderecoValido());
        assertSame(EnderecoAusenteNullObject.obterInstancia(), clienteSemSelecionado.obterEnderecoSelecionado());
        assertEquals(1, clienteSemSelecionado.obterEnderecos().size());
        assertEquals(endTrabalho.obterId(), clienteSemSelecionado.obterEnderecos().get(0).obterId());
    }

    /**
     * Verifica que o {@link EnderecoBuilder} rejeita CEPs ou UFs inválidas e que CPFs/e-mails
     * duplicados geram conflito de estado.
     *
     * <p>Objetivo Funcional: Garantir proteção contra dados cadastrais inconsistentes.</p>
     * <p>Papel no Design Pattern: Valida as invariantes impostas pelo padrão <b>Builder</b>.</p>
     */
    @Test
    @DisplayName("Deve rejeitar CEP inválido no EnderecoBuilder e impedir duplicidade de e-mail ou CPF")
    void deveRejeitarCepInvalidoEImpedirDuplicidade() {
        assertThrows(RegraNegocioModulo1Exception.class, () ->
            new EnderecoBuilder()
                .paraCliente(UUID.randomUUID())
                .comApelido("Invalido")
                .comCep("123")
                .comLogradouro("Rua A")
                .comNumero("10")
                .comBairro("Centro")
                .comCidade("Campinas")
                .comUf("SP")
                .construir()
        );

        this.servicoIdentidade.cadastrarClientePublico(
            "Ana Beatriz",
            "ana@exemplo.com",
            "52998224725",
            "19977776666",
            "SenhaForte@2026"
        );

        assertThrows(ConflitoDeEstadoOuDuplicidadeException.class, () ->
            this.servicoIdentidade.cadastrarClientePublico(
                "Outra Ana",
                "ana@exemplo.com",
                "11144477735",
                "19977776666",
                "SenhaForte@2026"
            )
        );
    }

    /**
     * Verifica que operadores internos são provisionados com seus perfis RBAC específicos
     * e que o Static Factory Method impede usar o método de operador para criar conta apenas CLIENTE.
     *
     * <p>Objetivo Funcional: Validar FR-002 (segregação entre clientes e operadores internos).</p>
     * <p>Papel no Design Pattern: Exercita o <b>Static Factory Method</b> {@link Usuario#criarContaOperadorInterno}.</p>
     */
    @Test
    @DisplayName("Deve provisionar operador interno via Static Factory Method e bloquear perfil exclusivo CLIENTE")
    void deveProvisionarOperadorInternoEBloquearPerfilExclusivoCliente() {
        Usuario operador = this.servicoIdentidade.provisionarOperadorInterno(
            "comercial@ecommerce.fatec.edu.br",
            "Operador@2026",
            Set.of(PerfilUsuario.OPERADOR_COMERCIAL, PerfilUsuario.RESPONSAVEL_PROMOCOES),
            "admin@ecommerce.fatec.edu.br"
        );

        assertTrue(operador.possuiPerfil(PerfilUsuario.OPERADOR_COMERCIAL));
        assertTrue(operador.possuiPerfil(PerfilUsuario.RESPONSAVEL_PROMOCOES));
        assertFalse(operador.possuiPerfil(PerfilUsuario.CLIENTE));

        assertThrows(RegraNegocioModulo1Exception.class, () ->
            this.servicoIdentidade.provisionarOperadorInterno(
                "invalido@ecommerce.fatec.edu.br",
                "Operador@2026",
                Set.of(PerfilUsuario.CLIENTE),
                "admin@ecommerce.fatec.edu.br"
            )
        );
    }
}

