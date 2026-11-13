/**
 * Módulo de Carrinho Lateral Interativo (Drawer Slide-Over).
 * Suporta visitante anônimo persistido via header HTTP X-Sessao-Carrinho e cliente autenticado,
 * atualização de quantidades em tempo real, barra de progresso de Frete Grátis, gestão de cupons
 * e exibição transparente da memória detalhada de descontos com piso R$ 0,00.
 */

import { apiDelete, apiGet, apiPost, apiPut } from './api-client.js';
import {
    assinarEstado,
    atualizarEstado,
    escapeHtml,
    formatarMoeda,
    formatarPeso,
    obterEstado
} from './store.js';

let dependenciasUi = {
    exibirToast: () => {},
    alternarVisao: () => {}
};

/**
 * Abre o drawer lateral do carrinho e sincroniza a visão com o servidor.
 */
export async function abrirDrawerCarrinho() {
    const backdrop = document.getElementById('drawer-backdrop');
    const drawer = document.getElementById('drawer-carrinho');
    if (backdrop) backdrop.hidden = false;
    if (drawer) drawer.hidden = false;
    await carregarCarrinho();
}

/**
 * Fecha o drawer lateral do carrinho.
 */
export function fecharDrawerCarrinho() {
    const backdrop = document.getElementById('drawer-backdrop');
    const drawer = document.getElementById('drawer-carrinho');
    if (backdrop) backdrop.hidden = true;
    if (drawer) drawer.hidden = true;
}

/**
 * Consulta o carrinho atual do visitante ou cliente autenticado junto com a memória de cálculo.
 * @returns {Promise<any|null>} Objeto CarrinhoResponse ou null em caso de erro
 */
export async function carregarCarrinho() {
    const estado = obterEstado();
    const ufEntrega = estado.clientePerfil?.enderecoSelecionado?.possuiEnderecoValido
        ? estado.clientePerfil.enderecoSelecionado.uf
        : null;
    const caminho = ufEntrega
        ? `/api/v1/carrinho?ufEntrega=${encodeURIComponent(ufEntrega)}`
        : '/api/v1/carrinho';

    try {
        const carrinho = await apiGet(caminho, { suprimirHandler401: true });
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
        return carrinho;
    } catch (erro) {
        console.warn('[Carrinho] Falha ao consultar carrinho:', erro);
        return null;
    }
}

/**
 * Adiciona um SKU (produto simples ou Combo/Kit) ao carrinho e abre o drawer lateral.
 * @param {string} sku Código SKU selecionado
 * @param {number} [quantidade] Quantidade a adicionar (padrão 1)
 */
export async function adicionarItemAoCarrinho(sku, quantidade = 1) {
    try {
        const carrinho = await apiPost('/api/v1/carrinho/itens', { sku, quantidade });
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
        await abrirDrawerCarrinho();
        dependenciasUi.exibirToast(`Item ${sku} adicionado ao carrinho!`, 'success');
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || `Não foi possível adicionar ${sku}.`, 'error');
    }
}

/**
 * Altera a quantidade exata de um SKU no carrinho (remove caso quantidade <= 0).
 * @param {string} sku Código SKU
 * @param {number} novaQuantidade Nova quantidade desejada
 */
async function alterarQuantidadeItem(sku, novaQuantidade) {
    try {
        if (novaQuantidade <= 0) {
            await removerItemDoCarrinho(sku);
            return;
        }
        const carrinho = await apiPut(`/api/v1/carrinho/itens/${encodeURIComponent(sku)}`, {
            quantidade: novaQuantidade
        });
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível atualizar a quantidade.', 'error');
    }
}

/**
 * Remove completamente um SKU do carrinho atual.
 * @param {string} sku Código SKU a remover
 */
async function removerItemDoCarrinho(sku) {
    try {
        const carrinho = await apiDelete(`/api/v1/carrinho/itens/${encodeURIComponent(sku)}`);
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
        dependenciasUi.exibirToast(`Item ${sku} removido.`, 'info');
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao remover item.', 'error');
    }
}

