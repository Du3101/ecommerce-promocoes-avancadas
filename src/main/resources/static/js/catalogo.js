/**
 * Módulo de Vitrine Pública: banners de promoções vigentes (Módulo 2), árvore recursiva
 * de categorias Composite (Módulo 1), busca textual, agrupamento de variações com atributos
 * Flyweight, decomposição de Combos/Kits e consulta sob demanda de estoque físico/gargalo.
 */

import { apiGet } from './api-client.js';
import {
    MAPA_MECANICAS_PROMOCAO,
    atualizarEstado,
    escapeHtml,
    formatarDataHora,
    formatarMoeda,
    formatarPeso,
    obterEstado
} from './store.js';

const catalogoCompletoPorSku = new Map();
const selecaoVariacaoPorGrupo = new Map();

let callbackAdicionarCarrinho = null;
let dependenciasUi = {
    exibirToast: () => {},
    abrirModalDetalhes: () => {}
};

/**
 * Registra callback executado ao clicar em "Adicionar ao Carrinho" na vitrine ou no modal de detalhes.
 * @param {(sku: string, quantidade: number) => Promise<void>} fn Manipulador de adição ao carrinho
 */
export function registrarAcaoAdicionarCarrinho(fn) {
    callbackAdicionarCarrinho = fn;
}

/**
 * Normaliza e extrai o nome base do produto quando o SKU usa convenção "Produto - Variação".
 * @param {string} nomeExibicao Nome retornado pelo catálogo
 * @returns {{nomeBase: string, sufixoVariacao: string}}
 */
function extrairBaseESufixo(nomeExibicao) {
    const texto = String(nomeExibicao || '').trim();
    const separadores = [' — ', ' - ', ' – '];
    for (const sep of separadores) {
        const idx = texto.indexOf(sep);
        if (idx > 0) {
            return {
                nomeBase: texto.slice(0, idx).trim(),
                sufixoVariacao: texto.slice(idx + sep.length).trim()
            };
        }
    }
    return { nomeBase: texto, sufixoVariacao: texto };
}

/**
 * Agrupa itens retornados pela API em cards: Combos permanecem individuais e variações irmãs
 * com o mesmo prefixo de nome e categoria compartilham seletor dinâmico no mesmo card.
 * @param {any[]} itens Lista plana retornada por GET /api/v1/catalogo/itens
 * @returns {Array<{key: string, nomeTitulo: string, eComboKit: boolean, variacoes: any[]}>}
 */
function agruparItensParaVitrine(itens) {
    const grupos = [];
    const indiceVariacoes = new Map();

    for (const item of itens) {
        if (item.eComboKit) {
            grupos.push({
                key: `COMBO:${item.sku}`,
                nomeTitulo: item.nomeExibicao,
                eComboKit: true,
                variacoes: [item]
            });
            continue;
        }

        const { nomeBase } = extrairBaseESufixo(item.nomeExibicao);
        const chaveGrupo = `PROD:${item.categoriaPrincipalId || 'geral'}::${nomeBase.toLowerCase()}`;
        if (!indiceVariacoes.has(chaveGrupo)) {
            const grupo = {
                key: chaveGrupo,
                nomeTitulo: nomeBase,
                eComboKit: false,
                variacoes: []
            };
            indiceVariacoes.set(chaveGrupo, grupo);
            grupos.push(grupo);
        }
        indiceVariacoes.get(chaveGrupo).variacoes.push(item);
    }

    return grupos;
}

/**
 * Gera texto compacto dos atributos Flyweight de uma variação para chips e detalhes.
 * @param {any[]} atributos Lista de atributosFlyweight
 * @returns {string}
 */
function resumirAtributosFlyweight(atributos) {
    if (!Array.isArray(atributos) || atributos.length === 0) {
        return '';
    }
    return atributos.map((a) => `${a.valor}`).join(' / ');
}

/**
 * Calcula detalhamento dos componentes físicos e economia real (em R$ e %) de um ComboKit.
 * @param {any} combo Item ComboKitComposite serializado
 * @returns {{linhas: Array<{sku: string, nome: string, quantidade: number, precoUnit: number}>, precoAvulsoTotal: number, economiaReais: number}}
 */
