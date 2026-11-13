/**
 * Módulo de Autenticação Integrada sem Popup, Acesso Rápido Demo, Mesclagem de Carrinho,
 * Auto-cadastro e Área do Cliente (Perfil + Gestão de Múltiplos Endereços com Null Object).
 */

import { apiDelete, apiGet, apiPost, apiPut } from './api-client.js';
import {
    assinarEstado,
    atualizarEstado,
    codificarBasicAuth,
    definirAutenticacao,
    escapeHtml,
    obterEstado,
    usuarioPossuiPerfil
} from './store.js';

let dependenciasUi = {
    exibirToast: () => {},
    fecharModalAuth: () => {},
    alternarVisao: () => {},
    carregarCarrinho: async () => {},
    carregarPedidosCliente: async () => {}
};

/**
 * Executa autenticação na API, salva sessão Basic no sessionStorage e dispara mesclagem de carrinho se CLIENTE.
 * @param {string} email E-mail cadastrado
 * @param {string} senha Senha em texto plano
 * @param {HTMLElement|null} [bannerErro] Elemento para exibir eventual erro no modal
 */
async function executarLogin(email, senha, bannerErro = null) {
    if (bannerErro) bannerErro.hidden = true;
    const emailLimpo = String(email || '').trim().toLowerCase();

    try {
        const resp = await apiPost(
            '/api/v1/auth/login',
            { email: emailLimpo, senha },
            { ignorarAuth: true, suprimirHandler401: true }
        );

        const tokenBasic = codificarBasicAuth(emailLimpo, senha);
        const perfis = Array.isArray(resp.perfis) ? resp.perfis : [];

        definirAutenticacao({
            usuarioId: resp.usuarioId,
            email: resp.email || emailLimpo,
            perfis,
            tokenBasic
        });

        dependenciasUi.fecharModalAuth();
        dependenciasUi.exibirToast(`Bem-vindo(a), ${resp.email || emailLimpo}!`, 'success');

        if (perfis.includes('CLIENTE')) {
            await mesclarSessaoVisitanteComCliente();
            await carregarPerfilCliente();
            await dependenciasUi.carregarPedidosCliente();
        } else {
            await dependenciasUi.carregarCarrinho();
            if (perfis.some((p) => ['ADMIN', 'OPERADOR_COMERCIAL', 'RESPONSAVEL_ESTOQUE', 'RESPONSAVEL_PROMOCOES'].includes(p))) {
                await dependenciasUi.alternarVisao('admin');
            }
        }
    } catch (erro) {
        const msg = erro?.mensagem || 'E-mail ou senha inválidos.';
        if (bannerErro) {
            bannerErro.textContent = msg;
            bannerErro.hidden = false;
        } else {
            dependenciasUi.exibirToast(msg, 'error');
        }
    }
}

/**
 * Executa mesclagem aditiva do carrinho da sessão anônima no login do cliente.
 */
async function mesclarSessaoVisitanteComCliente() {
    try {
        const carrinhoConsolidado = await apiPost('/api/v1/carrinho/mesclar', undefined, {
            suprimirHandler401: true
        });
        atualizarEstado({ carrinho: carrinhoConsolidado }, 'CARRINHO');
        await dependenciasUi.carregarCarrinho();
    } catch (erro) {
        console.warn('[Auth] Falha ao mesclar carrinho visitante:', erro);
        await dependenciasUi.carregarCarrinho();
    }
}

/**
 * Realiza auto-cadastro público de novo cliente e autentica imediatamente na interface.
 * @param {Event} evento Evento de submit do formulário de cadastro
 */
