/**
 * Ponto de entrada principal da aplicação web (Orquestrador / Bootstrapper).
 * Gerencia navegação entre visões, carregamento inicial de configurações públicas (/api/v1/loja/configuracao),
 * modais compartilhados, notificações Toast e ciclo de vida dos módulos de domínio.
 */

import { apiGet, registrarHandlerNaoAutorizado } from './api-client.js';
import { carregarPainelAdmin, inicializarModuloAdmin } from './admin.js';
import { carregarPerfilCliente, inicializarModuloAuthCliente } from './auth-cliente.js';
import { adicionarItemAoCarrinho, carregarCarrinho, inicializarModuloCarrinho } from './carrinho.js';
import { inicializarModuloCatalogo, recarregarVitrineCompleta, registrarAcaoAdicionarCarrinho } from './catalogo.js';
import { carregarPedidosCliente, carregarVisaoCheckout, inicializarModuloCheckoutEPedidos } from './checkout-pedidos.js';
import {
    PERFIS_ADMINISTRATIVOS,
    assinarEstado,
    atualizarEstado,
    escapeHtml,
    formatarMoeda,
    formatarPeso,
    limparAutenticacao,
    obterEstado,
    usuarioPossuiAlgumPerfil
} from './store.js';

const hooksNavegacao = new Map();

/**
 * Registra função executada sempre que o usuário navega para determinada visão.
 * @param {'vitrine'|'conta'|'checkout'|'admin'} visao Nome da visão
 * @param {() => (void|Promise<void>)} callback Função de atualização da visão
 */
export function registrarHookVisao(visao, callback) {
    if (!hooksNavegacao.has(visao)) {
        hooksNavegacao.set(visao, []);
    }
    hooksNavegacao.get(visao).push(callback);
}

/**
 * Exibe notificação flutuante temporária (Toast).
 * @param {string} mensagem Mensagem legível ao usuário
 * @param {'info'|'success'|'warning'|'error'} [tipo] Estilo visual do alerta
 * @param {number} [duracaoMs] Tempo de permanência em milissegundos
 */
export function exibirToast(mensagem, tipo = 'info', duracaoMs = 4200) {
    const container = document.getElementById('toast-container');
    if (!container) {
        return;
    }

    const classePorTipo = {
        info: '',
        success: 'toast-success',
        warning: 'toast-warning',
        error: 'toast-error'
    };

    const toast = document.createElement('div');
    toast.className = `toast ${classePorTipo[tipo] || ''}`.trim();
    toast.textContent = mensagem;
    container.appendChild(toast);

    window.setTimeout(() => {
        toast.remove();
    }, duracaoMs);
}

/**
 * Alterna a visão ativa da SPA e invoca ganchos dos módulos registrados.
 * @param {'vitrine'|'conta'|'checkout'|'admin'} visao Visão de destino
 */