function calcularResumoCombo(combo) {
    const mapa = combo.skusFisicosDecompostos || {};
    const linhas = [];
    let precoAvulsoTotal = 0;

    for (const [skuComp, qtd] of Object.entries(mapa)) {
        const comp = catalogoCompletoPorSku.get(skuComp);
        const quantidade = Number(qtd || 1);
        const precoUnit = comp ? Number(comp.precoVigente || 0) : 0;
        precoAvulsoTotal += precoUnit * quantidade;
        linhas.push({
            sku: skuComp,
            nome: comp ? comp.nomeExibicao : `Componente ${skuComp}`,
            quantidade,
            precoUnit
        });
    }

    const precoCombo = Number(combo.precoVigente || 0);
    const economiaReais = precoAvulsoTotal > precoCombo ? precoAvulsoTotal - precoCombo : 0;
    return { linhas, precoAvulsoTotal, economiaReais };
}

/**
 * Carrega campanhas promocionais ativas (GET /api/v1/promocoes/ativas) e renderiza os banners da loja.
 */
export async function carregarPromocoesAtivas() {
    const container = document.getElementById('lista-banners-promocoes');
    try {
        const promocoes = await apiGet('/api/v1/promocoes/ativas', { ignorarAuth: true, suprimirHandler401: true });
        atualizarEstado({ promocoesAtivas: Array.isArray(promocoes) ? promocoes : [] }, 'PROMOCOES');
        renderizarBannersPromocoes(promocoes || []);
    } catch (erro) {
        if (container) {
            container.innerHTML = `<div class="empty-state-card">Não foi possível carregar as promoções no momento.</div>`;
        }
    }
}

/**
 * Renderiza os cards de banners promocionais vigentes na vitrine.
 * @param {any[]} promocoes Lista retornada pelo endpoint público
 */
function renderizarBannersPromocoes(promocoes) {
    const container = document.getElementById('lista-banners-promocoes');
    if (!container) return;

    if (!Array.isArray(promocoes) || promocoes.length === 0) {
        container.innerHTML = `
            <div class="empty-state-card">
                <p class="text-muted">Nenhuma campanha promocional ativa no momento. Fique de olho nos combos e cupons do carrinho!</p>
            </div>
        `;
        return;
    }

    container.innerHTML = promocoes.map((promo) => {
        const meta = MAPA_MECANICAS_PROMOCAO[promo.tipoMecanica] || {
            label: promo.descricaoMecanica || 'Promoção',
            selo: 'Oferta Ativa'
        };
        const seloCumulativa = promo.cumulativa ? 'Cumulativa' : 'Exclusiva (Maior Prioridade)';
        return `
            <article class="promo-banner-card">
                <div class="promo-banner-meta">
                    <span class="promo-banner-pill">${escapeHtml(meta.selo)}</span>
                    <span class="promo-banner-pill">${escapeHtml(seloCumulativa)} • P${escapeHtml(promo.prioridade)}</span>
                </div>
                <h3>${escapeHtml(promo.nome)}</h3>
                <p>${escapeHtml(promo.descricao || promo.descricaoMecanica || meta.label)}</p>
                ${promo.dataFim ? `<small class="text-muted">Válida até ${escapeHtml(formatarDataHora(promo.dataFim))}</small>` : ''}
            </article>
        `;
    }).join('');
}

/**
 * Carrega a árvore hierárquica recursiva de categorias e renderiza a barra lateral.
 */
export async function carregarArvoreCategorias() {
    const container = document.getElementById('arvore-categorias');
    try {
        const raizes = await apiGet('/api/v1/catalogo/categorias/arvore', { ignorarAuth: true, suprimirHandler401: true });
        atualizarEstado({ categoriasArvore: Array.isArray(raizes) ? raizes : [] }, 'CATEGORIAS');
        renderizarArvoreCategorias();
    } catch {
        if (container) {
            container.innerHTML = `<p class="text-muted">Erro ao carregar árvore de categorias.</p>`;
        }
    }
}