async function processarCadastroCliente(evento) {
    evento.preventDefault();
    const banner = document.getElementById('cadastro-erro-banner');
    if (banner) banner.hidden = true;

    const payload = {
        nomeCompleto: document.getElementById('cad-nome')?.value?.trim() || '',
        email: document.getElementById('cad-email')?.value?.trim().toLowerCase() || '',
        cpf: (document.getElementById('cad-cpf')?.value || '').replace(/\D/g, ''),
        telefone: document.getElementById('cad-telefone')?.value?.trim() || '',
        senha: document.getElementById('cad-senha')?.value || ''
    };

    try {
        const resp = await apiPost('/api/v1/auth/clientes/cadastro', payload, {
            ignorarAuth: true,
            suprimirHandler401: true
        });

        const tokenBasic = codificarBasicAuth(payload.email, payload.senha);
        definirAutenticacao({
            usuarioId: resp.usuarioId,
            email: resp.email || payload.email,
            perfis: Array.isArray(resp.perfis) ? resp.perfis : ['CLIENTE'],
            tokenBasic
        });

        dependenciasUi.fecharModalAuth();
        dependenciasUi.exibirToast('Conta criada com sucesso! Cadastre seu endereço de entrega para continuar.', 'success');

        await mesclarSessaoVisitanteComCliente();
        await carregarPerfilCliente();
        await dependenciasUi.alternarVisao('conta');
    } catch (erro) {
        if (banner) {
            banner.textContent = erro?.mensagem || 'Não foi possível concluir o cadastro.';
            banner.hidden = false;
        }
    }
}

/**
 * Busca os dados do perfil e múltiplos endereços do cliente logado (/api/v1/clientes/me).
 * @returns {Promise<any|null>} PerfilCliente ou null
 */
export async function carregarPerfilCliente() {
    const estado = obterEstado();
    if (!estado.auth?.autenticado || !usuarioPossuiPerfil('CLIENTE')) {
        atualizarEstado({ clientePerfil: null }, 'PERFIL_CLIENTE');
        renderizarAreaConta();
        return null;
    }

    try {
        const perfil = await apiGet('/api/v1/clientes/me');
        atualizarEstado({ clientePerfil: perfil }, 'PERFIL_CLIENTE');
        renderizarAreaConta();
        return perfil;
    } catch (erro) {
        console.warn('[Auth] Não foi possível carregar /api/v1/clientes/me:', erro);
        renderizarAreaConta();
        return null;
    }
}

/**
 * Renderiza toda a Área do Cliente (#view-conta): dados cadastrais, alerta do Null Object e endereços.
 */