/**
 * Esvazia todos os itens e cupons do carrinho atual.
 */
async function esvaziarCarrinhoAtual() {
    try {
        await apiDelete('/api/v1/carrinho');
        await carregarCarrinho();
        dependenciasUi.exibirToast('Carrinho esvaziado.', 'info');
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível esvaziar o carrinho.', 'error');
    }
}

/**
 * Aplica um cupom promocional e mostra retorno imediato em caso de rejeição (valor mínimo, expiração, etc.).
 * @param {string} codigoCupom Código digitado pelo cliente
 */
async function aplicarCupomNoCarrinho(codigoCupom) {
    const feedbackEl = document.getElementById('drawer-cupom-feedback');
    if (feedbackEl) feedbackEl.hidden = true;

    const codigoNormalizado = String(codigoCupom || '').trim().toUpperCase();
    if (!codigoNormalizado) {
        exibirFeedbackCupom('Informe o código do cupom antes de aplicar.', 'error');
        return;
    }

    try {
        const carrinho = await apiPost('/api/v1/carrinho/cupom', { codigoCupom: codigoNormalizado });
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
        exibirFeedbackCupom(`Cupom ${codigoNormalizado} aplicado com sucesso!`, 'success');
    } catch (erro) {
        exibirFeedbackCupom(erro?.mensagem || 'Cupom inválido ou não elegível.', 'error');
    }
}

/**
 * Remove o cupom atualmente ativo no carrinho.
 */
async function removerCupomDoCarrinho() {
    try {
        const carrinho = await apiDelete('/api/v1/carrinho/cupom');
        atualizarEstado({ carrinho }, 'CARRINHO');
        renderizarDrawerCarrinho();
        exibirFeedbackCupom('Cupom removido do carrinho.', 'info');
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao remover cupom.', 'error');
    }
}

/**
 * Mostra mensagem contextual logo abaixo do formulário de cupom dentro do drawer.
 * @param {string} mensagem Texto do aviso
 * @param {'success'|'error'|'info'} tipo Estilo semântico
 */
function exibirFeedbackCupom(mensagem, tipo) {
    const el = document.getElementById('drawer-cupom-feedback');
    if (!el) return;
    const classe = tipo === 'success' ? 'alert-success' : tipo === 'error' ? 'alert-danger' : 'alert-info';
    el.className = `coupon-feedback alert ${classe}`;
    el.textContent = mensagem;
    el.hidden = false;
}

/**
 * Atualiza a barra de elegibilidade ao Frete Grátis PAC conforme valor líquido e limite de peso.
 * @param {any} carrinho Resposta atual do carrinho
 * @param {any} config ConfiguracaoLojaPublicaDTO
 */
function atualizarProgressoFreteGratis(carrinho, config) {
    const el = document.getElementById('drawer-frete-progresso');
    if (!el) return;

    const itens = carrinho?.itens || [];
    if (itens.length === 0) {
        el.hidden = true;
        return;
    }

    const totalLiquido = Number(carrinho?.memoriaCalculo?.totalLiquido ?? 0);
    const pesoGramas = Number(carrinho?.pesoTotalGramas ?? 0);
    const valorMin = Number(config?.freteGratisValorMinimo ?? 250);
    const pesoMax = Number(config?.freteGratisPesoMaximoGramas ?? 5000);

    el.hidden = false;
    if (pesoGramas > pesoMax) {
        el.classList.remove('achieved');
        el.innerHTML = `Peso total (${escapeHtml(formatarPeso(pesoGramas))}) excede o limite de <strong>${escapeHtml(formatarPeso(pesoMax))}</strong> para Frete Grátis PAC.`;
        return;
    }

    if (totalLiquido >= valorMin) {
        el.classList.add('achieved');
        el.innerHTML = `Frete Grátis no <strong>Correios PAC</strong> liberado! (Pedido acima de ${escapeHtml(formatarMoeda(valorMin))} e até ${escapeHtml(formatarPeso(pesoMax))})`;
        return;
    }

    el.classList.remove('achieved');
    const restante = Math.max(0, valorMin - totalLiquido);
    el.innerHTML = `Faltam <strong>${escapeHtml(formatarMoeda(restante))}</strong> para ganhar <strong>Frete Grátis PAC</strong>.`;
}