/**
 * Constrói recursivamente o HTML de uma lista aninhada de categorias Composite.
 * @param {any[]} nos Nós de categoria
 * @param {string|null} selecionadaId ID atualmente selecionado
 * @returns {string} HTML seguro
 */
function gerarHtmlNosCategorias(nos, selecionadaId) {
    if (!Array.isArray(nos) || nos.length === 0) {
        return '';
    }
    const itensHtml = nos
        .filter((no) => no.ativa !== false)
        .map((no) => {
            const ativaClass = selecionadaId && String(no.id) === String(selecionadaId) ? 'active' : '';
            const filhosHtml = gerarHtmlNosCategorias(no.subcategorias || [], selecionadaId);
            return `
                <li>
                    <button
                        type="button"
                        class="category-btn ${ativaClass}"
                        data-categoria-id="${escapeHtml(no.id)}"
                        data-categoria-caminho="${escapeHtml(no.caminhoHierarquico || no.nome)}"
                    >
                        <span>${escapeHtml(no.nome)}</span>
                    </button>
                    ${filhosHtml}
                </li>
            `;
        })
        .join('');
    return itensHtml ? `<ul>${itensHtml}</ul>` : '';
}

/**
 * Renderiza a árvore recursiva na barra lateral e atualiza o caminho hierárquico ativo.
 */
export function renderizarArvoreCategorias() {
    const container = document.getElementById('arvore-categorias');
    const pathEl = document.getElementById('categoria-selecionada-label');
    const estado = obterEstado();
    if (!container) return;

    const arvore = estado.categoriasArvore || [];
    const html = gerarHtmlNosCategorias(arvore, estado.categoriaSelecionadaId);
    container.innerHTML = html || `<p class="text-muted">Nenhuma categoria ativa cadastrada.</p>`;

    if (pathEl) {
        pathEl.textContent = estado.categoriaSelecionadaNome || 'Todas as categorias';
    }
}

/**
 * Busca itens públicos do catálogo (com filtro recursivo opcional por categoriaId).
 * @param {string|null} [categoriaId] ID da categoria selecionada ou null para vitrine completa
 */
export async function carregarItensCatalogo(categoriaId = null) {
    const grade = document.getElementById('grade-produtos');
    const url = categoriaId
        ? `/api/v1/catalogo/itens?categoriaId=${encodeURIComponent(categoriaId)}`
        : '/api/v1/catalogo/itens';

    try {
        const itens = await apiGet(url, { ignorarAuth: true, suprimirHandler401: true });
        const lista = Array.isArray(itens) ? itens : [];

        if (!categoriaId) {
            catalogoCompletoPorSku.clear();
            for (const item of lista) {
                catalogoCompletoPorSku.set(item.sku, item);
            }
        } else {
            for (const item of lista) {
                catalogoCompletoPorSku.set(item.sku, item);
            }
        }

        atualizarEstado({ catalogoItens: lista }, 'CATALOGO');
        renderizarVitrineProdutos();
    } catch {
        if (grade) {
            grade.innerHTML = `<div class="empty-state-card">Falha ao carregar itens do catálogo.</div>`;
        }
    }
}

/**
 * Retorna texto/classe de badge de estoque conforme saldo físico disponível ou gargalo do combo.
 * @param {number} disponibilidade Estoque máximo vendável
 * @returns {{texto: string, badgeClass: string}}
 */
function descreverDisponibilidadeEstoque(disponibilidade) {
    const saldo = Number(disponibilidade ?? 0);
    if (saldo <= 0) {
        return { texto: 'Sem estoque', badgeClass: 'badge-danger' };
    }
    if (saldo <= 5) {
        return { texto: `Últimas ${saldo} un.`, badgeClass: 'badge-warning' };
    }
    return { texto: `${saldo} un. disponíveis`, badgeClass: 'badge-success' };
}

/**
 * Filtra os itens carregados pelo termo de busca (nome, SKU ou atributos) e renderiza os cards na grade.
 */
