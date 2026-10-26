package br.edu.fatec.ecommerce.modulo1.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.compartilhado.excecao.AcessoNegadoModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.auditoria.aplicacao.ServicoConsultaAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.TipoOperacaoAuditoria;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.CatalogoAdminAuditoriaProxy;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoPort;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.EstoqueAdminSegurancaProxy;
import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoAdminEstoquePort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes automatizados para a User Story 5: Trilha de Auditoria e Proteção RBAC
 * via Padrão <b>Proxy</b> ({@link CatalogoAdminAuditoriaProxy} e {@link EstoqueAdminSegurancaProxy}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaProxyIntegrationTest {

    @Autowired
    private ServicoAdminCatalogoPort servicoAdminCatalogo;

    @Autowired
    private ServicoAdminEstoquePort servicoAdminEstoque;

    @Autowired
    private ServicoConsultaAuditoria servicoConsultaAuditoria;

    /**
     * Limpa o contexto de segurança do Spring Security após cada teste.
     *
     * <p>Objetivo Funcional: Garantir isolamento de credenciais entre os testes.</p>
     * <p>Papel no Design Pattern: Higienização de contexto consumido pelos Proxies.</p>
     */
    @AfterEach
    void limparContextoSeguranca() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Comprova que as portas injetadas pelo Spring são os Proxies {@code @Primary}, que eles
     * gravam automaticamente registros de auditoria com estado anterior/novo e permitem consulta
     * filtrada pelo perfil {@code ADMIN}.
     *
     * <p>Objetivo Funcional: Validar FR-012 e SC-005.</p>
     * <p>Papel no Design Pattern: Valida o funcionamento transparente de {@link CatalogoAdminAuditoriaProxy}
     * e {@link EstoqueAdminSegurancaProxy}.</p>
     */
    @Test
    @DisplayName("Deve interceptar operações via Proxy, gravar valor anterior/novo e consultar trilha como ADMIN")
    void deveInterceptarOperacoesViaProxyGravarAuditoriaEConsultarComoAdmin() {
        assertInstanceOf(CatalogoAdminAuditoriaProxy.class, this.servicoAdminCatalogo);
        assertInstanceOf(EstoqueAdminSegurancaProxy.class, this.servicoAdminEstoque);

        // 1. Autentica como OPERADOR_COMERCIAL e altera catálogo e preço
        autenticarNaThread("operador.comercial@fatec.edu.br", "ROLE_OPERADOR_COMERCIAL");

        CategoriaComp cat = this.servicoAdminCatalogo.criarCategoria("Áudio", "audio-pro", null);

        ProdutoBuilder builder = new ProdutoBuilder()
            .comNome("Headset Studio")
            .comDescricao("Headset profissional")
            .comMarca("FatecSound")
            .comPrecoBase(new BigDecimal("400.00"))
            .naCategoria(cat.obterId())
            .adicionarVariacao(
                "HDS-STUDIO-01",
                null,
                "Headset Studio Preto",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    350,
                    new BigDecimal("20.00"),
                    new BigDecimal("18.00"),
                    new BigDecimal("10.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogo.cadastrarProdutoComVariacoes(builder, Map.of("HDS-STUDIO-01", 10));

        // Altera preço de 400.00 para 450.00 via Proxy
        this.servicoAdminCatalogo.alterarPrecoItemCatalogo("HDS-STUDIO-01", new BigDecimal("450.00"));

        // 2. Autentica como RESPONSAVEL_ESTOQUE e ajusta saldo físico (+5 unidades)
        autenticarNaThread("gestor.estoque@fatec.edu.br", "ROLE_RESPONSAVEL_ESTOQUE");
        this.servicoAdminEstoque.ajustarSaldoDisponivelManual(
            "HDS-STUDIO-01",
            5,
            "Entrada de nota fiscal 9988"
        );

        // 3. Autentica como ADMIN e consulta a trilha de auditoria
        autenticarNaThread("admin@ecommerce.fatec.edu.br", "ROLE_ADMIN");

        List<RegistroAuditoria> alteracoesPreco = this.servicoConsultaAuditoria.consultarTrilhaAuditoria(
            null,
            TipoOperacaoAuditoria.ALTERACAO_PRECO_PRODUTO,
            null,
            null
        );
        assertTrue(alteracoesPreco.stream().anyMatch(r ->
            "HDS-STUDIO-01".equals(r.obterIdentificadorAlvo())
                && "400.00".equals(r.obterValorAnteriorResumo())
                && "450.00".equals(r.obterValorNovoResumo())
                && "operador.comercial@fatec.edu.br".equals(r.obterEmailResponsavel())
        ));

        List<RegistroAuditoria> ajustesEstoque = this.servicoConsultaAuditoria.consultarTrilhaAuditoria(
            null,
            TipoOperacaoAuditoria.AJUSTE_MANUAL_ESTOQUE,
            null,
            null
        );
        assertEquals(1, ajustesEstoque.size());
        RegistroAuditoria regEstoque = ajustesEstoque.get(0);
        assertEquals("HDS-STUDIO-01", regEstoque.obterIdentificadorAlvo());
        assertEquals("disponivel=10", regEstoque.obterValorAnteriorResumo());
        assertTrue(regEstoque.obterValorNovoResumo().contains("disponivel=15"));
        assertEquals("gestor.estoque@fatec.edu.br", regEstoque.obterEmailResponsavel());
    }

    /**
     * Comprova que os Protection Proxies bloqueiam imediatamente usuários com perfis inadequados
     * (ex.: CLIENTE tentando alterar catálogo, OPERADOR_COMERCIAL tentando ajustar estoque ou
     * consultar trilha de auditoria).
     *
     * <p>Objetivo Funcional: Validar FR-002 e FR-012 (segregação rigorosa de funções RBAC).</p>
     * <p>Papel no Design Pattern: Valida o papel de <i>Protection Proxy</i>.</p>
     */
    @Test
    @DisplayName("Deve bloquear perfis não autorizados nos Proxies de catálogo, estoque e consulta de auditoria")
    void deveBloquearPerfisNaoAutorizadosNosProxies() {
        // Cliente tentando criar categoria -> bloqueado pelo CatalogoAdminAuditoriaProxy
        autenticarNaThread("cliente@exemplo.com", "ROLE_CLIENTE");
        assertThrows(AcessoNegadoModulo1Exception.class, () ->
            this.servicoAdminCatalogo.criarCategoria("Proibida", "proibida", null)
        );

        // Operador Comercial tentando ajustar estoque -> bloqueado pelo EstoqueAdminSegurancaProxy
        autenticarNaThread("operador.comercial@fatec.edu.br", "ROLE_OPERADOR_COMERCIAL");
        assertThrows(AcessoNegadoModulo1Exception.class, () ->
            this.servicoAdminEstoque.ajustarSaldoDisponivelManual("SKU-QUALQUER", 10, "Tentativa indevida")
        );

        // Operador Comercial tentando consultar auditoria -> bloqueado (exclusivo ADMIN)
        assertThrows(AcessoNegadoModulo1Exception.class, () ->
            this.servicoConsultaAuditoria.consultarTrilhaAuditoria(null, null, null, null)
        );
    }

    /**
     * Configura um usuário autenticado simulado no {@link SecurityContextHolder} para o teste.
     *
     * <p>Objetivo Funcional: Simular diferentes perfis RBAC sem depender de transporte HTTP externo.</p>
     * <p>Papel no Design Pattern: Auxiliar de teste para exercitar os Protection &amp; Audit Proxies.</p>
     *
     * @param email e-mail do usuário autenticado
     * @param role autoridade RBAC (ex.: {@code ROLE_ADMIN})
     */
    private void autenticarNaThread(String email, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            email,
            "N/A",
            List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

