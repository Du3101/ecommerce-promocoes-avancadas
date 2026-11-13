package br.edu.fatec.ecommerce.modulo3;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ReservaEstoqueJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.aplicacao.ServicoCarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes;
import br.edu.fatec.ecommerce.modulo3.pagamento.api.WebhookPagamentoController;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base dos testes de integração do Módulo 3: contexto Spring completo, H2, MockMvc e rollback ao final de cada teste.
 *
 * <p>Oferece atalhos para montar o cenário (cliente com endereço, carrinho) usando os serviços reais dos Módulos 1 e 2
 * e para chamar os endpoints do Módulo 3 como um cliente, um administrador ou o gateway de pagamento.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegracaoModulo3Test {

    /** Token do webhook configurado em {@code application-test.yml}. */
    protected static final String TOKEN_GATEWAY = "token-teste-gateway";

    /** E-mail do administrador padrão criado pelo Módulo 1. */
    protected static final String EMAIL_ADMIN = "admin@ecommerce.fatec.edu.br";

    /** SKU do teclado de demonstração (10 unidades, R$ 300,00). */
    protected static final String TECLADO = InicializadorDadosDemonstracaoModulo1Runner.SKU_TECLADO_DEMO;

    /** SKU do mouse de demonstração (5 unidades, R$ 150,00). */
    protected static final String MOUSE = InicializadorDadosDemonstracaoModulo1Runner.SKU_MOUSE_DEMO;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PedidoJpaRepository pedidoRepository;

    @Autowired
    protected ItemEstoqueJpaRepository estoqueRepository;

    @Autowired
    protected ReservaEstoqueJpaRepository reservaRepository;

    @Autowired
    protected PortaConsultaCarrinhoPromocoes portaCarrinho;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedCatalogo;

    @Autowired
    private ServicoIdentidadeECliente servicoIdentidade;

    @Autowired
    private ServicoCarrinhoCompras servicoCarrinho;

    /**
     * Carrega o catálogo e o estoque de demonstração dentro da transação do teste.
     */
    @BeforeEach
    void carregarCatalogoDemonstracao() {
        seedCatalogo.executarCargaDemonstracaoIdempotente();
    }

    /**
     * Cadastra um cliente com endereço de entrega selecionado em São Paulo.
     *
     * @param prefixo prefixo do e-mail
     * @return e-mail do cliente
     */
    protected String criarClienteComEndereco(String prefixo) {
        String email = criarClienteSemEndereco(prefixo);
        servicoIdentidade.adicionarEnderecoAoCliente(email, "Casa", "01310100", "Avenida Paulista", "1000",
                "Apto 12", "Bela Vista", "São Paulo", "SP", true);
        return email;
    }

    /**
     * Cadastra um cliente sem endereço de entrega.
     *
     * @param prefixo prefixo do e-mail
     * @return e-mail do cliente
     */
    protected String criarClienteSemEndereco(String prefixo) {
        String email = prefixo + "." + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        servicoIdentidade.cadastrarClientePublico("Cliente " + prefixo, email, gerarCpfValido(), "11988887777",
                "SenhaForte@2026");
        return email;
    }

    /**
     * Adiciona um item ao carrinho do cliente (Módulo 2).
     *
     * @param email e-mail do cliente
     * @param sku SKU
     * @param quantidade quantidade
     */
    protected void adicionarAoCarrinho(String email, String sku, int quantidade) {
        servicoCarrinho.adicionarItem(portaCarrinho.resolverIdentificadorCarrinhoPorEmail(email), null, sku, quantidade);
    }

    /**
     * Executa {@code POST /api/v1/checkout} como o cliente informado.
     *
     * @param email e-mail do cliente
     * @param metodo método de pagamento
     * @param modalidade modalidade de frete
     * @param chave chave de idempotência (ou {@code null})
     * @param cartao número de cartão fictício (ou {@code null})
     * @return resultado HTTP
     * @throws Exception erro do MockMvc
     */
    protected MvcResult postarCheckout(String email, String metodo, String modalidade, String chave, String cartao)
            throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("metodoPagamento", metodo);
        corpo.put("modalidadeFrete", modalidade);
        corpo.put("chaveIdempotenciaCliente", chave);
        corpo.put("numeroCartaoFicticio", cartao);
        corpo.put("titularCartao", cartao == null ? null : "CLIENTE TESTE");
        return mockMvc.perform(post("/api/v1/checkout")
                        .with(user(email).roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpo)))
                .andReturn();
    }

    /**
     * Executa o checkout e exige HTTP 201.
     *
     * @param email e-mail do cliente
     * @param metodo método de pagamento
     * @param modalidade modalidade de frete
     * @param cartao cartão fictício (ou {@code null})
     * @return JSON da resposta
     * @throws Exception erro do MockMvc ou status diferente de 201
     */
    protected JsonNode checkoutComSucesso(String email, String metodo, String modalidade, String cartao) throws Exception {
        MvcResult resultado = postarCheckout(email, metodo, modalidade, null, cartao);
        if (resultado.getResponse().getStatus() != 201) {
            throw new AssertionError("Checkout deveria retornar 201, retornou " + resultado.getResponse().getStatus()
                    + ": " + resultado.getResponse().getContentAsString());
        }
        return json(resultado);
    }

    /**
     * Envia a notificação do gateway ao webhook.
     *
     * @param numeroPedido número do pedido
     * @param sucesso aprovado ou recusado
     * @param valorPago valor pago (ou {@code null})
     * @param token token do gateway (ou {@code null})
     * @return resultado HTTP
     * @throws Exception erro do MockMvc
     */
    protected MvcResult postarWebhook(String numeroPedido, boolean sucesso, BigDecimal valorPago, String token)
            throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("chaveOperacaoPedido", numeroPedido);
        corpo.put("idTransacaoGateway", "E2E-" + numeroPedido);
        corpo.put("sucesso", sucesso);
        corpo.put("valorPago", valorPago);
        corpo.put("motivoRecusa", sucesso ? null : "Saldo insuficiente na conta do pagador");
        var requisicao = post("/api/v1/pagamentos/confirmacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo));
        if (token != null) {
            requisicao.header(WebhookPagamentoController.CABECALHO_TOKEN, token);
        }
        return mockMvc.perform(requisicao).andReturn();
    }

    /**
     * Executa uma transição administrativa de expedição.
     *
     * @param pedidoId UUID do pedido
     * @param acao {@code preparar}, {@code despachar} ou {@code entregar}
     * @return resultado HTTP
     * @throws Exception erro do MockMvc
     */
    protected MvcResult transicaoAdmin(String pedidoId, String acao) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/pedidos/" + pedidoId + "/" + acao)
                .with(user(EMAIL_ADMIN).roles("ADMIN"))).andReturn();
    }

    /**
     * Converte o corpo da resposta em JSON.
     *
     * @param resultado resultado HTTP
     * @return árvore JSON
     * @throws Exception erro de leitura
     */
    protected JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    /**
     * Saldo disponível de um SKU no Módulo 1.
     *
     * @param sku SKU
     * @return unidades disponíveis
     */
    protected int disponivel(String sku) {
        return posicao(sku).obterQuantidadeDisponivel();
    }

    /**
     * Unidades reservadas de um SKU no Módulo 1.
     *
     * @param sku SKU
     * @return unidades reservadas
     */
    protected int reservado(String sku) {
        return posicao(sku).obterQuantidadeReservada();
    }

    /**
     * Estado da reserva de estoque de um pedido no Módulo 1.
     *
     * @param numeroPedido número do pedido
     * @return estado da reserva
     */
    protected EstadoReservaEstoque estadoReserva(String numeroPedido) {
        String chave = pedidoRepository.findByNumeroPedido(numeroPedido).orElseThrow().getChaveOperacaoEstoque();
        return reservaRepository.findByChaveOperacaoPedido(chave).orElseThrow().obterEstado();
    }

    /**
     * Posição de estoque de um SKU.
     *
     * @param sku SKU
     * @return posição de estoque
     */
    private ItemEstoque posicao(String sku) {
        return estoqueRepository.findBySkuIgnoreCase(sku).orElseThrow();
    }

    /**
     * Gera um CPF válido aleatório (dígitos verificadores corretos).
     *
     * @return CPF com 11 dígitos
     */
    protected static String gerarCpfValido() {
        int[] d = new int[11];
        ThreadLocalRandom aleatorio = ThreadLocalRandom.current();
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = aleatorio.nextInt(10);
            }
        } while (d[0] == d[1] && d[1] == d[2]);
        for (int j = 9; j < 11; j++) {
            int soma = 0;
            for (int i = 0; i < j; i++) {
                soma += d[i] * ((j + 1) - i);
            }
            int resto = (soma * 10) % 11;
            d[j] = resto == 10 ? 0 : resto;
        }
        StringBuilder cpf = new StringBuilder();
        for (int digito : d) {
            cpf.append(digito);
        }
        return cpf.toString();
    }
}