export function renderizarVitrineProdutos() {
    const grade = document.getElementById('grade-produtos');
    const contadorEl = document.getElementById('contador-itens-vitrine');
    const badgeBuscaEl = document.getElementById('filtro-busca-ativo');
    if (!grade) return;

    const estado = obterEstado();
    const termo = String(estado.termoBusca || '').trim().toLowerCase();

    if (badgeBuscaEl) {
        if (termo) {
            badgeBuscaEl.textContent = `Busca: "${estado.termoBusca.trim()}" ×`;
            badgeBuscaEl.hidden = false;
        } else {
            badgeBuscaEl.hidden = true;
        }
    }

    const itensAtivos = (estado.catalogoItens || []).filter((item) => item.ativo !== false);
    const filtrados = termo
        ? itensAtivos.filter((item) => {
            const atributosTexto = resumirAtributosFlyweight(item.atributosFlyweight).toLowerCase();
            return (
                String(item.nomeExibicao || '').toLowerCase().includes(termo)
                || String(item.sku || '').toLowerCase().includes(termo)
                || atributosTexto.includes(termo)
            );
        })
        : itensAtivos;

    const grupos = agruparItensParaVitrine(filtrados);
    if (contadorEl) {
        contadorEl.textContent = `${filtrados.length} ${filtrados.length === 1 ? 'SKU encontrado' : 'SKUs encontrados'} (${grupos.length} ${grupos.length === 1 ? 'oferta' : 'ofertas'})`;
    }

    if (grupos.length === 0) {
        grade.innerHTML = `
            <div class="empty-state-card">
                <p>Nenhum produto ou combo encontrado com os filtros atuais.</p>
                <button type="button" class="btn btn-outline btn-sm" id="btn-vitrine-limpar-filtros">Limpar Filtros</button>
            </div>
        `;
        return;
    }

    grade.innerHTML = grupos.map((grupo) => renderizarCardGrupo(grupo)).join('');
}

/**
 * Renderiza um único card de produto (com variações selecionáveis) ou ComboKit com decomposição e economia.
 * @param {{key: string, nomeTitulo: string, eComboKit: boolean, variacoes: any[]}} grupo
 * @returns {string} HTML do card
 */
