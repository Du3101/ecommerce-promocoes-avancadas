/**
 * Painel Administrativo RBAC Dinâmico (visível conforme perfil do usuário autenticado):
 * ADMIN (acesso total), OPERADOR_COMERCIAL (catálogo e expedição),
 * RESPONSAVEL_ESTOQUE (inventário e reservas) e RESPONSAVEL_PROMOCOES (campanhas e cupons).
 */

import { apiGet, apiPatch, apiPost } from './api-client.js';
import { montarHtmlSnapshotPedido } from './checkout-pedidos.js';
import {
    MAPA_STATUS_PEDIDO,
    assinarEstado,
    atualizarEstado,
    escapeHtml,
    formatarDataHora,
    formatarMoeda,
    obterEstado,
    obterMetaStatusPedido,
    usuarioPossuiAlgumPerfil
} from './store.js';

let dependenciasUi = {
    exibirToast: () => {},
    abrirModalDetalhes: () => {},
    recarregarVitrineCompleta: async () => {}
};

/**
 * Mapa declarativo orientado a tabela de abas administrativas x perfis RBAC (Princípio OCP).
 */
const ABAS_ADMIN = [
    {
        id: 'catalogo',
        titulo: 'Catálogo e Combos',
        perfis: ['ADMIN', 'OPERADOR_COMERCIAL'],
        renderizar: renderizarAbaCatalogo
    },
    {
        id: 'estoque',
        titulo: 'Estoque',
        perfis: ['ADMIN', 'RESPONSAVEL_ESTOQUE'],
        renderizar: renderizarAbaEstoque
    },
    {
        id: 'promocoes',
        titulo: 'Promoções e Cupons',
        perfis: ['ADMIN', 'RESPONSAVEL_PROMOCOES'],
        renderizar: renderizarAbaPromocoes
    },
    {
        id: 'pedidos',
        titulo: 'Expedição de Pedidos',
        perfis: ['ADMIN', 'OPERADOR_COMERCIAL'],
        renderizar: renderizarAbaPedidos
    },
    {
        id: 'auditoria',
        titulo: 'Auditoria e Operadores',
        perfis: ['ADMIN'],
        renderizar: renderizarAbaAuditoria
    },
    {
        id: 'relatorios',
        titulo: 'Relatórios Gerenciais',
        perfis: ['ADMIN'],
        renderizar: renderizarAbaRelatorios
    }
];

/**
 * Achatamento recursivo da árvore Composite de categorias para selects administrativos.
 * @param {any[]} nos Nós da árvore
 * @param {any[]} [acumulador] Lista acumulada
 * @returns {Array<{id: string, rotulo: string}>}
 */
function achatarCategoriasSelect(nos, acumulador = []) {
    for (const no of nos || []) {
        acumulador.push({ id: no.id, rotulo: no.caminhoHierarquico || no.nome });
        if (Array.isArray(no.subcategorias) && no.subcategorias.length > 0) {
            achatarCategoriasSelect(no.subcategorias, acumulador);
        }
    }
    return acumulador;
}

/**
 * Obtém as abas visíveis conforme os perfis RBAC do usuário logado e renderiza o painel.
 */
export async function carregarPainelAdmin() {
    const estado = obterEstado();
    const abasPermitidas = ABAS_ADMIN.filter((aba) => usuarioPossuiAlgumPerfil(aba.perfis));

    const badgePerfis = document.getElementById('admin-perfis-ativos');
    const navEl = document.getElementById('admin-abas-nav');
    const conteudoEl = document.getElementById('admin-tab-conteudo');
    if (!navEl || !conteudoEl) return;

    if (badgePerfis) {
        badgePerfis.textContent = (estado.auth?.perfis || []).join(' • ');
    }

    if (abasPermitidas.length === 0) {
        navEl.innerHTML = '';
        conteudoEl.innerHTML = `<p class="text-muted">Seu usuário não possui permissões administrativas.</p>`;
        return;
    }

    const ativaValida = abasPermitidas.find((a) => a.id === estado.abaAdminAtiva) || abasPermitidas[0];
    atualizarEstado({ abaAdminAtiva: ativaValida.id }, 'ADMIN_ABA_SILENT');

    navEl.innerHTML = abasPermitidas.map((aba) => `
        <button
            type="button"
            class="admin-tab-btn ${aba.id === ativaValida.id ? 'active' : ''}"
            data-admin-tab="${escapeHtml(aba.id)}"
            role="tab"
        >
            ${escapeHtml(aba.titulo)}
        </button>
    `).join('');

    conteudoEl.innerHTML = `<p class="text-muted">Carregando ${escapeHtml(ativaValida.titulo)}...</p>`;
    await ativaValida.renderizar(conteudoEl);
}

/**
 * Aba 1: Gestão de Categorias, Produtos, Variações, Combos, Preços e Ativação.
 * @param {HTMLElement} container Elemento destino
 */