export function renderizarAreaConta() {
    const estado = obterEstado();
    const boxNaoLogado = document.getElementById('conta-nao-autenticada');
    const boxLogado = document.getElementById('conta-conteudo-autenticado');
    if (!boxNaoLogado || !boxLogado) return;

    let boxOperacional = document.getElementById('conta-operacional-aviso');

    if (!estado.auth?.autenticado) {
        if (boxOperacional) {
            boxOperacional.hidden = true;
        }
        boxNaoLogado.hidden = false;
        boxLogado.hidden = true;
        return;
    }

    if (!usuarioPossuiPerfil('CLIENTE')) {
        if (!boxOperacional) {
            boxOperacional = document.createElement('div');
            boxOperacional.id = 'conta-operacional-aviso';
            boxOperacional.className = boxNaoLogado.className;
            boxNaoLogado.insertAdjacentElement('afterend', boxOperacional);
        }
        boxNaoLogado.hidden = true;
        boxLogado.hidden = true;
        boxOperacional.hidden = false;
        boxOperacional.innerHTML = `
            <h2>Conta Operacional (${escapeHtml((estado.auth.perfis || []).join(', '))})</h2>
            <p class="text-muted">Você está autenticado como operador da loja (${escapeHtml(estado.auth.email)}). Endereços e pedidos de compra são exclusivos de contas com perfil CLIENTE.</p>
            <button type="button" class="btn btn-primary" data-action="ir-admin-direto">Ir para Painel Administrativo</button>
        `;
        boxOperacional.querySelector('[data-action="ir-admin-direto"]')?.addEventListener('click', () => {
            dependenciasUi.alternarVisao('admin');
        });
        return;
    }

    if (boxOperacional) {
        boxOperacional.hidden = true;
    }
    boxNaoLogado.hidden = true;
    boxLogado.hidden = false;

    const perfil = estado.clientePerfil;
    const dadosEl = document.getElementById('dados-perfil-cliente');
    const alertaAusente = document.getElementById('alerta-endereco-ausente');
    const listaEnderecosEl = document.getElementById('lista-enderecos-cliente');

    if (!perfil) {
        if (dadosEl) dadosEl.innerHTML = `<p class="text-muted">Carregando perfil...</p>`;
        return;
    }

    if (dadosEl) {
        dadosEl.innerHTML = `
            <div><strong>Nome:</strong> ${escapeHtml(perfil.nomeCompleto)}</div>
            <div><strong>E-mail:</strong> ${escapeHtml(perfil.email)}</div>
            <div><strong>CPF:</strong> <span class="mono">${escapeHtml(perfil.cpf)}</span></div>
            <div><strong>Telefone:</strong> ${escapeHtml(perfil.telefone)}</div>
            <div>
                <span class="badge ${perfil.primeiraCompraConcluida ? 'badge-neutral' : 'badge-success'}">
                    ${perfil.primeiraCompraConcluida ? 'Cliente recorrente' : 'Elegível a cupom/promoção de 1ª compra'}
                </span>
            </div>
        `;
    }

    const selecionado = perfil.enderecoSelecionado;
    if (alertaAusente) {
        if (!selecionado || !selecionado.possuiEnderecoValido) {
            alertaAusente.innerHTML = `
                <strong>Atenção:</strong> ${escapeHtml(selecionado?.resumoFormatado || 'Nenhum endereço selecionado para entrega.')}
                Cadastre ou selecione um endereço abaixo para habilitar o cálculo de frete no checkout.
            `;
            alertaAusente.hidden = false;
        } else {
            alertaAusente.hidden = true;
        }
    }

    if (listaEnderecosEl) {
        const enderecos = Array.isArray(perfil.enderecos) ? perfil.enderecos : [];
        if (enderecos.length === 0) {
            listaEnderecosEl.innerHTML = `<p class="text-muted">Você ainda não cadastrou nenhum endereço de entrega.</p>`;
        } else {
            listaEnderecosEl.innerHTML = enderecos.map((end) => `
                <article class="address-card ${end.selecionadoParaEntrega ? 'selected' : ''}">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                        <strong>${escapeHtml(end.apelido)}</strong>
                        ${end.selecionadoParaEntrega ? '<span class="badge badge-success">Selecionado para Entrega</span>' : ''}
                    </div>
                    <p>${escapeHtml(end.resumoFormatado)}</p>
                    <div class="form-actions" style="margin-top: 0.5rem;">
                        ${!end.selecionadoParaEntrega ? `
                            <button type="button" class="btn btn-outline btn-xs" data-selecionar-endereco="${escapeHtml(end.id)}">
                                Usar para Entrega
                            </button>
                        ` : ''}
                        <button type="button" class="btn btn-danger-outline btn-xs" data-excluir-endereco="${escapeHtml(end.id)}">
                            Remover
                        </button>
                    </div>
                </article>
            `).join('');
        }
    }
}

/**
 * Envia o cadastro de novo endereço de entrega do cliente autenticado.
 * @param {Event} evento Evento de submit do formulário de endereço
 */