/**
 * Renderiza toda a interface do drawer do carrinho e atualiza o contador do cabeçalho.
 */
export function renderizarDrawerCarrinho() {
    const estado = obterEstado();
    const carrinho = estado.carrinho;
    const qtdTotal = Number(carrinho?.quantidadeTotalItens ?? 0);

    const badge = document.getElementById('badge-qtd-carrinho');
    if (badge) {
        badge.textContent = String(qtdTotal);
    }

    const sessaoLabel = document.getElementById('drawer-sessao-label');
    if (sessaoLabel) {
        if (estado.auth?.autenticado) {
            sessaoLabel.textContent = `Cliente: ${estado.auth.email}`;
        } else {
            const curto = String(estado.sessaoCarrinho || '').slice(0, 8);
            sessaoLabel.textContent = `Visitante anônimo (${curto}...)`;
        }
    }

    atualizarProgressoFreteGratis(carrinho, estado.configuracaoLoja);

    const itensContainer = document.getElementById('drawer-carrinho-itens');
    const memoriaEl = document.getElementById('drawer-memoria-calculo');
    const btnEsvaziar = document.getElementById('btn-esvaziar-carrinho');
    const btnCheckout = document.getElementById('btn-ir-checkout');
    const inputCupom = document.getElementById('input-cupom-codigo');
    const btnRemoverCupom = document.getElementById('btn-remover-cupom');

    const itens = carrinho?.itens || [];
    const temItens = itens.length > 0;
    if (btnEsvaziar) btnEsvaziar.disabled = !temItens;
    if (btnCheckout) btnCheckout.disabled = !temItens;

    if (inputCupom && btnRemoverCupom) {
        const cupomAtual = carrinho?.cupomCodigo || '';
        inputCupom.value = cupomAtual;
        btnRemoverCupom.hidden = !Boolean(cupomAtual);
    }

    if (itensContainer) {
        if (!temItens) {
            itensContainer.innerHTML = `
                <div class="empty-state-card">
                    <p class="text-muted">Seu carrinho está vazio. Adicione produtos ou combos da vitrine!</p>
                </div>
            `;
        } else {
            itensContainer.innerHTML = itens.map((item) => `
                <article class="cart-item-row">
                    <div class="cart-item-header">
                        <div>
                            <strong>${escapeHtml(item.nome)}</strong>
                            <div class="text-muted mono" style="font-size: 0.75rem;">
                                ${escapeHtml(item.sku)} • ${escapeHtml(item.tipoItem === 'COMBO_KIT' ? 'Combo/Kit' : 'Produto')} • ${escapeHtml(formatarPeso(item.pesoGramas))}
                            </div>
                        </div>
                        <button
                            type="button"
                            class="btn btn-ghost btn-xs"
                            data-rem-sku="${escapeHtml(item.sku)}"
                            aria-label="Remover ${escapeHtml(item.nome)}"
                        >
                            Remover
                        </button>
                    </div>
                    <div class="cart-item-header" style="align-items: center;">
                        <div class="qty-controls">
                            <button type="button" data-qty-sku="${escapeHtml(item.sku)}" data-qty-novo="${item.quantidade - 1}">-</button>
                            <span>${escapeHtml(item.quantidade)}</span>
                            <button type="button" data-qty-sku="${escapeHtml(item.sku)}" data-qty-novo="${item.quantidade + 1}">+</button>
                        </div>
                        <div style="text-align: right;">
                            <small class="text-muted">${escapeHtml(formatarMoeda(item.precoUnitario))} un.</small><br>
                            <strong>${escapeHtml(formatarMoeda(item.subtotal))}</strong>
                        </div>
                    </div>
                </article>
            `).join('');
        }
    }

    if (memoriaEl) {
        const mem = carrinho?.memoriaCalculo;
        if (!mem || !temItens) {
            memoriaEl.innerHTML = '';
        } else {
            const parcelas = Array.isArray(mem.descontosDiscriminados) ? mem.descontosDiscriminados : [];
            const parcelasHtml = parcelas.map((p) => `
                <div class="breakdown-line breakdown-discount">
                    <span>${escapeHtml(p.origem || 'Benefício')}: ${escapeHtml(p.nomeBeneficio)}</span>
                    <span>- ${escapeHtml(formatarMoeda(p.valorAbatido))}</span>
                </div>
            `).join('');

            const notaPisoZero = Number(mem.subtotalBruto) > 0 && Number(mem.totalLiquido) === 0
                ? `<div class="text-muted" style="font-size: 0.76rem; margin-top: 0.25rem;">Piso mínimo de R$ 0,00 aplicado no cálculo líquido.</div>`
                : '';

            memoriaEl.innerHTML = `
                <div class="breakdown-line">
                    <span>Subtotal Bruto (${escapeHtml(qtdTotal)} itens • ${escapeHtml(formatarPeso(carrinho.pesoTotalGramas))})</span>
                    <span>${escapeHtml(formatarMoeda(mem.subtotalBruto))}</span>
                </div>
                ${parcelasHtml}
                <div class="breakdown-line">
                    <span>Total de Descontos</span>
                    <span>- ${escapeHtml(formatarMoeda(mem.totalDescontos))}</span>
                </div>
                <div class="breakdown-line breakdown-total">
                    <span>Total Líquido (sem frete)</span>
                    <span>${escapeHtml(formatarMoeda(mem.totalLiquido))}</span>
                </div>
                ${notaPisoZero}
            `;
        }
    }
}