function renderizarCardGrupo(grupo) {
    const skuSelecionado = selecaoVariacaoPorGrupo.get(grupo.key);
    const itemAtual = grupo.variacoes.find((v) => v.sku === skuSelecionado) || grupo.variacoes[0];
    if (!itemAtual) return '';

    selecaoVariacaoPorGrupo.set(grupo.key, itemAtual.sku);
    const stockInfo = descreverDisponibilidadeEstoque(itemAtual.disponibilidadeMaximaEstoque);
    const semEstoque = Number(itemAtual.disponibilidadeMaximaEstoque ?? 0) <= 0;

    let blocoTipo = `<span class="badge badge-info">Produto Simples</span>`;
    let blocoVariacoesOuCombo = '';
    let blocoEconomia = '';

    if (itemAtual.eComboKit) {
        blocoTipo = `<span class="badge badge-purple">Combo / Kit</span>`;
        const resumo = calcularResumoCombo(itemAtual);
        const linhasHtml = resumo.linhas.map((l) => `
            <div>• ${escapeHtml(l.quantidade)}x ${escapeHtml(l.nome)} <span class="mono">(${escapeHtml(l.sku)})</span></div>
        `).join('');
        blocoVariacoesOuCombo = `
            <div class="combo-components-box">
                <strong>Composição do Kit (estoque pelo menor gargalo):</strong>
                ${linhasHtml || '<div class="text-muted">Componentes cadastrados no SKU.</div>'}
            </div>
        `;
        if (resumo.economiaReais > 0 || Number(itemAtual.percentualDescontoKit || 0) > 0) {
            const partes = [];
            if (resumo.economiaReais > 0) partes.push(`Economia de ${formatarMoeda(resumo.economiaReais)}`);
            if (Number(itemAtual.percentualDescontoKit || 0) > 0) {
                partes.push(`${Number(itemAtual.percentualDescontoKit)}% OFF`);
            }
            blocoEconomia = `<span class="product-savings">${escapeHtml(partes.join(' • '))}</span>`;
        }
    } else {
        const atributos = Array.isArray(itemAtual.atributosFlyweight) ? itemAtual.atributosFlyweight : [];
        const pillsHtml = atributos.map((attr) => `
            <span class="badge badge-neutral">${escapeHtml(attr.tipo)}: ${escapeHtml(attr.valor)}</span>
        `).join('');

        let seletorHtml = '';
        if (grupo.variacoes.length > 1) {
            const botoes = grupo.variacoes.map((v) => {
                const { sufixoVariacao } = extrairBaseESufixo(v.nomeExibicao);
                const attrTxt = resumirAtributosFlyweight(v.atributosFlyweight);
                const rotulo = attrTxt || sufixoVariacao || v.sku;
                const active = v.sku === itemAtual.sku ? 'active' : '';
                return `
                    <button
                        type="button"
                        class="variation-chip ${active}"
                        data-grupo-key="${escapeHtml(grupo.key)}"
                        data-variacao-sku="${escapeHtml(v.sku)}"
                    >
                        ${escapeHtml(rotulo)}
                    </button>
                `;
            }).join('');
            seletorHtml = `<div class="variation-selector">${botoes}</div>`;
        }

        blocoVariacoesOuCombo = `
            ${seletorHtml}
            ${pillsHtml ? `<div class="attribute-pills">${pillsHtml}</div>` : ''}
        `;
    }

    return `
        <article class="product-card" data-sku-card="${escapeHtml(itemAtual.sku)}">
            <div class="product-card-top">
                ${blocoTipo}
                <span class="badge ${stockInfo.badgeClass}">${escapeHtml(stockInfo.texto)}</span>
            </div>

            <div>
                <h3 class="product-title">${escapeHtml(itemAtual.eComboKit ? itemAtual.nomeExibicao : grupo.nomeTitulo)}</h3>
                <div class="text-muted mono" style="font-size: 0.78rem;">
                    SKU: ${escapeHtml(itemAtual.sku)} • ${escapeHtml(formatarPeso(itemAtual.pesoTotalGramas))}
                </div>
            </div>

            ${blocoVariacoesOuCombo}

            <div class="product-price-row">
                <span class="product-price">${escapeHtml(formatarMoeda(itemAtual.precoVigente))}</span>
                ${blocoEconomia}
            </div>

            <div class="product-card-actions">
                <button
                    type="button"
                    class="btn btn-primary btn-sm"
                    style="flex: 1;"
                    data-add-sku="${escapeHtml(itemAtual.sku)}"
                    ${semEstoque ? 'disabled' : ''}
                >
                    ${semEstoque ? 'Indisponível' : '+ Carrinho'}
                </button>
                <button
                    type="button"
                    class="btn btn-outline btn-sm"
                    data-detalhe-sku="${escapeHtml(itemAtual.sku)}"
                >
                    Detalhes
                </button>
            </div>
        </article>
    `;
}

/**
 * Abre o modal detalhado do item consultando ficha completa e posição de estoque ao vivo na API.
 * @param {string} sku Código SKU selecionado
 */