async function renderizarAbaCatalogo(container) {
    const [arvore, itens] = await Promise.all([
        apiGet('/api/v1/catalogo/categorias/arvore', { ignorarAuth: true, suprimirHandler401: true }),
        apiGet('/api/v1/admin/catalogo/itens')
    ]);
    const categoriasFlat = achatarCategoriasSelect(Array.isArray(arvore) ? arvore : []);
    const optionsCat = categoriasFlat.map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.rotulo)}</option>`).join('');
    const skusSimples = (Array.isArray(itens) ? itens : []).filter((it) => !it.eComboKit && it.ativo);

    container.innerHTML = `
        <div class="stack-md">
            <div class="admin-grid-2">
                <form id="form-admin-categoria" class="card-inset stack-sm">
                    <h3>Nova Categoria</h3>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-cat-nome">Nome*</label>
                            <input id="adm-cat-nome" type="text" required placeholder="Periféricos">
                        </div>
                        <div class="form-group">
                            <label for="adm-cat-slug">Slug*</label>
                            <input id="adm-cat-slug" type="text" required placeholder="perifericos-gamer">
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="adm-cat-pai">Categoria Pai (opcional)</label>
                        <select id="adm-cat-pai">
                            <option value="">Raiz (Sem pai)</option>
                            ${optionsCat}
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm">Criar Categoria</button>
                </form>

                <form id="form-admin-produto" class="card-inset stack-sm">
                    <h3>Novo Produto + Variação Inicial</h3>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-prod-nome">Nome do Produto*</label>
                            <input id="adm-prod-nome" type="text" required placeholder="Headset Pro">
                        </div>
                        <div class="form-group">
                            <label for="adm-prod-marca">Marca*</label>
                            <input id="adm-prod-marca" type="text" required placeholder="FATEC Tech">
                        </div>
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-prod-preco">Preço Base (R$)*</label>
                            <input id="adm-prod-preco" type="number" step="0.01" min="0.01" required placeholder="249.90">
                        </div>
                        <div class="form-group">
                            <label for="adm-prod-cat">Categoria*</label>
                            <select id="adm-prod-cat" required>${optionsCat}</select>
                        </div>
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-var-sku">SKU Inicial*</label>
                            <input id="adm-var-sku" type="text" required placeholder="HEAD-PRO-PRETO">
                        </div>
                        <div class="form-group">
                            <label for="adm-var-nome">Nome Variação*</label>
                            <input id="adm-var-nome" type="text" required placeholder="Headset Pro - Preto">
                        </div>
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-var-cor">Cor (Flyweight)*</label>
                            <input id="adm-var-cor" type="text" required value="Preto">
                        </div>
                        <div class="form-group">
                            <label for="adm-var-estoque">Estoque Inicial*</label>
                            <input id="adm-var-estoque" type="number" min="0" required value="10">
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm">Cadastrar Produto e Variação</button>
                </form>
            </div>

            <form id="form-admin-combo" class="card-inset stack-sm">
                <h3>Novo Combo / Kit (Composite)</h3>
                <div class="form-row-2">
                    <div class="form-group">
                        <label for="adm-combo-sku">SKU do Combo*</label>
                        <input id="adm-combo-sku" type="text" required placeholder="KIT-SETUP-PRO">
                    </div>
                    <div class="form-group">
                        <label for="adm-combo-nome">Nome do Combo*</label>
                        <input id="adm-combo-nome" type="text" required placeholder="Kit Setup Produtividade">
                    </div>
                </div>
                <div class="form-row-2">
                    <div class="form-group">
                        <label for="adm-combo-cat">Categoria*</label>
                        <select id="adm-combo-cat" required>${optionsCat}</select>
                    </div>
                    <div class="form-group">
                        <label for="adm-combo-desc">Desconto Recursivo (%)</label>
                        <input id="adm-combo-desc" type="number" step="0.01" min="0" max="90" value="15">
                    </div>
                </div>
                <div class="form-row-2">
                    <div class="form-group">
                        <label for="adm-combo-c1">Componente 1*</label>
                        <select id="adm-combo-c1" required>
                            ${skusSimples.map((s) => `<option value="${escapeHtml(s.sku)}">${escapeHtml(s.sku)} — ${escapeHtml(s.nomeExibicao)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="adm-combo-c2">Componente 2*</label>
                        <select id="adm-combo-c2" required>
                            ${skusSimples.map((s, idx) => `<option value="${escapeHtml(s.sku)}" ${idx === 1 ? 'selected' : ''}>${escapeHtml(s.sku)} — ${escapeHtml(s.nomeExibicao)}</option>`).join('')}
                        </select>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-sm">Criar Combo/Kit</button>
            </form>

            <div>
                <h3>Itens no Catálogo (Ativos e Inativos)</h3>
                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>SKU</th>
                                <th>Exibição</th>
                                <th>Tipo</th>
                                <th>Preço Vigente</th>
                                <th>Estoque</th>
                                <th>Status</th>
                                <th>Ações Auditadas</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${(Array.isArray(itens) ? itens : []).map((it) => `
                                <tr>
                                    <td class="mono">${escapeHtml(it.sku)}</td>
                                    <td>${escapeHtml(it.nomeExibicao)}</td>
                                    <td>${it.eComboKit ? 'Combo' : 'Produto'}</td>
                                    <td>
                                        <div style="display: flex; gap: 0.35rem; align-items: center;">
                                            <input type="number" step="0.01" min="0.01" value="${escapeHtml(it.precoVigente)}" data-preco-input="${escapeHtml(it.sku)}" style="width: 95px;">
                                            <button type="button" class="btn btn-outline btn-xs" data-salvar-preco="${escapeHtml(it.sku)}">Alterar</button>
                                        </div>
                                    </td>
                                    <td>${escapeHtml(it.disponibilidadeMaximaEstoque)} un.</td>
                                    <td><span class="badge ${it.ativo ? 'badge-success' : 'badge-neutral'}">${it.ativo ? 'Ativo' : 'Inativo'}</span></td>
                                    <td>
                                        <button type="button" class="btn ${it.ativo ? 'btn-danger-outline' : 'btn-outline'} btn-xs" data-toggle-status-sku="${escapeHtml(it.sku)}" data-novo-status="${!it.ativo}">
                                            ${it.ativo ? 'Inativar' : 'Reativar'}
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    `;

    document.getElementById('form-admin-categoria')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        try {
            await apiPost('/api/v1/admin/catalogo/categorias', {
                nome: document.getElementById('adm-cat-nome').value.trim(),
                slug: document.getElementById('adm-cat-slug').value.trim(),
                categoriaPaiId: document.getElementById('adm-cat-pai').value || null
            });
            dependenciasUi.exibirToast('Categoria criada e registrada na auditoria!', 'success');
            await Promise.all([renderizarAbaCatalogo(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao criar categoria.', 'error');
        }
    });

    document.getElementById('form-admin-produto')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const sku = document.getElementById('adm-var-sku').value.trim();
        const cor = document.getElementById('adm-var-cor').value.trim() || 'Padrão';
        try {
            await apiPost('/api/v1/admin/catalogo/produtos', {
                nome: document.getElementById('adm-prod-nome').value.trim(),
                descricao: 'Cadastrado via Painel Administrativo',
                marca: document.getElementById('adm-prod-marca').value.trim(),
                precoBase: Number(document.getElementById('adm-prod-preco').value),
                categoriaId: document.getElementById('adm-prod-cat').value,
                atributosEspecificos: { origem: 'PortalAdmin' },
                variacoes: [{
                    sku,
                    codigoBarras: null,
                    nomeVariacao: document.getElementById('adm-var-nome').value.trim(),
                    adicionalPreco: 0,
                    pesoGramas: 450,
                    alturaCm: 10,
                    larguraCm: 15,
                    profundidadeCm: 15,
                    estoqueInicial: Number(document.getElementById('adm-var-estoque').value || 0),
                    atributos: [{ tipo: 'COR', valor: cor, codigoApresentacao: cor.toUpperCase() }]
                }]
            });
            dependenciasUi.exibirToast(`Produto/SKU ${sku} cadastrado!`, 'success');
            await Promise.all([renderizarAbaCatalogo(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao cadastrar produto.', 'error');
        }
    });

    document.getElementById('form-admin-combo')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const c1 = document.getElementById('adm-combo-c1').value;
        const c2 = document.getElementById('adm-combo-c2').value;
        const componentes = c1 === c2
            ? [{ componenteSku: c1, quantidadePorKit: 2 }]
            : [{ componenteSku: c1, quantidadePorKit: 1 }, { componenteSku: c2, quantidadePorKit: 1 }];
        try {
            await apiPost('/api/v1/admin/catalogo/combos', {
                skuCombo: document.getElementById('adm-combo-sku').value.trim(),
                nomeCombo: document.getElementById('adm-combo-nome').value.trim(),
                descricao: 'Kit criado pelo Painel Administrativo',
                categoriaId: document.getElementById('adm-combo-cat').value,
                modoPreco: 'SOMA_RECURSIVA_COMPONENTES',
                percentualDescontoKit: Number(document.getElementById('adm-combo-desc').value || 0),
                precoFixoOpcional: null,
                componentes
            });
            dependenciasUi.exibirToast('Combo/Kit criado com sucesso!', 'success');
            await Promise.all([renderizarAbaCatalogo(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao criar ComboKit.', 'error');
        }
    });

    container.querySelectorAll('[data-salvar-preco]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const sku = btn.getAttribute('data-salvar-preco');
            const input = container.querySelector(`[data-preco-input="${CSS.escape(sku)}"]`);
            const novoValor = Number(input?.value);
            if (!Number.isFinite(novoValor) || novoValor <= 0) {
                dependenciasUi.exibirToast('Informe um preço positivo válido.', 'warning');
                return;
            }
            try {
                await apiPatch(`/api/v1/admin/catalogo/itens/${encodeURIComponent(sku)}/preco`, { novoValor });
                dependenciasUi.exibirToast(`Preço de ${sku} atualizado e auditado!`, 'success');
                await Promise.all([renderizarAbaCatalogo(container), dependenciasUi.recarregarVitrineCompleta()]);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao alterar preço.', 'error');
            }
        });
    });

    container.querySelectorAll('[data-toggle-status-sku]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const sku = btn.getAttribute('data-toggle-status-sku');
            const ativo = btn.getAttribute('data-novo-status') === 'true';
            try {
                await apiPatch(`/api/v1/admin/catalogo/itens/${encodeURIComponent(sku)}/status`, { ativo });
                dependenciasUi.exibirToast(`Status do SKU ${sku} atualizado.`, 'info');
                await Promise.all([renderizarAbaCatalogo(container), dependenciasUi.recarregarVitrineCompleta()]);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao alterar status.', 'error');
            }
        });
    });
}

/**
 * Aba 2: Ajuste manual de inventário e expiração de reservas vencidas.
 * @param {HTMLElement} container Elemento destino
 */
async function renderizarAbaEstoque(container) {
    const itens = await apiGet('/api/v1/catalogo/itens', { ignorarAuth: true, suprimirHandler401: true });
    const fisicos = (Array.isArray(itens) ? itens : []).filter((it) => !it.eComboKit);

    container.innerHTML = `
        <div class="stack-md">
            <div class="admin-grid-2">
                <form id="form-admin-ajuste-estoque" class="card-inset stack-sm">
                    <h3>Ajuste Manual de Inventário</h3>
                    <div class="form-group">
                        <label for="adm-est-sku">SKU Físico*</label>
                        <select id="adm-est-sku" required>
                            ${fisicos.map((s) => `<option value="${escapeHtml(s.sku)}">${escapeHtml(s.sku)} — ${escapeHtml(s.nomeExibicao)} (disp: ${escapeHtml(s.disponibilidadeMaximaEstoque)})</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-est-delta">Delta Quantidade (+ ou -)*</label>
                            <input id="adm-est-delta" type="number" required value="5">
                        </div>
                        <div class="form-group">
                            <label for="adm-est-motivo">Motivo Obrigatório*</label>
                            <input id="adm-est-motivo" type="text" required value="Reposição de estoque em painel">
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm">Aplicar Ajuste Auditado</button>
                </form>

                <div class="card-inset stack-sm">
                    <h3>Manutenção de Reservas Concorrentes</h3>
                    <p class="text-muted">Libera reservas temporárias cujo prazo de expiração já venceu, devolvendo o saldo ao estoque disponível.</p>
                    <button type="button" class="btn btn-outline btn-sm" id="btn-expirar-reservas">
                        Expirar Reservas Vencidas Agora
                    </button>
                </div>
            </div>

            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr><th>SKU</th><th>Produto</th><th>Disponível para Venda</th></tr>
                    </thead>
                    <tbody>
                        ${fisicos.map((s) => `
                            <tr>
                                <td class="mono">${escapeHtml(s.sku)}</td>
                                <td>${escapeHtml(s.nomeExibicao)}</td>
                                <td><strong>${escapeHtml(s.disponibilidadeMaximaEstoque)} un.</strong></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('form-admin-ajuste-estoque')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const sku = document.getElementById('adm-est-sku').value;
        try {
            const res = await apiPost(`/api/v1/admin/estoque/${encodeURIComponent(sku)}/ajustes`, {
                deltaQuantidade: Number(document.getElementById('adm-est-delta').value),
                motivo: document.getElementById('adm-est-motivo').value.trim()
            });
            dependenciasUi.exibirToast(`Estoque de ${res.sku} atualizado: ${res.quantidadeDisponivel} un. disponíveis.`, 'success');
            await Promise.all([renderizarAbaEstoque(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro no ajuste de estoque.', 'error');
        }
    });

    document.getElementById('btn-expirar-reservas')?.addEventListener('click', async () => {
        try {
            const res = await apiPost('/api/v1/admin/estoque/reservas/expirar-vencidas', {});
            dependenciasUi.exibirToast(`Reservas expiradas liberadas: ${res.reservasExpiradas ?? 0}`, 'info');
            await Promise.all([renderizarAbaEstoque(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Falha ao expirar reservas.', 'error');
        }
    });
}

/**
 * Aba 3: Gestão de Campanhas Promocionais e Cupons (Módulo 2).
 * @param {HTMLElement} container Elemento destino
 */
async function renderizarAbaPromocoes(container) {
    const [campanhas, cupons] = await Promise.all([
        apiGet('/api/v1/admin/promocoes'),
        apiGet('/api/v1/admin/cupons')
    ]);

    container.innerHTML = `
        <div class="stack-md">
            <div class="admin-grid-2">
                <form id="form-admin-campanha" class="card-inset stack-sm">
                    <h3>Nova Campanha Promocional</h3>
                    <div class="form-group">
                        <label for="adm-camp-nome">Nome*</label>
                        <input id="adm-camp-nome" type="text" required placeholder="Semana Tech - R$ 40 OFF">
                    </div>
                    <div class="form-group">
                        <label for="adm-camp-desc">Descrição*</label>
                        <input id="adm-camp-desc" type="text" required placeholder="Desconto automático para compras acima de R$ 200">
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-camp-tipo">Mecânica*</label>
                            <select id="adm-camp-tipo" required>
                                <option value="VALOR_FIXO_META_CARRINHO">Valor Fixo Meta Carrinho</option>
                                <option value="PERCENTUAL_CONDICIONAL">Percentual Condicional</option>
                                <option value="LEVE_X_PAGUE_Y">Leve X e Pague Y</option>
                                <option value="COMBO_CRUZADO_CASADO">Combo Cruzado Casado</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="adm-camp-prio">Prioridade*</label>
                            <input id="adm-camp-prio" type="number" value="50" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="adm-camp-json">Parâmetros JSON</label>
                        <input id="adm-camp-json" type="text" value='{"metaMinima": 200.00, "valorDesconto": 40.00}'>
                    </div>
                    <label class="checkbox-inline">
                        <input id="adm-camp-cumulativa" type="checkbox" checked> Cumulativa com outras ofertas
                    </label>
                    <button type="submit" class="btn btn-primary btn-sm">Criar Campanha</button>
                </form>

                <form id="form-admin-cupom" class="card-inset stack-sm">
                    <h3>Novo Cupom de Desconto</h3>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-cup-codigo">Código*</label>
                            <input id="adm-cup-codigo" type="text" required placeholder="PROMO20">
                        </div>
                        <div class="form-group">
                            <label for="adm-cup-tipo">Tipo*</label>
                            <select id="adm-cup-tipo" required>
                                <option value="PERCENTUAL">Percentual (%)</option>
                                <option value="VALOR_FIXO">Valor Fixo (R$)</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="adm-cup-valor">Valor Desconto*</label>
                            <input id="adm-cup-valor" type="number" step="0.01" min="0.01" required value="20">
                        </div>
                        <div class="form-group">
                            <label for="adm-cup-min">Carrinho Mínimo (R$)</label>
                            <input id="adm-cup-min" type="number" step="0.01" min="0" value="100">
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="adm-cup-usos">Limite de Usos*</label>
                        <input id="adm-cup-usos" type="number" min="1" value="50" required>
                    </div>
                    <label class="checkbox-inline">
                        <input id="adm-cup-cumulativo" type="checkbox" checked> Cumulativo com campanhas automáticas
                    </label>
                    <button type="submit" class="btn btn-primary btn-sm">Criar Cupom</button>
                </form>
            </div>

            <h3>Campanhas Cadastradas</h3>
            <div class="table-responsive">
                <table class="data-table">
                    <thead><tr><th>Campanha</th><th>Mecânica</th><th>Prioridade</th><th>Status</th><th>Ação</th></tr></thead>
                    <tbody>
                        ${(Array.isArray(campanhas) ? campanhas : []).map((c) => `
                            <tr>
                                <td><strong>${escapeHtml(c.nome)}</strong><br><small class="text-muted">${escapeHtml(c.descricao)}</small></td>
                                <td>${escapeHtml(c.tipoMecanica)}</td>
                                <td>${escapeHtml(c.prioridade)} (${c.cumulativa ? 'Cumulativa' : 'Exclusiva'})</td>
                                <td><span class="badge ${c.ativa ? 'badge-success' : 'badge-neutral'}">${c.ativa ? 'Ativa' : 'Inativa'}</span></td>
                                <td>
                                    <button type="button" class="btn btn-outline btn-xs" data-toggle-campanha="${escapeHtml(c.id)}" data-ativo-alvo="${!c.ativa}">
                                        ${c.ativa ? 'Desativar' : 'Ativar'}
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>

            <h3>Cupons Cadastrados</h3>
            <div class="table-responsive">
                <table class="data-table">
                    <thead><tr><th>Código</th><th>Desconto</th><th>Mínimo</th><th>Usos</th><th>Status</th></tr></thead>
                    <tbody>
                        ${(Array.isArray(cupons) ? cupons : []).map((cup) => `
                            <tr>
                                <td class="mono"><strong>${escapeHtml(cup.codigo)}</strong></td>
                                <td>${cup.tipoDesconto === 'PERCENTUAL' ? `${escapeHtml(cup.valorDesconto)}%` : escapeHtml(formatarMoeda(cup.valorDesconto))}</td>
                                <td>${escapeHtml(formatarMoeda(cup.valorMinimoCarrinho))}</td>
                                <td>${escapeHtml(cup.quantidadeUsosAtuais)} / ${escapeHtml(cup.quantidadeMaximaUsos)}</td>
                                <td><span class="badge ${cup.ativo ? 'badge-success' : 'badge-neutral'}">${cup.ativo ? 'Ativo' : 'Inativo'}</span></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('form-admin-campanha')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const agora = new Date();
        const fim = new Date(agora.getTime() + 60 * 86400 * 1000);
        try {
            await apiPost('/api/v1/admin/promocoes', {
                nome: document.getElementById('adm-camp-nome').value.trim(),
                descricao: document.getElementById('adm-camp-desc').value.trim(),
                tipoMecanica: document.getElementById('adm-camp-tipo').value,
                parametrosJson: document.getElementById('adm-camp-json').value.trim() || '{}',
                prioridade: Number(document.getElementById('adm-camp-prio').value || 10),
                cumulativa: Boolean(document.getElementById('adm-camp-cumulativa').checked),
                dataInicio: agora.toISOString(),
                dataFim: fim.toISOString()
            });
            dependenciasUi.exibirToast('Campanha promocional cadastrada!', 'success');
            await Promise.all([renderizarAbaPromocoes(container), dependenciasUi.recarregarVitrineCompleta()]);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao criar campanha.', 'error');
        }
    });

    document.getElementById('form-admin-cupom')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const expira = new Date(Date.now() + 90 * 86400 * 1000);
        try {
            await apiPost('/api/v1/admin/cupons', {
                codigo: document.getElementById('adm-cup-codigo').value.trim().toUpperCase(),
                tipoDesconto: document.getElementById('adm-cup-tipo').value,
                valorDesconto: Number(document.getElementById('adm-cup-valor').value),
                valorMinimoCarrinho: Number(document.getElementById('adm-cup-min').value || 0),
                quantidadeMaximaUsos: Number(document.getElementById('adm-cup-usos').value || 100),
                expiraEm: expira.toISOString(),
                cumulativoComPromocoes: Boolean(document.getElementById('adm-cup-cumulativo').checked)
            });
            dependenciasUi.exibirToast('Cupom criado com sucesso!', 'success');
            await renderizarAbaPromocoes(container);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao criar cupom.', 'error');
        }
    });

    container.querySelectorAll('[data-toggle-campanha]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-toggle-campanha');
            const ativo = btn.getAttribute('data-ativo-alvo') === 'true';
            try {
                await apiPatch(`/api/v1/admin/promocoes/${encodeURIComponent(id)}/status?ativo=${ativo}`);
                dependenciasUi.exibirToast('Vigência da campanha atualizada!', 'info');
                await Promise.all([renderizarAbaPromocoes(container), dependenciasUi.recarregarVitrineCompleta()]);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao alterar status da campanha.', 'error');
            }
        });
    });
}

/**
 * Aba 4: Fila geral de pedidos e expedição logística State (preparar, despachar com rastreio, entregar).
 * @param {HTMLElement} container Elemento destino
 * @param {string} [filtroStatus] StatusPedido selecionado ou vazio
 */
async function renderizarAbaPedidos(container, filtroStatus = '') {
    const query = filtroStatus ? `?status=${encodeURIComponent(filtroStatus)}` : '';
    const pedidos = await apiGet(`/api/v1/admin/pedidos${query}`);
    const opcoesStatus = Object.entries(MAPA_STATUS_PEDIDO).map(([cod, meta]) => `
        <option value="${escapeHtml(cod)}" ${cod === filtroStatus ? 'selected' : ''}>${escapeHtml(meta.label)} (${escapeHtml(cod)})</option>
    `).join('');

    container.innerHTML = `
        <div class="stack-md">
            <div class="section-header-inline">
                <div class="form-group" style="min-width: 260px;">
                    <label for="adm-filtro-status-pedido">Filtrar por Status</label>
                    <select id="adm-filtro-status-pedido">
                        <option value="">Todos os Pedidos</option>
                        ${opcoesStatus}
                    </select>
                </div>
                <button type="button" class="btn btn-outline btn-xs" id="btn-adm-atualizar-pedidos">Atualizar Fila</button>
            </div>

            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Pedido</th>
                            <th>Data</th>
                            <th>Status</th>
                            <th>Total</th>
                            <th>Rastreamento</th>
                            <th>Ações State</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${(Array.isArray(pedidos) ? pedidos : []).map((p) => {
                            const meta = obterMetaStatusPedido(p.status);
                            let botoesEstado = '';
                            if (p.status === 'PAGO') {
                                botoesEstado = `<button type="button" class="btn btn-primary btn-xs" data-adm-preparar="${escapeHtml(p.pedidoId)}">Preparar</button>`;
                            } else if (p.status === 'EM_PREPARACAO') {
                                botoesEstado = `
                                    <div style="display: flex; gap: 0.3rem; align-items: center;">
                                        <input type="text" placeholder="Rastreio (opc.)" data-rastreio-input="${escapeHtml(p.pedidoId)}" style="width: 115px;">
                                        <button type="button" class="btn btn-primary btn-xs" data-adm-despachar="${escapeHtml(p.pedidoId)}">Despachar</button>
                                    </div>
                                `;
                            } else if (p.status === 'ENVIADO') {
                                botoesEstado = `<button type="button" class="btn btn-primary btn-xs" data-adm-entregar="${escapeHtml(p.pedidoId)}">Confirmar Entrega</button>`;
                            }
                            return `
                                <tr>
                                    <td class="mono"><strong>${escapeHtml(p.numeroPedido)}</strong><br><small class="text-muted">${escapeHtml(p.metodoPagamento)}</small></td>
                                    <td>${escapeHtml(formatarDataHora(p.criadoEm))}</td>
                                    <td><span class="badge ${meta.cssClass}">${escapeHtml(meta.label)}</span></td>
                                    <td><strong>${escapeHtml(formatarMoeda(p.snapshotFinanceiro?.valorTotalLiquido))}</strong></td>
                                    <td class="mono">${escapeHtml(p.entrega?.codigoRastreio || '-')}</td>
                                    <td>
                                        <div style="display: flex; gap: 0.35rem; flex-wrap: wrap;">
                                            ${botoesEstado}
                                            <button type="button" class="btn btn-outline btn-xs" data-adm-snapshot="${escapeHtml(p.pedidoId)}">Snapshot</button>
                                        </div>
                                    </td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('adm-filtro-status-pedido')?.addEventListener('change', (ev) => {
        renderizarAbaPedidos(container, ev.target.value);
    });
    document.getElementById('btn-adm-atualizar-pedidos')?.addEventListener('click', () => {
        const atual = document.getElementById('adm-filtro-status-pedido')?.value || '';
        renderizarAbaPedidos(container, atual);
    });

    container.querySelectorAll('[data-adm-preparar]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-adm-preparar');
            try {
                await apiPatch(`/api/v1/admin/pedidos/${encodeURIComponent(id)}/preparar`);
                dependenciasUi.exibirToast('Pedido movido para EM_PREPARACAO!', 'success');
                await renderizarAbaPedidos(container, filtroStatus);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao preparar pedido.', 'error');
            }
        });
    });

    container.querySelectorAll('[data-adm-despachar]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-adm-despachar');
            const rastreio = container.querySelector(`[data-rastreio-input="${CSS.escape(id)}"]`)?.value?.trim() || '';
            const q = rastreio ? `?codigoRastreio=${encodeURIComponent(rastreio)}` : '';
            try {
                await apiPatch(`/api/v1/admin/pedidos/${encodeURIComponent(id)}/despachar${q}`);
                dependenciasUi.exibirToast('Pedido despachado (ENVIADO)!', 'success');
                await renderizarAbaPedidos(container, filtroStatus);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao despachar pedido.', 'error');
            }
        });
    });

    container.querySelectorAll('[data-adm-entregar]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-adm-entregar');
            try {
                await apiPatch(`/api/v1/admin/pedidos/${encodeURIComponent(id)}/entregar`);
                dependenciasUi.exibirToast('Entrega confirmada (ENTREGUE)!', 'success');
                await renderizarAbaPedidos(container, filtroStatus);
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao registrar entrega.', 'error');
            }
        });
    });

    container.querySelectorAll('[data-adm-snapshot]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-adm-snapshot');
            try {
                const det = await apiGet(`/api/v1/pedidos/${encodeURIComponent(id)}`);
                dependenciasUi.abrirModalDetalhes(`Snapshot Admin — ${det.numeroPedido}`, montarHtmlSnapshotPedido(det));
            } catch (erro) {
                dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao detalhar pedido.', 'error');
            }
        });
    });
}

/**
 * Aba 5: Trilha de Auditoria imutável e Provisionamento de Operadores RBAC (somente ADMIN).
 * @param {HTMLElement} container Elemento destino
 * @param {string} [tipoFiltro] TipoOperacaoAuditoria opcional
 */
async function renderizarAbaAuditoria(container, tipoFiltro = '') {
    const query = tipoFiltro ? `?tipoOperacao=${encodeURIComponent(tipoFiltro)}` : '';
    const logs = await apiGet(`/api/v1/admin/auditoria${query}`);

    container.innerHTML = `
        <div class="stack-md">
            <form id="form-admin-operador" class="card-inset stack-sm">
                <h3>Provisionar Novo Operador Interno (RBAC)</h3>
                <div class="form-row-2">
                    <div class="form-group">
                        <label for="adm-op-email">E-mail Corporativo*</label>
                        <input id="adm-op-email" type="email" required placeholder="operador@ecommerce.fatec.edu.br">
                    </div>
                    <div class="form-group">
                        <label for="adm-op-senha">Senha Inicial*</label>
                        <input id="adm-op-senha" type="password" required minlength="6" placeholder="SenhaForte@2026">
                    </div>
                </div>
                <div class="form-group">
                    <label for="adm-op-perfil">Perfil Operacional*</label>
                    <select id="adm-op-perfil" required>
                        <option value="OPERADOR_COMERCIAL">OPERADOR_COMERCIAL (Catálogo e Expedição)</option>
                        <option value="RESPONSAVEL_ESTOQUE">RESPONSAVEL_ESTOQUE (Estoque e Reservas)</option>
                        <option value="RESPONSAVEL_PROMOCOES">RESPONSAVEL_PROMOCOES (Campanhas e Cupons)</option>
                        <option value="ADMIN">ADMIN (Acesso Total)</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary btn-sm">Criar Operador</button>
            </form>

            <div class="section-header-inline">
                <h3>Trilha de Auditoria</h3>
                <select id="adm-filtro-auditoria" style="max-width: 280px;">
                    <option value="">Todas as Operações</option>
                    <option value="ALTERACAO_PRECO_PRODUTO" ${tipoFiltro === 'ALTERACAO_PRECO_PRODUTO' ? 'selected' : ''}>Alteração Preço Produto</option>
                    <option value="ALTERACAO_PRECO_COMBO" ${tipoFiltro === 'ALTERACAO_PRECO_COMBO' ? 'selected' : ''}>Alteração Preço Combo</option>
                    <option value="CRIACAO_ALTERACAO_CATEGORIA" ${tipoFiltro === 'CRIACAO_ALTERACAO_CATEGORIA' ? 'selected' : ''}>Criação Categoria</option>
                    <option value="CADASTRO_ITEM_CATALOGO" ${tipoFiltro === 'CADASTRO_ITEM_CATALOGO' ? 'selected' : ''}>Cadastro Item Catálogo</option>
                    <option value="AJUSTE_MANUAL_ESTOQUE" ${tipoFiltro === 'AJUSTE_MANUAL_ESTOQUE' ? 'selected' : ''}>Ajuste Manual Estoque</option>
                    <option value="ATIVACAO_INATIVACAO_ITEM" ${tipoFiltro === 'ATIVACAO_INATIVACAO_ITEM' ? 'selected' : ''}>Ativação / Inativação Item</option>
                </select>
            </div>

            <div class="table-responsive">
                <table class="data-table">
                    <thead><tr><th>Quando</th><th>Executor</th><th>Operação</th><th>Alvo</th><th>Antes → Depois</th></tr></thead>
                    <tbody>
                        ${(Array.isArray(logs) ? logs : []).map((log) => `
                            <tr>
                                <td>${escapeHtml(formatarDataHora(log.ocorridoEm))}</td>
                                <td>${escapeHtml(log.emailResponsavel)}<br><small class="text-muted">${escapeHtml(log.perfilResponsavel)}</small></td>
                                <td><span class="badge badge-neutral">${escapeHtml(log.tipoOperacao)}</span></td>
                                <td class="mono">${escapeHtml(log.tipoEntidadeAlvo)}: ${escapeHtml(log.identificadorAlvo)}</td>
                                <td>${escapeHtml(log.valorAnteriorResumo || '-')} → <strong>${escapeHtml(log.valorNovoResumo || '-')}</strong></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('adm-filtro-auditoria')?.addEventListener('change', (ev) => {
        renderizarAbaAuditoria(container, ev.target.value);
    });

    document.getElementById('form-admin-operador')?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        try {
            const resp = await apiPost('/api/v1/admin/usuarios/operadores', {
                email: document.getElementById('adm-op-email').value.trim(),
                senha: document.getElementById('adm-op-senha').value,
                perfis: [document.getElementById('adm-op-perfil').value]
            });
            dependenciasUi.exibirToast(`Operador ${resp.email} criado com sucesso!`, 'success');
            await renderizarAbaAuditoria(container, tipoFiltro);
        } catch (erro) {
            dependenciasUi.exibirToast(erro?.mensagem || 'Erro ao criar operador.', 'error');
        }
    });
}

/**
 * Aba 6: Relatórios Gerenciais Consolidados do Módulo 3 (somente ADMIN).
 * @param {HTMLElement} container Elemento destino
 */
async function renderizarAbaRelatorios(container) {
    const [vendas, ticket, maisVendidos, cancelamentos] = await Promise.all([
        apiGet('/api/v1/admin/relatorios/vendas-consolidadas'),
        apiGet('/api/v1/admin/relatorios/ticket-medio'),
        apiGet('/api/v1/admin/relatorios/produtos-mais-vendidos?limite=10'),
        apiGet('/api/v1/admin/relatorios/cancelamentos')
    ]);

    container.innerHTML = `
        <div class="stack-md">
            <div class="kpi-grid">
                <div class="kpi-card">
                    <span>Pedidos Faturados</span>
                    <strong>${escapeHtml(vendas.quantidadePedidosFaturados ?? 0)}</strong>
                </div>
                <div class="kpi-card">
                    <span>Faturamento Bruto</span>
                    <strong>${escapeHtml(formatarMoeda(vendas.faturamentoBruto))}</strong>
                </div>
                <div class="kpi-card">
                    <span>Descontos Concedidos</span>
                    <strong>${escapeHtml(formatarMoeda(vendas.totalDescontosConcedidos))}</strong>
                </div>
                <div class="kpi-card">
                    <span>Faturamento Líquido</span>
                    <strong>${escapeHtml(formatarMoeda(vendas.faturamentoLiquido))}</strong>
                </div>
                <div class="kpi-card">
                    <span>Ticket Médio / Pedido</span>
                    <strong>${escapeHtml(formatarMoeda(ticket.ticketMedioPorPedido))}</strong>
                </div>
                <div class="kpi-card">
                    <span>Taxa Cancel./Recusa</span>
                    <strong>${escapeHtml(cancelamentos.taxaCancelamentoPercentual ?? 0)}%</strong>
                </div>
            </div>

            <div class="admin-grid-2">
                <div class="card-inset">
                    <h3>Produtos Mais Vendidos (Top 10)</h3>
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead><tr><th>SKU</th><th>Item</th><th>Qtd</th><th>Receita Líq.</th></tr></thead>
                            <tbody>
                                ${(Array.isArray(maisVendidos) ? maisVendidos : []).map((it) => `
                                    <tr>
                                        <td class="mono">${escapeHtml(it.sku)}</td>
                                        <td>${escapeHtml(it.nome)}</td>
                                        <td><strong>${escapeHtml(it.quantidadeVendida)}</strong></td>
                                        <td>${escapeHtml(formatarMoeda(it.receitaLiquida))}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="card-inset stack-md">
                    <div>
                        <h3>Descontos por Cupom</h3>
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead><tr><th>Cupom</th><th>Pedidos</th><th>Total Abatido</th></tr></thead>
                                <tbody>
                                    ${(vendas.descontosPorCupom || []).map((c) => `
                                        <tr>
                                            <td class="mono">${escapeHtml(c.cupom || 'Sem Cupom')}</td>
                                            <td>${escapeHtml(c.quantidadePedidos)}</td>
                                            <td>${escapeHtml(formatarMoeda(c.totalDescontos))}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div>
                        <h3>Motivos de Cancelamento / Recusa</h3>
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead><tr><th>Motivo</th><th>Ocorrências</th></tr></thead>
                                <tbody>
                                    ${(cancelamentos.motivosMaisFrequentes || []).map((m) => `
                                        <tr>
                                            <td>${escapeHtml(m.motivo || 'Não informado')}</td>
                                            <td>${escapeHtml(m.quantidade)}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Conecta o clique nas abas administrativas RBAC e sincroniza mudanças de autenticação.
 * @param {typeof dependenciasUi} ui Helpers compartilhados de main.js
 */
export function inicializarModuloAdmin(ui) {
    dependenciasUi = { ...dependenciasUi, ...ui };

    document.getElementById('admin-abas-nav')?.addEventListener('click', async (ev) => {
        const btn = ev.target.closest('[data-admin-tab]');
        if (!btn) return;
        const abaId = btn.getAttribute('data-admin-tab');
        if (abaId) {
            atualizarEstado({ abaAdminAtiva: abaId }, 'ADMIN_ABA');
            await carregarPainelAdmin();
        }
    });

    assinarEstado((estado, evento) => {
        if (evento === 'AUTH' && estado.visaoAtiva === 'admin') {
            carregarPainelAdmin();
        }
    });
}