/**
 * Registra listeners de UI do drawer do carrinho e realiza carga inicial em segundo plano.
 * @param {{exibirToast: Function, alternarVisao: Function}} ui Helpers compartilhados de main.js
 */
export async function inicializarModuloCarrinho(ui) {
    dependenciasUi = { ...dependenciasUi, ...ui };

    document.getElementById('btn-abrir-carrinho')?.addEventListener('click', abrirDrawerCarrinho);
    document.getElementById('btn-fechar-carrinho')?.addEventListener('click', fecharDrawerCarrinho);
    document.getElementById('drawer-backdrop')?.addEventListener('click', fecharDrawerCarrinho);

    document.getElementById('btn-esvaziar-carrinho')?.addEventListener('click', esvaziarCarrinhoAtual);
    document.getElementById('btn-ir-checkout')?.addEventListener('click', () => {
        fecharDrawerCarrinho();
        dependenciasUi.alternarVisao('checkout');
    });

    document.getElementById('form-cupom-carrinho')?.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const codigo = document.getElementById('input-cupom-codigo')?.value || '';
        await aplicarCupomNoCarrinho(codigo);
    });

    document.getElementById('btn-remover-cupom')?.addEventListener('click', removerCupomDoCarrinho);

    document.getElementById('drawer-carrinho-itens')?.addEventListener('click', async (evento) => {
        const btnRemover = evento.target.closest('[data-rem-sku]');
        if (btnRemover) {
            const sku = btnRemover.getAttribute('data-rem-sku');
            if (sku) await removerItemDoCarrinho(sku);
            return;
        }

        const btnQtd = evento.target.closest('[data-qty-sku]');
        if (btnQtd) {
            const sku = btnQtd.getAttribute('data-qty-sku');
            const novaQtd = Number(btnQtd.getAttribute('data-qty-novo'));
            if (sku && Number.isFinite(novaQtd)) {
                await alterarQuantidadeItem(sku, novaQtd);
            }
        }
    });

    assinarEstado((estado, evento) => {
        if (evento === 'AUTH' || evento === 'CONFIGURACAO_LOJA') {
            if (evento === 'AUTH') {
                carregarCarrinho();
            } else {
                renderizarDrawerCarrinho();
            }
        }
    });

    await carregarCarrinho();
}