export async function abrirModalDetalhesItem(sku) {
    try {
        const [itemRes, estoqueRes] = await Promise.allSettled([
            apiGet(`/api/v1/catalogo/itens/${encodeURIComponent(sku)}`, { ignorarAuth: true, suprimirHandler401: true }),
            apiGet(`/api/v1/estoque/${encodeURIComponent(sku)}`, { ignorarAuth: true, suprimirHandler401: true })
        ]);

        const item = itemRes.status === 'fulfilled' ? itemRes.value : catalogoCompletoPorSku.get(sku);
        if (!item) {
            dependenciasUi.exibirToast('Item não encontrado no catálogo.', 'warning');
            return;
        }

        const est = estoqueRes.status === 'fulfilled' ? estoqueRes.value : null;
        const saldoAoVivo = est?.quantidadeDisponivel ?? item.disponibilidadeMaximaEstoque ?? 0;
        const dim = item.dimensoesFisicas || {};

        let blocoEspecifico = '';
        if (item.eComboKit) {
            const resumo = calcularResumoCombo(item);
            blocoEspecifico = `
                <div class="card-inset stack-sm">
                    <strong>Componentes físicos e regra de gargalo (Composite):</strong>
                    ${resumo.linhas.map((l) => `
                        <div>• ${escapeHtml(l.quantidade)}x ${escapeHtml(l.nome)} (<span class="mono">${escapeHtml(l.sku)}</span>) — unit. ${escapeHtml(formatarMoeda(l.precoUnit))}</div>
                    `).join('')}
                    ${resumo.economiaReais > 0 ? `<div class="product-savings">Soma avulsa: ${escapeHtml(formatarMoeda(resumo.precoAvulsoTotal))} • Você economiza ${escapeHtml(formatarMoeda(resumo.economiaReais))}!</div>` : ''}
                </div>
            `;
        } else if (Array.isArray(item.atributosFlyweight) && item.atributosFlyweight.length > 0) {
            blocoEspecifico = `
                <div class="attribute-pills">
                    ${item.atributosFlyweight.map((a) => `
                        <span class="badge badge-info">${escapeHtml(a.tipo)}: ${escapeHtml(a.valor)}</span>
                    `).join('')}
                </div>
            `;
        }

        const html = `
            <div class="stack-md">
                <div>
                    <span class="badge ${item.eComboKit ? 'badge-purple' : 'badge-info'}">
                        ${item.eComboKit ? 'Combo / Kit' : 'Variação Física (SKU)'}
                    </span>
                    <span class="badge ${saldoAoVivo > 0 ? 'badge-success' : 'badge-danger'}">
                        Estoque disponível ao vivo: ${escapeHtml(saldoAoVivo)} un.
                    </span>
                    ${est && !item.eComboKit ? `<span class="badge badge-neutral">Reservado em carrinhos/pedidos: ${escapeHtml(est.quantidadeReservada ?? 0)}</span>` : ''}
                </div>

                <div>
                    <p class="mono text-muted">SKU: ${escapeHtml(item.sku)}</p>
                    <p class="product-price">${escapeHtml(formatarMoeda(item.precoVigente))}</p>
                </div>

                ${blocoEspecifico}

                <div class="card-inset">
                    <strong>Dimensões Físicas e Logística</strong>
                    <p class="text-muted">
                        Peso: ${escapeHtml(formatarPeso(item.pesoTotalGramas))} •
                        Altura: ${escapeHtml(dim.alturaCm ?? '-')} cm •
                        Largura: ${escapeHtml(dim.larguraCm ?? '-')} cm •
                        Profundidade: ${escapeHtml(dim.profundidadeCm ?? '-')} cm
                    </p>
                </div>

                <div class="form-actions">
                    <button
                        type="button"
                        class="btn btn-primary"
                        id="btn-modal-detalhe-adicionar"
                        data-modal-add-sku="${escapeHtml(item.sku)}"
                        ${saldoAoVivo <= 0 ? 'disabled' : ''}
                    >
                        ${saldoAoVivo <= 0 ? 'Item Sem Estoque' : 'Adicionar ao Carrinho'}
                    </button>
                </div>
            </div>
        `;

        dependenciasUi.abrirModalDetalhes(item.nomeExibicao, html);
        document.getElementById('btn-modal-detalhe-adicionar')?.addEventListener('click', async () => {
            if (typeof callbackAdicionarCarrinho === 'function') {
                await callbackAdicionarCarrinho(item.sku, 1);
            }
        });
    } catch (erro) {
        dependenciasUi.exibirToast(erro?.mensagem || 'Não foi possível carregar os detalhes do item.', 'error');
    }
}

/**
 * Recarrega completamente promoções, árvore de categorias e catálogo de vitrine.
 */
export async function recarregarVitrineCompleta() {
    const estado = obterEstado();
    await Promise.all([
        carregarPromocoesAtivas(),
        carregarArvoreCategorias(),
        carregarItensCatalogo(estado.categoriaSelecionadaId)
    ]);
}

