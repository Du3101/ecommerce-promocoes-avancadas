/**
 * Módulo de Histórico de Pedidos do Cliente e Checkout Guiado (Módulo 3).
 * Inclui linha do tempo dos estados, visualização do snapshot imutável, cancelamento via Mediator,
 * cotação de frete em tempo real com regra de Frete Grátis PAC, checkout idempotente e painel
 * controlado de simulação de pagamento no modo demonstração.
 */

import { apiDelete, apiGet, apiPost } from './api-client.js';
import {
    assinarEstado,
    atualizarEstado,
    escapeHtml,
    formatarDataHora,
    formatarMoeda,
    obterEstado,
    obterMetaStatusPedido,
    usuarioPossuiPerfil
} from './store.js';

let dependenciasUi = {
    exibirToast: () => {},
    abrirModalDetalhes: () => {},
    fecharModalDetalhes: () => {},
    alternarVisao: () => {},
    carregarCarrinho: async () => {},
    carregarPerfilCliente: async () => {},
    recarregarVitrineCompleta: async () => {}
};

const ORDEM_TIMELINE_LINEAR = [
    'CRIADO',
    'AGUARDANDO_PAGAMENTO',
    'PAGO',
    'EM_PREPARACAO',
    'ENVIADO',
    'ENTREGUE'
];

const STATUS_CANCELAVEIS = new Set([
    'CRIADO',
    'AGUARDANDO_PAGAMENTO',
    'PAGO',
    'EM_PREPARACAO'
]);

/**
 * Gera a linha do tempo visual de estados do pedido respeitando transições normais e desvios de recusa/cancelamento.
 * @param {string} statusAtual StatusPedido atual
 * @returns {string} HTML da timeline
 */
export function gerarHtmlLinhaDoTempo(statusAtual) {
    if (statusAtual === 'PAGAMENTO_RECUSADO' || statusAtual === 'CANCELADO') {
        const etapasAnteriores = statusAtual === 'PAGAMENTO_RECUSADO'
            ? ['CRIADO', 'AGUARDANDO_PAGAMENTO']
            : ['CRIADO'];
        const metaFalha = obterMetaStatusPedido(statusAtual);

        return `
            <div class="order-timeline">
                ${etapasAnteriores.map((st) => `
                    <span class="timeline-step done">✓ ${escapeHtml(obterMetaStatusPedido(st).label)}</span>
                `).join('')}
                <span class="timeline-step failed">✕ ${escapeHtml(metaFalha.label)}</span>
            </div>
        `;
    }

    const idxAtual = ORDEM_TIMELINE_LINEAR.indexOf(statusAtual);
    return `
        <div class="order-timeline">
            ${ORDEM_TIMELINE_LINEAR.map((st, idx) => {
                const meta = obterMetaStatusPedido(st);
                let classe = '';
                if (idxAtual > idx || (idxAtual === ORDEM_TIMELINE_LINEAR.length - 1 && idx === idxAtual)) {
                    classe = 'done';
                } else if (idx === idxAtual) {
                    classe = 'current';
                }
                return `<span class="timeline-step ${classe}">${escapeHtml(meta.label)}</span>`;
            }).join('')}
        </div>
    `;
}

/**
 * Constrói o HTML completo do Snapshot Imutável de um pedido para exibição no modal de detalhes.
 * @param {any} pedido PedidoResponseDTO
 * @returns {string} HTML sanitizado
 */