export async function alternarVisao(visao) {
    const mapaSecoes = {
        vitrine: document.getElementById('view-vitrine'),
        conta: document.getElementById('view-conta'),
        checkout: document.getElementById('view-checkout'),
        admin: document.getElementById('view-admin')
    };

    if (!mapaSecoes[visao]) {
        return;
    }

    if (visao === 'admin' && !usuarioPossuiAlgumPerfil(PERFIS_ADMINISTRATIVOS)) {
        exibirToast('Acesso restrito a perfis administrativos ou operacionais.', 'warning');
        return;
    }

    for (const [nome, secao] of Object.entries(mapaSecoes)) {
        const ativa = nome === visao;
        secao.hidden = !ativa;
        secao.classList.toggle('active', ativa);
    }

    document.querySelectorAll('[data-nav]').forEach((botao) => {
        botao.classList.toggle('active', botao.getAttribute('data-nav') === visao);
    });

    atualizarEstado({ visaoAtiva: visao }, 'NAVEGACAO');

    const listaHooks = hooksNavegacao.get(visao) || [];
    for (const hook of listaHooks) {
        try {
            await hook();
        } catch (erro) {
            console.error(`[Main] Falha ao ativar hook da visão ${visao}:`, erro);
        }
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * Abre o modal genérico de detalhes (ex.: ficha técnica do produto ou snapshot imutável do pedido).
 * @param {string} titulo Título exibido no cabeçalho do modal
 * @param {string} htmlConteudo HTML previamente sanitizado com escapeHtml
 */
export function abrirModalDetalhes(titulo, htmlConteudo) {
    const modal = document.getElementById('modal-detalhes');
    const tituloEl = document.getElementById('modal-detalhes-titulo');
    const bodyEl = document.getElementById('modal-detalhes-body');
    if (!modal || !tituloEl || !bodyEl) {
        return;
    }
    tituloEl.textContent = titulo;
    bodyEl.innerHTML = htmlConteudo;
    modal.hidden = false;
}

/**
 * Fecha o modal genérico de detalhes.
 */
export function fecharModalDetalhes() {
    const modal = document.getElementById('modal-detalhes');
    if (modal) {
        modal.hidden = true;
    }
}

/**
 * Abre o modal interno de autenticação/cadastro (evitando qualquer popup nativo HTTP Basic).
 * @param {'login'|'cadastro'} [abaInicial] Aba selecionada inicialmente
 */
export function abrirModalAuth(abaInicial = 'login') {
    const modal = document.getElementById('modal-auth');
    if (!modal) {
        return;
    }
    alternarAbaModalAuth(abaInicial);
    modal.hidden = false;
}

/**
 * Fecha o modal interno de autenticação.
 */
export function fecharModalAuth() {
    const modal = document.getElementById('modal-auth');
    if (modal) {
        modal.hidden = true;
    }
}

/**
 * Alterna entre as abas 'Entrar' e 'Criar Conta de Cliente' dentro do modal de autenticação.
 * @param {'login'|'cadastro'} aba Aba alvo
 */
export function alternarAbaModalAuth(aba) {
    const tabLogin = document.getElementById('tab-btn-login');
    const tabCadastro = document.getElementById('tab-btn-cadastro');
    const paneLogin = document.getElementById('tab-pane-login');
    const paneCadastro = document.getElementById('tab-pane-cadastro');
    const bannerLogin = document.getElementById('auth-erro-banner');
    const bannerCad = document.getElementById('cadastro-erro-banner');

    const eLogin = aba !== 'cadastro';
    if (tabLogin) tabLogin.classList.toggle('active', eLogin);
    if (tabCadastro) tabCadastro.classList.toggle('active', !eLogin);
    if (paneLogin) paneLogin.hidden = !eLogin;
    if (paneCadastro) paneCadastro.hidden = eLogin;
    if (bannerLogin) bannerLogin.hidden = true;
    if (bannerCad) bannerCad.hidden = true;
}

/**
 * Atualiza elementos globais de cabeçalho e banners conforme o estado de autenticação e configuração.
 * @param {ReturnType<typeof obterEstado>} estado Estado global corrente
 */
function sincronizarCabecalhoEConfiguracao(estado) {
    const pillLogado = document.getElementById('usuario-logado-pill');
    const emailEl = document.getElementById('usuario-logado-email');
    const perfisEl = document.getElementById('usuario-logado-perfis');
    const btnEntrar = document.getElementById('btn-abrir-login');
    const btnAdmin = document.getElementById('btn-nav-admin');
    const caixaDemo = document.getElementById('secao-acesso-rapido-demo');

    const autenticado = Boolean(estado.auth?.autenticado);
    if (pillLogado) pillLogado.hidden = !autenticado;
    if (btnEntrar) btnEntrar.hidden = autenticado;

    if (autenticado && emailEl && perfisEl) {
        emailEl.textContent = estado.auth.email || '';
        perfisEl.textContent = (estado.auth.perfis || []).join(', ');
    }

    const eAdmin = usuarioPossuiAlgumPerfil(PERFIS_ADMINISTRATIVOS);
    if (btnAdmin) {
        btnAdmin.hidden = !eAdmin;
    }

    if (caixaDemo) {
        caixaDemo.hidden = !Boolean(estado.configuracaoLoja?.modoDemonstracaoAtivo);
    }

    const topbar = document.getElementById('topbar-frete');
    const topbarTexto = document.getElementById('topbar-frete-texto');
    if (topbar && topbarTexto && estado.configuracaoLoja) {
        const valorMin = formatarMoeda(estado.configuracaoLoja.freteGratisValorMinimo);
        const pesoMax = formatarPeso(estado.configuracaoLoja.freteGratisPesoMaximoGramas);
        const seloDemo = estado.configuracaoLoja.modoDemonstracaoAtivo
            ? ' • Modo Demonstração Ativo'
            : '';
        topbarTexto.innerHTML = `Frete Grátis PAC acima de <strong>${escapeHtml(valorMin)}</strong> (até ${escapeHtml(pesoMax)})${escapeHtml(seloDemo)}`;
        topbar.hidden = false;
    }

    if (estado.visaoAtiva === 'admin' && !eAdmin) {
        alternarVisao('vitrine');
    }
}

/**
 * Busca configuração pública da loja (/api/v1/loja/configuracao) para política de frete e modo demo.
 */
async function carregarConfiguracaoPublicaLoja() {
    try {
        const config = await apiGet('/api/v1/loja/configuracao', { ignorarAuth: true, suprimirHandler401: true });
        if (config) {
            atualizarEstado({ configuracaoLoja: config }, 'CONFIGURACAO_LOJA');
        }
    } catch (erro) {
        console.warn('[Main] Não foi possível carregar /api/v1/loja/configuracao:', erro);
    }
}

/**
 * Conecta eventos globais de navegação, modais, saída e handler HTTP 401 sem popup nativo.
 */
function registrarEventosGlobais() {
    registrarHandlerNaoAutorizado((erro) => {
        exibirToast(erro?.mensagem || 'Sua sessão expirou ou requer login.', 'warning');
        abrirModalAuth('login');
    });

    document.querySelectorAll('[data-nav]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const destino = btn.getAttribute('data-nav');
            if (destino) alternarVisao(destino);
        });
    });

    document.querySelectorAll('[data-action="ir-vitrine"]').forEach((btn) => {
        btn.addEventListener('click', () => alternarVisao('vitrine'));
    });

    document.getElementById('btn-abrir-login')?.addEventListener('click', () => abrirModalAuth('login'));
    document.getElementById('btn-conta-entrar')?.addEventListener('click', () => abrirModalAuth('login'));
    document.getElementById('btn-checkout-entrar')?.addEventListener('click', () => abrirModalAuth('login'));
    document.getElementById('btn-fechar-modal-auth')?.addEventListener('click', fecharModalAuth);
    document.getElementById('btn-fechar-modal-detalhes')?.addEventListener('click', fecharModalDetalhes);
    document.getElementById('tab-btn-login')?.addEventListener('click', () => alternarAbaModalAuth('login'));
    document.getElementById('tab-btn-cadastro')?.addEventListener('click', () => alternarAbaModalAuth('cadastro'));

    document.getElementById('modal-auth')?.addEventListener('click', (evento) => {
        if (evento.target?.id === 'modal-auth') fecharModalAuth();
    });
    document.getElementById('modal-detalhes')?.addEventListener('click', (evento) => {
        if (evento.target?.id === 'modal-detalhes') fecharModalDetalhes();
    });

    document.getElementById('btn-logout')?.addEventListener('click', () => {
        limparAutenticacao();
        exibirToast('Você saiu da sua conta.', 'info');
        alternarVisao('vitrine');
    });

    assinarEstado((estado) => {
        sincronizarCabecalhoEConfiguracao(estado);
    });
}

