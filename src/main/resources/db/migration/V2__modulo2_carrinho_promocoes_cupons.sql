-- ============================================================================
-- Migração V2: Módulo 2 (Integrante 2 — Eduardo Juncker Toras)
-- Carrinho de Compras, Itens do Carrinho, Campanhas Promocionais Avançadas e Cupons
-- ============================================================================

CREATE TABLE carrinhos (
    id UUID PRIMARY KEY,
    cliente_id UUID REFERENCES clientes(id) ON DELETE SET NULL,
    sessao_id VARCHAR(100),
    cupom_codigo VARCHAR(50),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE itens_carrinho (
    id UUID PRIMARY KEY,
    carrinho_id UUID NOT NULL REFERENCES carrinhos(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    tipo_item VARCHAR(40) NOT NULL,
    preco_unitario NUMERIC(15, 2) NOT NULL,
    quantidade INT NOT NULL,
    peso_gramas INT NOT NULL,
    categoria_id UUID
);

CREATE INDEX idx_itens_carrinho_carrinho ON itens_carrinho(carrinho_id);
CREATE INDEX idx_itens_carrinho_sku ON itens_carrinho(sku);

CREATE TABLE campanhas_promocionais (
    id UUID PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    descricao TEXT NOT NULL,
    tipo_mecanica VARCHAR(60) NOT NULL,
    parametros_json TEXT NOT NULL,
    prioridade INT NOT NULL DEFAULT 0,
    cumulativa BOOLEAN NOT NULL DEFAULT FALSE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    data_inicio TIMESTAMP WITH TIME ZONE,
    data_fim TIMESTAMP WITH TIME ZONE,
    arvore_elegibilidade_json TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_campanhas_ativa_prioridade ON campanhas_promocionais(ativa, prioridade DESC);

CREATE TABLE cupons_promocionais (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo_desconto VARCHAR(40) NOT NULL,
    valor_desconto NUMERIC(15, 2) NOT NULL,
    valor_minimo_carrinho NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    quantidade_maxima_usos INT NOT NULL DEFAULT 0,
    quantidade_usos_atuais INT NOT NULL DEFAULT 0,
    expira_em TIMESTAMP WITH TIME ZONE,
    cumulativo_com_promocoes BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_cupons_codigo ON cupons_promocionais(codigo);