/**
 * Inicializa todos os manipuladores de eventos da Vitrine e carrega os dados iniciais.
 * @param {{exibirToast: Function, abrirModalDetalhes: Function}} ui Helpers visuais de main.js
 */
export async function inicializarModuloCatalogo(ui) {
    dependenciasUi = { ...dependenciasUi, ...ui };

    document.getElementById('arvore-categorias')?.addEventListener('click', async (evento) => {
        const btn = evento.target.closest('.category-btn');
        if (!btn) return;
        const categoriaId = btn.getAttribute('data-categoria-id');
        const caminho = btn.getAttribute('data-categoria-caminho') || btn.textContent.trim();
        atualizarEstado({
            categoriaSelecionadaId: categoriaId,
            categoriaSelecionadaNome: caminho
        }, 'FILTRO_CATEGORIA');
        renderizarArvoreCategorias();
        await carregarItensCatalogo(categoriaId);
    });

    document.getElementById('btn-limpar-categoria')?.addEventListener('click', async () => {
        atualizarEstado({
            categoriaSelecionadaId: null,
            categoriaSelecionadaNome: 'Todas as categorias'
        }, 'FILTRO_CATEGORIA');
        renderizarArvoreCategorias();
        await carregarItensCatalogo(null);
    });

    document.getElementById('form-busca-global')?.addEventListener('submit', (evento) => {
        evento.preventDefault();
        const termo = document.getElementById('input-busca-catalogo')?.value || '';
        atualizarEstado({ termoBusca: termo }, 'BUSCA');
        renderizarVitrineProdutos();
    });

    document.getElementById('input-busca-catalogo')?.addEventListener('input', (evento) => {
        atualizarEstado({ termoBusca: evento.target.value || '' }, 'BUSCA');
        renderizarVitrineProdutos();
    });

    document.getElementById('filtro-busca-ativo')?.addEventListener('click', () => {
        const input = document.getElementById('input-busca-catalogo');
        if (input) input.value = '';
        atualizarEstado({ termoBusca: '' }, 'BUSCA');
        renderizarVitrineProdutos();
    });

    document.getElementById('btn-recarregar-vitrine')?.addEventListener('click', async () => {
        await recarregarVitrineCompleta();
        dependenciasUi.exibirToast('Vitrine e saldos atualizados.', 'info');
    });

    document.getElementById('grade-produtos')?.addEventListener('click', async (evento) => {
        const chipVariacao = evento.target.closest('.variation-chip');
        if (chipVariacao) {
            const grupoKey = chipVariacao.getAttribute('data-grupo-key');
            const sku = chipVariacao.getAttribute('data-variacao-sku');
            if (grupoKey && sku) {
                selecaoVariacaoPorGrupo.set(grupoKey, sku);
                renderizarVitrineProdutos();
            }
            return;
        }

        const btnDetalhe = evento.target.closest('[data-detalhe-sku]');
        if (btnDetalhe) {
            const sku = btnDetalhe.getAttribute('data-detalhe-sku');
            if (sku) await abrirModalDetalhesItem(sku);
            return;
        }

        const btnAdicionar = evento.target.closest('[data-add-sku]');
        if (btnAdicionar) {
            const sku = btnAdicionar.getAttribute('data-add-sku');
            if (sku && typeof callbackAdicionarCarrinho === 'function') {
                await callbackAdicionarCarrinho(sku, 1);
            } else {
                dependenciasUi.exibirToast('Módulo do carrinho ainda está sendo carregado.', 'info');
            }
            return;
        }

        if (evento.target.closest('#btn-vitrine-limpar-filtros')) {
            const input = document.getElementById('input-busca-catalogo');
            if (input) input.value = '';
            atualizarEstado({
                categoriaSelecionadaId: null,
                categoriaSelecionadaNome: 'Todas as categorias',
                termoBusca: ''
            }, 'FILTRO_RESET');
            renderizarArvoreCategorias();
            await carregarItensCatalogo(null);
        }
    });

    await carregarItensCatalogo(null);
    await Promise.all([
        carregarPromocoesAtivas(),
        carregarArvoreCategorias()
    ]);
}