/**
 * Inicializa a aplicação SPA quando o DOM estiver pronto.
 */
async function inicializarAplicacao() {
    registrarEventosGlobais();
    sincronizarCabecalhoEConfiguracao(obterEstado());
    await carregarConfiguracaoPublicaLoja();

    registrarHookVisao('vitrine', async () => {
        await recarregarVitrineCompleta();
    });
    registrarHookVisao('conta', async () => {
        await Promise.all([
            carregarPerfilCliente(),
            carregarPedidosCliente()
        ]);
    });
    registrarHookVisao('checkout', async () => {
        await carregarVisaoCheckout();
    });
    registrarHookVisao('admin', async () => {
        await carregarPainelAdmin();
    });

    registrarAcaoAdicionarCarrinho(adicionarItemAoCarrinho);
    await inicializarModuloCatalogo({
        exibirToast,
        abrirModalDetalhes
    });
    await inicializarModuloCarrinho({
        exibirToast,
        alternarVisao
    });
    await inicializarModuloAuthCliente({
        exibirToast,
        fecharModalAuth,
        alternarVisao,
        carregarCarrinho,
        carregarPedidosCliente
    });
    await inicializarModuloCheckoutEPedidos({
        exibirToast,
        abrirModalDetalhes,
        fecharModalDetalhes,
        alternarVisao,
        carregarCarrinho,
        carregarPerfilCliente,
        recarregarVitrineCompleta
    });
    inicializarModuloAdmin({
        exibirToast,
        abrirModalDetalhes,
        recarregarVitrineCompleta
    });
}

document.addEventListener('DOMContentLoaded', () => {
    inicializarAplicacao();
});
