/**
 * Store reativa central da loja web (Padrão Observer + Singleton em memória).
 * Gerencia sessão anônima persistente (localStorage), sessão autenticada sem popup (sessionStorage),
 * configurações da loja e helpers puros compartilhados de formatação/segurança XSS.
 */

const CHAVE_SESSAO_CARRINHO = 'ecommerce_sessao_carrinho';
const CHAVE_AUTH_SESSAO = 'ecommerce_auth';

/**
 * Mapeamento declarativo dos estados do pedido (Módulo 3) para rótulos e classes CSS.
 */
export const MAPA_STATUS_PEDIDO = Object.freeze({
    CRIADO: { label: 'Criado', cssClass: 'status-CRIADO' },
    AGUARDANDO_PAGAMENTO: { label: 'Aguardando Pagamento', cssClass: 'status-AGUARDANDO_PAGAMENTO' },
    PAGO: { label: 'Pago', cssClass: 'status-PAGO' },
    EM_PREPARACAO: { label: 'Em Preparação', cssClass: 'status-EM_PREPARACAO' },
    ENVIADO: { label: 'Enviado / Em Trânsito', cssClass: 'status-ENVIADO' },
    ENTREGUE: { label: 'Entregue', cssClass: 'status-ENTREGUE' },
    CANCELADO: { label: 'Cancelado', cssClass: 'status-CANCELADO' },
    PAGAMENTO_RECUSADO: { label: 'Pagamento Recusado', cssClass: 'status-PAGAMENTO_RECUSADO' }
});

/**
 * Mapeamento declarativo dos tipos de mecânica promocional (Módulo 2).
 */
export const MAPA_MECANICAS_PROMOCAO = Object.freeze({
    LEVE_X_PAGUE_Y: { label: 'Leve X e Pague Y', selo: 'Compre + Ganhe' },
    PERCENTUAL_CONDICIONAL: { label: 'Desconto Percentual', selo: '% Progressivo' },
    VALOR_FIXO_META_CARRINHO: { label: 'Bônus em Valor Fixo', selo: 'Meta de Carrinho' },
    COMBO_CRUZADO_CASADO: { label: 'Compre Junto', selo: 'Oferta Casada' }
});

/**
 * Perfis administrativos que habilitam o Painel Administrativo RBAC.
 */
export const PERFIS_ADMINISTRATIVOS = Object.freeze([
    'ADMIN',
    'OPERADOR_COMERCIAL',
    'RESPONSAVEL_ESTOQUE',
    'RESPONSAVEL_PROMOCOES'
]);

const assinantes = new Set();

/**
 * Gera um identificador UUID v4 seguro usando Web Crypto API com fallback determinístico de formato.
 * @returns {string} UUID v4
 */
function gerarUuidV4() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.floor(Math.random() * 16);
        const v = c === 'x' ? r : ((r & 0x3) | 0x8);
        return v.toString(16);
    });
}

/**
 * Recupera ou inicializa o identificador de sessão anônima do visitante em localStorage.
 * @returns {string} UUID persistido da sessão de carrinho
 */
export function gerarOuObterSessaoCarrinho() {
    try {
        let sessao = localStorage.getItem(CHAVE_SESSAO_CARRINHO);
        if (!sessao || sessao.trim().length < 8) {
            sessao = gerarUuidV4();
            localStorage.setItem(CHAVE_SESSAO_CARRINHO, sessao);
        }
        return sessao;
    } catch {
        return 'sessao-visitante-memoria';
    }
}

/**
 * Carrega credenciais de sessão (sessionStorage) caso o usuário tenha recarregado a página.
 * @returns {{autenticado: boolean, usuarioId: string|null, email: string|null, perfis: string[], tokenBasic: string|null}}
 */
function carregarAuthSessao() {
    try {
        const bruto = sessionStorage.getItem(CHAVE_AUTH_SESSAO);
        if (!bruto) {
            return { autenticado: false, usuarioId: null, email: null, perfis: [], tokenBasic: null };
        }
        const parsed = JSON.parse(bruto);
        if (parsed && parsed.email && parsed.tokenBasic) {
            return {
                autenticado: true,
                usuarioId: parsed.usuarioId || null,
                email: parsed.email,
                perfis: Array.isArray(parsed.perfis) ? parsed.perfis : [],
                tokenBasic: parsed.tokenBasic
            };
        }
    } catch {
        // Ignora dados corrompidos de sessionStorage
    }
    return { autenticado: false, usuarioId: null, email: null, perfis: [], tokenBasic: null };
}

const estadoGlobal = {
    visaoAtiva: 'vitrine',
    configuracaoLoja: {
        modoDemonstracaoAtivo: false,
        freteGratisValorMinimo: 250.0,
        freteGratisPesoMaximoGramas: 5000
    },
    auth: carregarAuthSessao(),
    sessaoCarrinho: gerarOuObterSessaoCarrinho(),
    categoriasArvore: [],
    catalogoItens: [],
    promocoesAtivas: [],
    categoriaSelecionadaId: null,
    categoriaSelecionadaNome: 'Todas as categorias',
    termoBusca: '',
    carrinho: null,
    clientePerfil: null,
    meusPedidos: [],
    freteCotacaoAtual: null,
    modalidadeFreteSelecionada: 'CORREIOS_PAC',
    ultimoPedidoCheckout: null,
    abaAdminAtiva: null
};

/**
 * Registra uma função observadora invocada sempre que o estado compartilhado muda.
 * @param {(estado: typeof estadoGlobal, evento: string) => void} observador Callback de atualização
 * @returns {() => void} Função para cancelar inscrição
 */
export function assinarEstado(observador) {
    assinantes.add(observador);
    return () => assinantes.delete(observador);
}