export function montarHtmlSnapshotPedido(pedido) {
    const meta = obterMetaStatusPedido(pedido.status);
    const fin = pedido.snapshotFinanceiro || {};
    const ent = pedido.entrega || {};
    const pgto = pedido.pagamento;
    const itens = Array.isArray(pedido.itens) ? pedido.itens : [];
    const promos = Array.isArray(pedido.promocoesAplicadas) ? pedido.promocoesAplicadas : [];

    return `
        <div class="stack-md">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <div>
                    <strong class="mono">${escapeHtml(pedido.numeroPedido)}</strong>
                    <div class="text-muted" style="font-size: 0.8rem;">
                        Criado em ${escapeHtml(formatarDataHora(pedido.criadoEm))} • Atualizado em ${escapeHtml(formatarDataHora(pedido.atualizadoEm))}
                    </div>
                </div>
                <span class="badge ${meta.cssClass}">${escapeHtml(pedido.statusDescricao || meta.label)}</span>
            </div>

            ${gerarHtmlLinhaDoTempo(pedido.status)}

            ${pedido.motivoCancelamento ? `
                <div class="alert alert-danger">
                    <strong>Motivo registrado:</strong> ${escapeHtml(pedido.motivoCancelamento)}
                </div>
            ` : ''}

            <div class="card-inset stack-sm">
                <strong>Entrega Congelada (Snapshot)</strong>
                <div><strong>Modalidade:</strong> ${escapeHtml(ent.nomeModalidade || ent.modalidade)} (Prazo estimado: ${escapeHtml(ent.prazoDiasUteis ?? '-')} dias úteis)</div>
                <div><strong>Endereço:</strong> ${escapeHtml(ent.enderecoDestino)}</div>
                ${ent.codigoRastreio ? `<div><strong>Rastreamento:</strong> <span class="badge badge-info mono">${escapeHtml(ent.codigoRastreio)}</span></div>` : ''}
            </div>

            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>SKU / Item</th>
                            <th>Tipo</th>
                            <th>Qtd</th>
                            <th>Unit. Tabela</th>
                            <th>Desc. Unit.</th>
                            <th>Subtotal Líq.</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itens.map((it) => `
                            <tr>
                                <td><strong>${escapeHtml(it.nome)}</strong><br><span class="mono text-muted">${escapeHtml(it.sku)}</span></td>
                                <td>${escapeHtml(it.tipoItem)}</td>
                                <td>${escapeHtml(it.quantidade)}</td>
                                <td>${escapeHtml(formatarMoeda(it.precoUnitarioTabela))}</td>
                                <td>- ${escapeHtml(formatarMoeda(it.descontoUnitario))}</td>
                                <td><strong>${escapeHtml(formatarMoeda(it.subtotalLiquido))}</strong></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>

            <div class="price-breakdown">
                <div class="breakdown-line"><span>Subtotal Bruto</span><span>${escapeHtml(formatarMoeda(fin.subtotalBruto))}</span></div>
                <div class="breakdown-line breakdown-discount"><span>Descontos Congelados (${escapeHtml(pedido.cupomUtilizado ? `Cupom ${pedido.cupomUtilizado}` : 'Motor/Cupom')})</span><span>- ${escapeHtml(formatarMoeda(fin.totalDescontos))}</span></div>
                ${promos.length > 0 ? `<div class="breakdown-line"><span>Promoções registradas</span><span>${escapeHtml(promos.join(', '))}</span></div>` : ''}
                <div class="breakdown-line"><span>Frete (${fin.freteGratis ? 'Frete Grátis PAC' : escapeHtml(ent.nomeModalidade || '-')})</span><span>${fin.freteGratis ? 'R$ 0,00 (Grátis)' : escapeHtml(formatarMoeda(fin.valorFrete))}</span></div>
                <div class="breakdown-line breakdown-total"><span>Total Final Imutável</span><span>${escapeHtml(formatarMoeda(fin.valorTotalLiquido))}</span></div>
            </div>

            ${pgto ? `
                <div class="card-inset stack-sm">
                    <strong>Registro de Pagamento (${escapeHtml(pedido.metodoPagamento)})</strong>
                    <div class="mono" style="font-size: 0.8rem;">Transação: ${escapeHtml(pgto.identificadorTransacao)} • Status: ${escapeHtml(pgto.statusTransacao)}</div>
                    ${pgto.comprovante ? `<div class="mono" style="font-size: 0.8rem; word-break: break-all;">Comprovante/Código: ${escapeHtml(pgto.comprovante)}</div>` : ''}
                </div>
            ` : ''}
        </div>
    `;
}

/**
 * Busca histórico de pedidos do cliente autenticado (/api/v1/pedidos/me) e atualiza a lista na Área do Cliente.
 * @returns {Promise<any[]>}
 */
export async function carregarPedidosCliente() {
    const estado = obterEstado();
    if (!estado.auth?.autenticado || !usuarioPossuiPerfil('CLIENTE')) {
        atualizarEstado({ meusPedidos: [] }, 'MEUS_PEDIDOS');
        renderizarListaPedidosCliente();
        return [];
    }

    try {
        const lista = await apiGet('/api/v1/pedidos/me');
        const pedidos = Array.isArray(lista) ? lista : [];
        atualizarEstado({ meusPedidos: pedidos }, 'MEUS_PEDIDOS');
        renderizarListaPedidosCliente();
        return pedidos;
    } catch (erro) {
        console.warn('[Pedidos] Erro ao carregar histórico de pedidos:', erro);
        return [];
    }
}

/**
 * Renderiza os cards de pedidos na coluna de histórico do cliente.
 */
function renderizarListaPedidosCliente() {
    const container = document.getElementById('lista-meus-pedidos');
    if (!container) return;

    const estado = obterEstado();
    const pedidos = estado.meusPedidos || [];
    if (pedidos.length === 0) {
        container.innerHTML = `<p class="text-muted">Você ainda não realizou nenhum pedido.</p>`;
        return;
    }

    const demoAtivo = Boolean(estado.configuracaoLoja?.modoDemonstracaoAtivo);

    container.innerHTML = pedidos.map((pedido) => {
        const meta = obterMetaStatusPedido(pedido.status);
        const podeCancelar = STATUS_CANCELAVEIS.has(pedido.status);
        const podeSimularDemo = demoAtivo && pedido.status === 'AGUARDANDO_PAGAMENTO';

        return `
            <article class="order-card">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 0.5rem; flex-wrap: wrap;">
                    <div>
                        <strong class="mono">${escapeHtml(pedido.numeroPedido)}</strong>
                        <div class="text-muted" style="font-size: 0.78rem;">
                            ${escapeHtml(formatarDataHora(pedido.criadoEm))} • ${escapeHtml(pedido.metodoPagamento)} • ${escapeHtml(pedido.entrega?.nomeModalidade || '')}
                        </div>
                    </div>
                    <span class="badge ${meta.cssClass}">${escapeHtml(meta.label)}</span>
                </div>

                ${gerarHtmlLinhaDoTempo(pedido.status)}

                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.6rem; flex-wrap: wrap; gap: 0.5rem;">
                    <strong>Total: ${escapeHtml(formatarMoeda(pedido.snapshotFinanceiro?.valorTotalLiquido))}</strong>
                    <div class="form-actions">
                        <button type="button" class="btn btn-outline btn-xs" data-ver-snapshot="${escapeHtml(pedido.pedidoId)}">
                            Ver Snapshot
                        </button>
                        ${podeSimularDemo ? `
                            <button type="button" class="btn btn-primary btn-xs" data-simular-pgto-lista="${escapeHtml(pedido.pedidoId)}">
                                Simular Pagamento (Demo)
                            </button>
                        ` : ''}
                        ${podeCancelar ? `
                            <button type="button" class="btn btn-danger-outline btn-xs" data-abrir-cancelamento="${escapeHtml(pedido.pedidoId)}">
                                Cancelar Pedido
                            </button>
                        ` : ''}
                    </div>
                </div>
            </article>
        `;
    }).join('');
}

/**
 * Abre o modal de confirmação de cancelamento sem usar prompt nativo do navegador.
 * @param {string} pedidoId UUID do pedido
 */
function abrirModalCancelamentoPedido(pedidoId) {
    const html = `
        <form id="form-confirmar-cancelamento-pedido" class="stack-md">
            <p>Confirma o cancelamento deste pedido? As reservas/baixas de estoque serão revertidas e eventual pagamento aprovado entrará em estorno.</p>
            <div class="form-group">
                <label for="input-motivo-cancelamento">Motivo do cancelamento (opcional)</label>
                <input id="input-motivo-cancelamento" type="text" maxlength="160" placeholder="Ex.: Desistência da compra ou troca de método">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-danger btn-sm">Confirmar Cancelamento</button>
                <button type="button" class="btn btn-ghost btn-sm" id="btn-desistir-cancelamento">Voltar</button>
            </div>
        </form>
    `;
    dependenciasUi.abrirModalDetalhes('Cancelar Pedido', html);
    document.getElementById('btn-desistir-cancelamento')?.addEventListener('click', dependenciasUi.fecharModalDetalhes);
    document.getElementById('form-confirmar-cancelamento-pedido')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const motivo = document.getElementById('input-motivo-cancelamento')?.value?.trim() || 'Cancelamento solicitado pelo cliente na Área do Cliente';
        try {
            const resp = await apiDelete(`/api/v1/pedidos/${encodeURIComponent(pedidoId)}/cancelamento?motivo=${encodeURIComponent(motivo)}`);
            dependenciasUi.fecharModalDetalhes();
            dependenciasUi.exibirToast(resp?.mensagem || 'Pedido cancelado com sucesso.', 'info');
            await Promise.all([
                carregarPedidosCliente(),
                dependenciasUi.recarregarVitrineCompleta()
            ]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível cancelar o pedido.', 'error');
        }
    });
}

/**
 * Cotação em tempo real de frete via POST /api/v1/frete/calcular para o endereço ativo e carrinho atual.
 */
export async function calcularFreteCheckout() {
    const estado = obterEstado();
    const opcoesEl = document.getElementById('checkout-frete-opcoes');
    const endereco = estado.clientePerfil?.enderecoSelecionado;
    const carrinho = estado.carrinho;

    if (!endereco || !endereco.possuiEnderecoValido) {
        atualizarEstado({ freteCotacaoAtual: null }, 'CHECKOUT_FRETE');
        if (opcoesEl) {
            opcoesEl.innerHTML = `<p class="text-muted">Selecione um endereço de entrega válido primeiro para calcular o frete.</p>`;
        }
        renderizarResumoCheckout();
        return;
    }

    if (!carrinho || !Array.isArray(carrinho.itens) || carrinho.itens.length === 0) {
        atualizarEstado({ freteCotacaoAtual: null }, 'CHECKOUT_FRETE');
        if (opcoesEl) {
            opcoesEl.innerHTML = `<p class="text-muted">Adicione ao menos um item ao carrinho para calcular o frete.</p>`;
        }
        renderizarResumoCheckout();
        return;
    }

    try {
        const cotacao = await apiPost('/api/v1/frete/calcular', {
            cepDestino: endereco.cep,
            pesoTotalGramas: Math.max(1, Number(carrinho.pesoTotalGramas || 1)),
            subtotalLiquido: Number(carrinho.memoriaCalculo?.totalLiquido ?? 0)
        }, { ignorarAuth: true, suprimirHandler401: true });

        atualizarEstado({ freteCotacaoAtual: cotacao }, 'CHECKOUT_FRETE');
        renderizarOpcoesFrete(cotacao);
        renderizarResumoCheckout();
    } catch (erro) {
        if (opcoesEl) {
            opcoesEl.innerHTML = `<div class="alert alert-danger">${escapeHtml(erro?.mensagem || 'Erro ao cotar frete.')}</div>`;
        }
    }
}

/**
 * Renderiza as opções de frete cotadas (PAC, SEDEX, Expressa) destacando o selo de Frete Grátis.
 * @param {any} cotacao CalculoFreteResponseDTO
 */
function renderizarOpcoesFrete(cotacao) {
    const opcoesEl = document.getElementById('checkout-frete-opcoes');
    if (!opcoesEl) return;

    const estado = obterEstado();
    const opcoes = Array.isArray(cotacao?.opcoes) ? cotacao.opcoes : [];
    if (opcoes.length === 0) {
        opcoesEl.innerHTML = `<p class="text-muted">Nenhuma modalidade disponível para este CEP.</p>`;
        return;
    }

    const selecionadaValida = opcoes.some((o) => o.modalidade === estado.modalidadeFreteSelecionada)
        ? estado.modalidadeFreteSelecionada
        : opcoes[0].modalidade;
    atualizarEstado({ modalidadeFreteSelecionada: selecionadaValida }, 'FRETE_ESCOLHA_SILENT');

    opcoesEl.innerHTML = `
        <div class="text-muted" style="font-size: 0.82rem; margin-bottom: 0.5rem;">
            Destino: CEP <span class="mono">${escapeHtml(cotacao.cepDestino)}</span> (${escapeHtml(cotacao.regiao)})
        </div>
        ${opcoes.map((op) => {
            const checked = op.modalidade === selecionadaValida ? 'checked' : '';
            const gratisBadge = op.freteGratisAplicado
                ? `<span class="badge badge-success">FRETE GRÁTIS</span>`
                : '';
            const precoHtml = op.freteGratisAplicado
                ? `<span class="text-muted" style="text-decoration: line-through; margin-right: 0.4rem;">${escapeHtml(formatarMoeda(op.valorOriginal))}</span><strong>R$ 0,00</strong>`
                : `<strong>${escapeHtml(formatarMoeda(op.valorCobrado))}</strong>`;

            return `
                <label class="freight-option">
                    <input type="radio" name="modalidadeFrete" value="${escapeHtml(op.modalidade)}" ${checked}>
                    <div style="flex: 1; display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                        <div>
                            <strong>${escapeHtml(op.nome)}</strong> ${gratisBadge}
                            <div class="text-muted" style="font-size: 0.78rem;">
                                ${escapeHtml(op.transportadora)} • Prazo: ${escapeHtml(op.prazoDiasUteis)} dias úteis
                            </div>
                        </div>
                        <div>${precoHtml}</div>
                    </div>
                </label>
            `;
        }).join('')}
    `;
}

/**
 * Atualiza todos os painéis da tela de Checkout (#view-checkout).
 */
export async function carregarVisaoCheckout() {
    const estado = obterEstado();
    const boxNaoLogado = document.getElementById('checkout-nao-autenticada')
        || document.getElementById('checkout-nao-autenticado');
    const boxConteudo = document.getElementById('checkout-conteudo');
    if (!boxNaoLogado || !boxConteudo) return;

    if (!estado.auth?.autenticado || !usuarioPossuiPerfil('CLIENTE')) {
        limparConfirmacaoCheckout();
        boxNaoLogado.hidden = false;
        boxConteudo.hidden = true;
        return;
    }

    boxNaoLogado.hidden = true;
    boxConteudo.hidden = false;

    await Promise.all([
        dependenciasUi.carregarPerfilCliente(),
        dependenciasUi.carregarCarrinho()
    ]);

    renderizarBlocoEnderecoCheckout();
    await calcularFreteCheckout();
}

/**
 * Renderiza o endereço selecionado ou tratamento amigável do Null Object EnderecoAusente no passo 1 do checkout.
 */
function renderizarBlocoEnderecoCheckout() {
    const box = document.getElementById('checkout-endereco-box');
    if (!box) return;
    const selecionado = obterEstado().clientePerfil?.enderecoSelecionado;

    if (!selecionado || !selecionado.possuiEnderecoValido) {
        box.innerHTML = `
            <div class="alert alert-warning">
                <strong>Nenhum endereço selecionado:</strong> ${escapeHtml(selecionado?.resumoFormatado || 'Você precisa cadastrar e selecionar um endereço antes de fechar a compra.')}
                <div style="margin-top: 0.5rem;">
                    <button type="button" class="btn btn-primary btn-xs" id="btn-ir-cadastrar-endereco">
                        Cadastrar Endereço na Minha Conta
                    </button>
                </div>
            </div>
        `;
        document.getElementById('btn-ir-cadastrar-endereco')?.addEventListener('click', () => {
            dependenciasUi.alternarVisao('conta');
        });
        return;
    }

    box.innerHTML = `
        <div class="address-card selected">
            <strong>Entrega para CEP ${escapeHtml(selecionado.cep)} (${escapeHtml(selecionado.uf)})</strong>
            <p>${escapeHtml(selecionado.resumoFormatado)}</p>
        </div>
    `;
}

/**
 * Renderiza o resumo lateral de itens e totais (com frete escolhido) no checkout.
 */
function renderizarResumoCheckout() {
    const itensEl = document.getElementById('checkout-resumo-itens');
    const totaisEl = document.getElementById('checkout-resumo-totais');
    const btnConfirmar = document.getElementById('btn-confirmar-checkout');
    const estado = obterEstado();

    const carrinho = estado.carrinho;
    const itens = carrinho?.itens || [];
    const enderecoOk = Boolean(estado.clientePerfil?.enderecoSelecionado?.possuiEnderecoValido);

    if (itensEl) {
        itensEl.innerHTML = itens.length === 0
            ? `<p class="text-muted">Carrinho vazio.</p>`
            : itens.map((it) => `
                <div class="breakdown-line">
                    <span>${escapeHtml(it.quantidade)}x ${escapeHtml(it.nome)}</span>
                    <span>${escapeHtml(formatarMoeda(it.subtotal))}</span>
                </div>
            `).join('');
    }

    const mem = carrinho?.memoriaCalculo || {};
    const opcoes = estado.freteCotacaoAtual?.opcoes || [];
    const freteAtual = opcoes.find((o) => o.modalidade === estado.modalidadeFreteSelecionada) || opcoes[0];
    const valorFrete = freteAtual ? Number(freteAtual.valorCobrado ?? 0) : 0;
    const totalProdutosLiq = Number(mem.totalLiquido ?? 0);
    const totalComFrete = Math.max(0, totalProdutosLiq + valorFrete);

    if (totaisEl) {
        totaisEl.innerHTML = `
            <div class="breakdown-line"><span>Subtotal Bruto</span><span>${escapeHtml(formatarMoeda(mem.subtotalBruto))}</span></div>
            <div class="breakdown-line breakdown-discount"><span>Promoções e Cupom${carrinho?.cupomCodigo ? ` (${escapeHtml(carrinho.cupomCodigo)})` : ''}</span><span>- ${escapeHtml(formatarMoeda(mem.totalDescontos))}</span></div>
            <div class="breakdown-line">
                <span>Frete (${escapeHtml(freteAtual?.nome || 'Selecione')})</span>
                <span>${freteAtual?.freteGratisAplicado ? '<strong class="badge badge-success">GRÁTIS</strong>' : escapeHtml(formatarMoeda(valorFrete))}</span>
            </div>
            <div class="breakdown-line breakdown-total"><span>Total Final Estimado</span><span>${escapeHtml(formatarMoeda(totalComFrete))}</span></div>
        `;
    }

    if (btnConfirmar) {
        btnConfirmar.disabled = itens.length === 0 || !enderecoOk || !freteAtual;
    }
}

/**
/**
 * Limpa a referência do último pedido pós-checkout e oculta cartões residuais de confirmação ou erro.
 * @param {boolean} [atualizarStore=true] se deve zerar {@code ultimoPedidoCheckout} no estado global
 */
function limparConfirmacaoCheckout(atualizarStore = true) {
    const resultadoBox = document.getElementById('checkout-confirmacao-resultado');
    if (resultadoBox) {
        resultadoBox.hidden = true;
        resultadoBox.innerHTML = '';
    }
    const erroBox = document.getElementById('checkout-erro-box');
    if (erroBox) {
        erroBox.hidden = true;
        erroBox.textContent = '';
    }
    if (atualizarStore && obterEstado().ultimoPedidoCheckout !== null) {
        atualizarEstado({ ultimoPedidoCheckout: null }, 'CHECKOUT_RESET');
    }
}

/**
 * Submete o fechamento de compra (POST /api/v1/checkout) e renderiza a tela de confirmação pós-checkout.
 */
async function confirmarCheckoutPedido() {
    const erroBox = document.getElementById('checkout-erro-box');
    const resultadoBox = document.getElementById('checkout-confirmacao-resultado');
    if (erroBox) erroBox.hidden = true;
    if (resultadoBox) {
        resultadoBox.hidden = true;
        resultadoBox.innerHTML = '';
    }

    const metodoPagamento = document.querySelector('input[name="metodoPagamento"]:checked')?.value || 'PIX';
    const estado = obterEstado();
    const modalidadeFrete = estado.modalidadeFreteSelecionada || 'CORREIOS_PAC';
    const chaveIdemp = document.getElementById('checkout-chave-idempotencia')?.value?.trim() || null;

    const payload = {
        metodoPagamento,
        modalidadeFrete,
        chaveIdempotenciaCliente: chaveIdemp,
        numeroCartaoFicticio: metodoPagamento === 'CARTAO_CREDITO'
            ? (document.getElementById('checkout-numero-cartao')?.value?.trim() || null)
            : null,
        titularCartao: metodoPagamento === 'CARTAO_CREDITO'
            ? (document.getElementById('checkout-titular-cartao')?.value?.trim() || null)
            : null
    };

    try {
        const resp = await apiPost('/api/v1/checkout', payload);
        atualizarEstado({ ultimoPedidoCheckout: resp }, 'CHECKOUT_SUCESSO');

        renderizarConfirmacaoCheckout(resp);
        if (resultadoBox) resultadoBox.hidden = false;

        await Promise.all([
            dependenciasUi.carregarCarrinho(),
            carregarPedidosCliente(),
            dependenciasUi.recarregarVitrineCompleta()
        ]);
        dependenciasUi.exibirToast(`Pedido ${resp.numeroPedido} registrado com status ${obterMetaStatusPedido(resp.status).label}!`, 'success');
    } catch (erro) {
        if (erroBox) {
            erroBox.textContent = erro?.mensagem || 'Não foi possível concluir o checkout.';
            erroBox.hidden = false;
        }
    }
}

/**
 * Renderiza o cartão pós-checkout com dados da transação, badge de idempotência e simulação demo condicional.
 * @param {any} resp CheckoutResponseDTO
 * @param {any} [ultimaSimulacao] ConfirmacaoPagamentoResponseDTO retornada pela simulação controlada
 */
function renderizarConfirmacaoCheckout(resp, ultimaSimulacao = null) {
    const container = document.getElementById('checkout-confirmacao-resultado');
    if (!container || !resp) return;

    const statusAtual = ultimaSimulacao?.novoStatusPedido || resp.status;
    const meta = obterMetaStatusPedido(statusAtual);
    const demoAtivo = Boolean(obterEstado().configuracaoLoja?.modoDemonstracaoAtivo);
    const dadosPgto = resp.dadosPagamento || {};
    const podeSimularInicial = demoAtivo && statusAtual === 'AGUARDANDO_PAGAMENTO';
    const podeReexecutarIdempotencia = demoAtivo
        && resp.status === 'AGUARDANDO_PAGAMENTO'
        && statusAtual === 'PAGO'
        && Boolean(ultimaSimulacao);
    const exibirControlesSimulacaoDemo = podeSimularInicial || podeReexecutarIdempotencia;

    container.innerHTML = `
        <div class="stack-md">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <div>
                    <h2>Pedido <span class="mono">${escapeHtml(resp.numeroPedido)}</span></h2>
                    <small class="text-muted">Método: ${escapeHtml(resp.metodoPagamento)} • Entrega: ${escapeHtml(resp.entrega?.nomeModalidade || '')}</small>
                </div>
                <span class="badge ${meta.cssClass}">${escapeHtml(meta.label)}</span>
            </div>

            ${resp.reaproveitado ? `
                <div class="alert alert-info">
                    <strong>Idempotência de Checkout:</strong> Este pedido já havia sido criado com a chave informada e foi reaproveitado sem duplicar estoque ou cobrança.
                </div>
            ` : ''}

            ${ultimaSimulacao ? `
                <div class="alert ${ultimaSimulacao.idempotenteReexecutado ? 'alert-info' : 'alert-success'}">
                    <strong>${ultimaSimulacao.idempotenteReexecutado ? 'Reexecução Idempotente de Pagamento:' : 'Simulação Executada:'}</strong>
                    ${escapeHtml(ultimaSimulacao.mensagem)}
                    (${escapeHtml(ultimaSimulacao.statusProcessamento)} • idempotenteReexecutado=${escapeHtml(String(ultimaSimulacao.idempotenteReexecutado))})
                </div>
            ` : ''}

            ${dadosPgto.comprovante || dadosPgto.instrucoes ? `
                <div class="card-inset stack-sm">
                    <strong>Instruções de Pagamento</strong>
                    ${dadosPgto.instrucoes ? `<div>${escapeHtml(dadosPgto.instrucoes)}</div>` : ''}
                    ${dadosPgto.comprovante ? `<div class="mono" style="word-break: break-all;">Código / Comprovante: ${escapeHtml(dadosPgto.comprovante)}</div>` : ''}
                </div>
            ` : ''}

            ${gerarHtmlLinhaDoTempo(statusAtual)}

            <div class="price-breakdown">
                <div class="breakdown-line"><span>Subtotal Bruto</span><span>${escapeHtml(formatarMoeda(resp.snapshotFinanceiro?.subtotalBruto))}</span></div>
                <div class="breakdown-line breakdown-discount"><span>Descontos</span><span>- ${escapeHtml(formatarMoeda(resp.snapshotFinanceiro?.totalDescontos))}</span></div>
                <div class="breakdown-line"><span>Frete</span><span>${resp.snapshotFinanceiro?.freteGratis ? 'R$ 0,00 (Grátis PAC)' : escapeHtml(formatarMoeda(resp.snapshotFinanceiro?.valorFrete))}</span></div>
                <div class="breakdown-line breakdown-total"><span>Total do Pedido</span><span>${escapeHtml(formatarMoeda(resp.snapshotFinanceiro?.valorTotalLiquido))}</span></div>
            </div>

            ${statusAtual === 'PAGAMENTO_RECUSADO' ? `
                <div class="alert alert-danger">
                    <strong>Pagamento Recusado:</strong> A transação não foi aprovada pelo gateway e a reserva de estoque vinculada a este pedido foi liberada. Se desejar, tente um novo checkout escolhendo outra forma de pagamento.
                </div>
            ` : ''}

            ${exibirControlesSimulacaoDemo ? `
                <div class="card-inset stack-sm">
                    <strong>Simulação Controlada de Gateway (Exclusivo Modo Demonstração)</strong>
                    <p class="text-muted" style="font-size: 0.82rem;">
                        Teste a confirmação assíncrona no servidor sem expor tokens secretos do webhook. Clicar novamente após a aprovação comprova a idempotência do processamento.
                    </p>
                    <div class="form-actions">
                        <button type="button" class="btn btn-primary btn-sm" id="btn-simular-webhook-pago">
                            ${podeReexecutarIdempotencia
                                ? 'Reenviar Confirmação (Testar Idempotência)'
                                : 'Simular Confirmação de Pagamento (PAGO)'}
                        </button>
                        ${podeSimularInicial ? `
                            <button type="button" class="btn btn-danger-outline btn-sm" id="btn-simular-webhook-recusa">
                                Simular Recusa no Gateway
                            </button>
                        ` : ''}
                    </div>
                </div>
            ` : ''}
        </div>
    `;

    document.getElementById('btn-simular-webhook-pago')?.addEventListener('click', async () => {
        await executarSimulacaoPagamentoDemo(resp.pedidoId, true, resp);
    });
    document.getElementById('btn-simular-webhook-recusa')?.addEventListener('click', async () => {
        await executarSimulacaoPagamentoDemo(resp.pedidoId, false, resp);
    });
}

/**
 * Chama POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento e atualiza pedido/estoque/UI.
 * @param {string} pedidoId UUID do pedido
 * @param {boolean} sucesso True para simular aprovação; False para recusa
 * @param {any} [checkoutRespAtual] Se vier da tela de confirmação, re-renderiza com feedback de idempotência
 */
async function executarSimulacaoPagamentoDemo(pedidoId, sucesso, checkoutRespAtual = null) {
    try {
        const resultado = await apiPost(
            `/api/v1/pedidos/${encodeURIComponent(pedidoId)}/simular-confirmacao-pagamento`,
            {
                sucesso,
                motivoRecusa: sucesso ? null : 'Recusa simulada na demonstração pela loja web'
            }
        );
        if (checkoutRespAtual) {
            renderizarConfirmacaoCheckout(checkoutRespAtual, resultado);
        }
        await Promise.all([
            carregarPedidosCliente(),
            dependenciasUi.recarregarVitrineCompleta()
        ]);
        dependenciasUi.exibirToast(
            resultado.idempotenteReexecutado
                ? 'Confirmação já processada antes (idempotência validada).'
                : `Status atualizado para ${obterMetaStatusPedido(resultado.novoStatusPedido).label}!`,
            resultado.idempotenteReexecutado ? 'info' : 'success'
        );
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível simular a confirmação do pagamento.', 'error');
    }
}

/**
 * Inicializa todos os eventos de pedidos do cliente e fluxo de checkout guiado.
 * @param {typeof dependenciasUi} ui Helpers compartilhados de main.js
 */
export async function inicializarModuloCheckoutEPedidos(ui) {
    dependenciasUi = { ...dependenciasUi, ...ui };

    document.getElementById('btn-atualizar-meus-pedidos')?.addEventListener('click', carregarPedidosCliente);
    document.getElementById('btn-checkout-trocar-endereco')?.addEventListener('click', () => {
        dependenciasUi.alternarVisao('conta');
    });
    document.getElementById('btn-recalcular-frete')?.addEventListener('click', calcularFreteCheckout);
    document.getElementById('btn-confirmar-checkout')?.addEventListener('click', confirmarCheckoutPedido);

    document.querySelectorAll('input[name="metodoPagamento"]').forEach((radio) => {
        radio.addEventListener('change', (ev) => {
            const camposCartao = document.getElementById('checkout-cartao-campos');
            if (camposCartao) {
                camposCartao.hidden = ev.target.value !== 'CARTAO_CREDITO';
            }
        });
    });

    document.getElementById('btn-cartao-aprovado')?.addEventListener('click', () => {
        const num = document.getElementById('checkout-numero-cartao');
        const tit = document.getElementById('checkout-titular-cartao');
        if (num) num.value = '4111111111111111';
        if (tit) tit.value = 'CLIENTE DEMO FATEC';
    });

    document.getElementById('btn-cartao-recusado')?.addEventListener('click', () => {
        const num = document.getElementById('checkout-numero-cartao');
        const tit = document.getElementById('checkout-titular-cartao');
        if (num) num.value = '4000000000000000';
        if (tit) tit.value = 'CARTAO RECUSADO TESTE';
    });

    document.getElementById('checkout-frete-opcoes')?.addEventListener('change', (ev) => {
        const input = ev.target;
        if (input && input.name === 'modalidadeFrete') {
            atualizarEstado({ modalidadeFreteSelecionada: input.value }, 'FRETE_ESCOLHA');
            renderizarResumoCheckout();
        }
    });

    document.getElementById('lista-meus-pedidos')?.addEventListener('click', async (ev) => {
        const btnSnap = ev.target.closest('[data-ver-snapshot]');
        if (btnSnap) {
            const id = btnSnap.getAttribute('data-ver-snapshot');
            try {
                const detalhe = await apiGet(`/api/v1/pedidos/${encodeURIComponent(id)}`);
                dependenciasUi.abrirModalDetalhes(`Snapshot Imutável — ${detalhe.numeroPedido}`, montarHtmlSnapshotPedido(detalhe));
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível carregar os detalhes do pedido.', 'error');
            }
            return;
        }

        const btnSim = ev.target.closest('[data-simular-pgto-lista]');
        if (btnSim) {
            const id = btnSim.getAttribute('data-simular-pgto-lista');
            if (id) await executarSimulacaoPagamentoDemo(id, true, null);
            return;
        }

        const btnCanc = ev.target.closest('[data-abrir-cancelamento]');
        if (btnCanc) {
            const id = btnCanc.getAttribute('data-abrir-cancelamento');
            if (id) abrirModalCancelamentoPedido(id);
        }
    });

    let emailAuthAnterior = obterEstado().auth?.autenticado
        ? (obterEstado().auth?.email || null)
        : null;

    assinarEstado((estado, evento) => {
        if (evento === 'AUTH') {
            const emailAtual = estado.auth?.autenticado ? (estado.auth?.email || null) : null;
            if (!estado.auth?.autenticado || emailAtual !== emailAuthAnterior) {
                emailAuthAnterior = emailAtual;
                limparConfirmacaoCheckout(true);
                if (estado.auth?.autenticado && usuarioPossuiPerfil('CLIENTE')) {
                    carregarPedidosCliente();
                } else {
                    renderizarListaPedidosCliente();
                }
            }
        }
        if (evento === 'AUTH' || evento === 'CARRINHO' || evento === 'PERFIL_CLIENTE') {
            if (estado.visaoAtiva === 'checkout') {
                renderizarBlocoEnderecoCheckout();
                calcularFreteCheckout();
            }
        }
    });

    if (obterEstado().auth?.autenticado && usuarioPossuiPerfil('CLIENTE')) {
        await carregarPedidosCliente();
    } else {
        renderizarListaPedidosCliente();
    }
}