async function salvarNovoEndereco(evento) {
    evento.preventDefault();
    const payload = {
        apelido: document.getElementById('end-apelido')?.value?.trim() || '',
        cep: (document.getElementById('end-cep')?.value || '').replace(/\D/g, ''),
        logradouro: document.getElementById('end-logradouro')?.value?.trim() || '',
        numero: document.getElementById('end-numero')?.value?.trim() || '',
        complemento: document.getElementById('end-complemento')?.value?.trim() || '',
        bairro: document.getElementById('end-bairro')?.value?.trim() || '',
        cidade: document.getElementById('end-cidade')?.value?.trim() || '',
        uf: (document.getElementById('end-uf')?.value || '').trim().toUpperCase(),
        selecionarParaEntrega: Boolean(document.getElementById('end-selecionar')?.checked)
    };

    try {
        await apiPost('/api/v1/clientes/me/enderecos', payload);
        document.getElementById('form-novo-endereco')?.reset();
        const formEl = document.getElementById('form-novo-endereco');
        if (formEl) formEl.hidden = true;
        await carregarPerfilCliente();
        dependenciasUi.exibirToast('Endereço salvo com sucesso!', 'success');
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível salvar o endereço.', 'error');
    }
}

/**
 * Conecta todos os eventos de autenticação, acesso rápido demo e área do cliente.
 * @param {typeof dependenciasUi} ui Dependências compartilhadas de main.js
 */
export async function inicializarModuloAuthCliente(ui) {
    dependenciasUi = { ...dependenciasUi, ...ui };

    document.getElementById('form-login')?.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const email = document.getElementById('login-email')?.value || '';
        const senha = document.getElementById('login-senha')?.value || '';
        const banner = document.getElementById('auth-erro-banner');
        await executarLogin(email, senha, banner);
    });

    document.getElementById('btn-demo-cliente')?.addEventListener('click', async () => {
        const banner = document.getElementById('auth-erro-banner');
        await executarLogin('cliente.demo@ecommerce.fatec.edu.br', 'ClienteFatec@2026', banner);
    });

    document.getElementById('btn-demo-admin')?.addEventListener('click', async () => {
        const banner = document.getElementById('auth-erro-banner');
        await executarLogin('admin@ecommerce.fatec.edu.br', 'AdminFatec@2026', banner);
    });

    document.getElementById('form-cadastro-cliente')?.addEventListener('submit', processarCadastroCliente);

    document.getElementById('btn-toggle-novo-endereco')?.addEventListener('click', () => {
        const formEl = document.getElementById('form-novo-endereco');
        if (formEl) formEl.hidden = !formEl.hidden;
    });

    document.getElementById('btn-cancelar-endereco')?.addEventListener('click', () => {
        const formEl = document.getElementById('form-novo-endereco');
        if (formEl) formEl.hidden = true;
    });

    document.getElementById('form-novo-endereco')?.addEventListener('submit', salvarNovoEndereco);

    document.getElementById('lista-enderecos-cliente')?.addEventListener('click', async (evento) => {
        const btnSel = evento.target.closest('[data-selecionar-endereco]');
        if (btnSel) {
            const id = btnSel.getAttribute('data-selecionar-endereco');
            try {
                const perfil = await apiPut(`/api/v1/clientes/me/enderecos/${encodeURIComponent(id)}/selecionar`);
                atualizarEstado({ clientePerfil: perfil }, 'PERFIL_CLIENTE');
                renderizarAreaConta();
                dependenciasUi.exibirToast('Endereço ativo para entrega atualizado!', 'success');
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao selecionar endereço.', 'error');
            }
            return;
        }

        const btnDel = evento.target.closest('[data-excluir-endereco]');
        if (btnDel) {
            const id = btnDel.getAttribute('data-excluir-endereco');
            try {
                await apiDelete(`/api/v1/clientes/me/enderecos/${encodeURIComponent(id)}`);
                await carregarPerfilCliente();
                dependenciasUi.exibirToast('Endereço removido.', 'info');
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao remover endereço.', 'error');
            }
        }
    });

    assinarEstado((estado, evento) => {
        if (evento === 'AUTH') {
            renderizarAreaConta();
        }
    });

    if (obterEstado().auth?.autenticado && usuarioPossuiPerfil('CLIENTE')) {
        await carregarPerfilCliente();
    } else {
        renderizarAreaConta();
    }
}