/**
 * Retorna a referência de leitura do estado global atual.
 * @returns {typeof estadoGlobal}
 */
export function obterEstado() {
    return estadoGlobal;
}

/**
 * Notifica todos os observadores inscritos com o evento disparado.
 * @param {string} evento Identificador semântico da mudança de estado
 */
export function notificarMudancaEstado(evento = 'GERAL') {
    for (const observador of assinantes) {
        try {
            observador(estadoGlobal, evento);
        } catch (erro) {
            console.error('[Store] Erro em observador de estado:', erro);
        }
    }
}

/**
 * Atualiza parcialmente propriedades do estado global e notifica observadores.
 * @param {Partial<typeof estadoGlobal>} parcial Campos a mesclar no estado
 * @param {string} evento Nome do evento emitido
 */
export function atualizarEstado(parcial, evento = 'GERAL') {
    Object.assign(estadoGlobal, parcial);
    notificarMudancaEstado(evento);
}

/**
 * Codifica par e-mail/senha em Base64 UTF-8 seguro para o header HTTP Authorization Basic.
 * @param {string} email E-mail do usuário
 * @param {string} senha Senha em texto plano durante envio TLS/local
 * @returns {string} Token Base64
 */
export function codificarBasicAuth(email, senha) {
    const bytes = new TextEncoder().encode(`${email}:${senha}`);
    let binario = '';
    for (const byte of bytes) {
        binario += String.fromCharCode(byte);
    }
    return btoa(binario);
}

/**
 * Persiste autenticação em memória e sessionStorage e notifica assinantes.
 * @param {{usuarioId: string|null, email: string, perfis: string[], tokenBasic: string}} dados
 */
export function definirAutenticacao(dados) {
    const authNormalizada = {
        autenticado: true,
        usuarioId: dados.usuarioId || null,
        email: dados.email,
        perfis: Array.isArray(dados.perfis) ? dados.perfis : [],
        tokenBasic: dados.tokenBasic
    };
    estadoGlobal.auth = authNormalizada;
    try {
        sessionStorage.setItem(CHAVE_AUTH_SESSAO, JSON.stringify(authNormalizada));
    } catch {
        // Ignora indisponibilidade eventual de sessionStorage
    }
    notificarMudancaEstado('AUTH');
}

/**
 * Encerra sessão autenticada mantendo ou renovando o identificador anônimo de carrinho.
 */
export function limparAutenticacao() {
    estadoGlobal.auth = {
        autenticado: false,
        usuarioId: null,
        email: null,
        perfis: [],
        tokenBasic: null
    };
    estadoGlobal.clientePerfil = null;
    estadoGlobal.meusPedidos = [];
    estadoGlobal.abaAdminAtiva = null;
    try {
        sessionStorage.removeItem(CHAVE_AUTH_SESSAO);
    } catch {
        // Ignora erro de storage
    }
    notificarMudancaEstado('AUTH');
}

/**
 * Verifica se o usuário autenticado possui um perfil RBAC específico.
 * @param {string} perfil Nome exato do PerfilUsuario
 * @returns {boolean}
 */
export function usuarioPossuiPerfil(perfil) {
    return Boolean(estadoGlobal.auth.autenticado && estadoGlobal.auth.perfis.includes(perfil));
}

/**
 * Verifica se o usuário possui ao menos um dos perfis informados.
 * @param {string[]} perfisPermitidos Lista de perfis aceitos
 * @returns {boolean}
 */
export function usuarioPossuiAlgumPerfil(perfisPermitidos) {
    if (!estadoGlobal.auth.autenticado) {
        return false;
    }
    return perfisPermitidos.some((p) => estadoGlobal.auth.perfis.includes(p));
}

/**
 * Escapa caracteres HTML sensíveis antes de qualquer inserção via innerHTML (prevenção contra XSS).
 * @param {unknown} valor Valor a escapar
 * @returns {string} Texto seguro para renderização em HTML
 */
export function escapeHtml(valor) {
    if (valor === null || valor === undefined) {
        return '';
    }
    return String(valor)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

const formatadorBrl = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
});

/**
 * Formata valores numéricos em moeda brasileira (R$).
 * @param {number|string|null|undefined} valor Valor decimal
 * @returns {string}
 */
export function formatarMoeda(valor) {
    const numero = Number(valor ?? 0);
    return Number.isFinite(numero) ? formatadorBrl.format(numero) : 'R$ 0,00';
}

/**
 * Formata timestamps ISO-8601 para leitura amigável em pt-BR.
 * @param {string|null|undefined} isoString Data/hora ISO
 * @returns {string}
 */
export function formatarDataHora(isoString) {
    if (!isoString) {
        return '-';
    }
    try {
        const data = new Date(isoString);
        if (Number.isNaN(data.getTime())) {
            return String(isoString);
        }
        return data.toLocaleString('pt-BR', {
            dateStyle: 'short',
            timeStyle: 'short'
        });
    } catch {
        return String(isoString);
    }
}

/**
 * Converte gramas em texto legível (g ou kg).
 * @param {number|null|undefined} gramas Peso em gramas
 * @returns {string}
 */
export function formatarPeso(gramas) {
    const g = Number(gramas ?? 0);
    if (!Number.isFinite(g) || g <= 0) {
        return '0 g';
    }
    if (g >= 1000) {
        return `${(g / 1000).toFixed(2).replace('.', ',')} kg`;
    }
    return `${g} g`;
}

/**
 * Obtém rótulo e classe CSS correspondente ao status de pedido informado.
 * @param {string} status StatusPedido
 * @returns {{label: string, cssClass: string}}
 */
export function obterMetaStatusPedido(status) {
    return MAPA_STATUS_PEDIDO[status] || {
        label: status || 'Desconhecido',
        cssClass: 'status-criado'
    };
}
