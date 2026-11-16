/**
 * Store reativa central da loja web (Padrão Observer + Singleton em memória).
 * Gerencia sessão anônima persistente (localStorage), sessão autenticada sem popup (sessionStorage),
 * configurações da loja e helpers puros compartilhados de formatação/segurança XSS.
 */

const CHAVE_SESSAO_CARRINHO = 'ecommerce_sessao_carrinho';
const CHAVE_AUTH_SESSAO = 'ecommerce_auth';
const CHAVE_PREFERENCIA_TEMA = 'fatec_store_tema_v1';

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

/**
 * Resolve o tema visual inicial da loja respeitando precedência: escolha manual salva > sistema > claro.
 *
 * Objetivo Funcional: Impedir discrepâncias de tema e garantir aderência às preferências do dispositivo na 1ª visita.
 * Papel Arquitetural: Inicializador de preferência visual do Singleton de estado global.
 *
 * @returns {{tema: 'light'|'dark', origem: 'manual'|'sistema'}} Tema resolvido e sua origem
 */
function resolverPreferenciaInicialTema() {
    try {
        const salvo = localStorage.getItem(CHAVE_PREFERENCIA_TEMA);
        if (salvo === 'light' || salvo === 'dark') {
            return { tema: salvo, origem: 'manual' };
        }
    } catch {
        // Ignora bloqueios de acesso a localStorage
    }
    const prefereEscuro = typeof window !== 'undefined'
        && typeof window.matchMedia === 'function'
        && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return { tema: prefereEscuro ? 'dark' : 'light', origem: 'sistema' };
}

/**
 * Aplica o atributo semântico {@code data-theme} no elemento raiz do documento.
 *
 * Objetivo Funcional: Ativar imediatamente o conjunto de tokens claro ou escuro em toda a árvore DOM.
 * Papel Arquitetural: Mutador de tema na fronteira de apresentação da SPA.
 *
 * @param {'light'|'dark'} tema Tema a aplicar
 */
function aplicarTemaNoDocumento(tema) {
    if (typeof document !== 'undefined' && document.documentElement) {
        document.documentElement.dataset.theme = tema;
    }
}

const temaInicial = resolverPreferenciaInicialTema();
aplicarTemaNoDocumento(temaInicial.tema);

const estadoGlobal = {
    visaoAtiva: 'vitrine',
    tema: temaInicial.tema,
    origemTema: temaInicial.origem,
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

if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    const mediaEscura = window.matchMedia('(prefers-color-scheme: dark)');
    const aoMudarTemaSistema = (evento) => {
        if (estadoGlobal.origemTema !== 'sistema') {
            return;
        }
        const proximo = evento.matches ? 'dark' : 'light';
        if (estadoGlobal.tema === proximo) {
            return;
        }
        estadoGlobal.tema = proximo;
        aplicarTemaNoDocumento(proximo);
        notificarMudancaEstado('TEMA');
    };
    if (typeof mediaEscura.addEventListener === 'function') {
        mediaEscura.addEventListener('change', aoMudarTemaSistema);
    } else if (typeof mediaEscura.addListener === 'function') {
        mediaEscura.addListener(aoMudarTemaSistema);
    }
}

/**
 * Retorna o tema visual ativo no momento ('light' ou 'dark').
 *
 * Objetivo Funcional: Permitir inspeção reativa do tema atual pelos módulos de interface.
 * Papel Arquitetural: Leitor do estado de tema gerenciado pelo Observer.
 *
 * @returns {'light'|'dark'} Tema vigente
 */
export function obterTemaAtual() {
    return estadoGlobal.tema === 'dark' ? 'dark' : 'light';
}

/**
 * Alterna diretamente o tema entre Modo Claro e Modo Escuro em 1 clique, persistindo a escolha manual.
 *
 * Objetivo Funcional: Atender à alternância imediata e persistente entre visitas sem recarregar a página.
 * Papel Arquitetural: Comando de transição de tema no Singleton reativo (dispara evento 'TEMA').
 *
 * @returns {'light'|'dark'} Novo tema ativo após alternância
 */
export function alternarTema() {
    const proximoTema = obterTemaAtual() === 'dark' ? 'light' : 'dark';
    estadoGlobal.tema = proximoTema;
    estadoGlobal.origemTema = 'manual';
    try {
        localStorage.setItem(CHAVE_PREFERENCIA_TEMA, proximoTema);
    } catch {
        // Prossegue em memória se localStorage estiver indisponível
    }
    aplicarTemaNoDocumento(proximoTema);
    notificarMudancaEstado('TEMA');
    return proximoTema;
}

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
