/**
 * Cliente HTTP central da aplicação (Padrão Facade sobre Fetch API).
 * Injeta automaticamente Authorization Basic (quando autenticado) e X-Sessao-Carrinho
 * para persistência do carrinho de visitante anônimo sem disparar popup nativo no navegador.
 */

import { gerarOuObterSessaoCarrinho, limparAutenticacao, obterEstado } from './store.js';

let callbackNaoAutorizado = null;

/**
 * Erro estruturado retornado pelas chamadas REST dos Módulos 1, 2, 3 e 4.
 */
export class ErroApi extends Error {
    /**
     * @param {number} status Código HTTP da resposta
     * @param {string} codigo Código de erro da API ou padrão
     * @param {string} mensagem Mensagem legível para exibição em interface
     * @param {unknown} detalhes Payload bruto completo retornado pelo servidor
     */
    constructor(status, codigo, mensagem, detalhes = null) {
        super(mensagem);
        this.name = 'ErroApi';
        this.status = status;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.detalhes = detalhes;
    }
}

/**
 * Registra o manipulador global de respostas HTTP 401 (sessão ausente ou expirada).
 * @param {(erro: ErroApi) => void} fn Função chamada para abrir o modal de login interno
 */
export function registrarHandlerNaoAutorizado(fn) {
    callbackNaoAutorizado = fn;
}

/**
 * Extrai a mensagem mais informativa possível dos diferentes formatos de erro do back-end.
 * @param {any} payload Objeto JSON retornado pelo back-end
 * @param {number} status Código HTTP
 * @returns {string}
 */
function extrairMensagemErro(payload, status) {
    if (!payload) {
        return `Erro HTTP ${status} ao comunicar com o servidor.`;
    }
    if (typeof payload === 'string') {
        return payload;
    }
    if (Array.isArray(payload.errosValidacao) && payload.errosValidacao.length > 0) {
        return payload.errosValidacao.map((e) => `${e.campo || 'campo'}: ${e.mensagem}`).join(' | ');
    }
    if (payload.campos && typeof payload.campos === 'object') {
        const lista = Object.entries(payload.campos).map(([campo, msg]) => `${campo}: ${msg}`);
        if (lista.length > 0) {
            return lista.join(' | ');
        }
    }
    return payload.mensagem || payload.message || payload.error || `Falha na operação (HTTP ${status}).`;
}

/**
 * Executa requisição HTTP contra a API Spring Boot aplicando headers padrão e tratamento de erros.
 * @param {string} caminho Caminho relativo (ex.: '/api/v1/catalogo/itens')
 * @param {{
 *   method?: string,
 *   body?: unknown,
 *   headers?: Record<string, string>,
 *   tokenBasicOverride?: string|null,
 *   ignorarAuth?: boolean,
 *   suprimirHandler401?: boolean
 * }} [opcoes]
 * @returns {Promise<any>}
 */
export async function requisicaoApi(caminho, opcoes = {}) {
    const estado = obterEstado();
    const metodo = (opcoes.method || 'GET').toUpperCase();
    const headers = {
        Accept: 'application/json',
        ...(opcoes.headers || {})
    };

    const sessaoCarrinho = estado.sessaoCarrinho || gerarOuObterSessaoCarrinho();
    if (sessaoCarrinho) {
        headers['X-Sessao-Carrinho'] = sessaoCarrinho;
    }

    if (!opcoes.ignorarAuth) {
        const token = opcoes.tokenBasicOverride ?? estado.auth?.tokenBasic;
        if (token) {
            headers.Authorization = `Basic ${token}`;
        }
    }

    const fetchInit = {
        method: metodo,
        headers,
        credentials: 'same-origin'
    };

    if (opcoes.body !== undefined) {
        headers['Content-Type'] = 'application/json';
        fetchInit.body = typeof opcoes.body === 'string' ? opcoes.body : JSON.stringify(opcoes.body);
    }

    let response;
    try {
        response = await fetch(caminho, fetchInit);
    } catch (erroRede) {
        throw new ErroApi(
            0,
            'ERRO_CONEXAO',
            'Não foi possível conectar ao servidor da loja. Verifique se o back-end está em execução.',
            erroRede
        );
    }

    const textoResposta = await response.text();
    let payload = null;
    if (textoResposta) {
        try {
            payload = JSON.parse(textoResposta);
        } catch {
            payload = textoResposta;
        }
    }

    if (!response.ok) {
        const codigo = (payload && typeof payload === 'object' && (payload.codigo || payload.error))
            ? String(payload.codigo || payload.error)
            : `HTTP_${response.status}`;
        const mensagem = extrairMensagemErro(payload, response.status);
        const erro = new ErroApi(response.status, codigo, mensagem, payload);

        if (response.status === 401 && !opcoes.suprimirHandler401) {
            if (estado.auth?.autenticado) {
                limparAutenticacao();
            }
            if (typeof callbackNaoAutorizado === 'function') {
                callbackNaoAutorizado(erro);
            }
        }
        throw erro;
    }

    return payload;
}

/** Atalho HTTP GET */
export function apiGet(caminho, opcoes = {}) {
    return requisicaoApi(caminho, { ...opcoes, method: 'GET' });
}

/** Atalho HTTP POST */
export function apiPost(caminho, body = undefined, opcoes = {}) {
    return requisicaoApi(caminho, { ...opcoes, method: 'POST', body });
}

/** Atalho HTTP PUT */
export function apiPut(caminho, body = undefined, opcoes = {}) {
    return requisicaoApi(caminho, { ...opcoes, method: 'PUT', body });
}

/** Atalho HTTP PATCH */
export function apiPatch(caminho, body = undefined, opcoes = {}) {
    return requisicaoApi(caminho, { ...opcoes, method: 'PATCH', body });
}

/** Atalho HTTP DELETE */
export function apiDelete(caminho, opcoes = {}) {
    return requisicaoApi(caminho, { ...opcoes, method: 'DELETE' });
}
