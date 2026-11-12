-- ============================================================================
-- Migração V3: Módulo 3 (Integrante 3 — Henrique Sandrini)
-- Checkout, Pedidos com Snapshot Imutável, Ciclo de Vida (State),
-- Pagamentos com Idempotência Estrita e Frete Logístico
-- ============================================================================

CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    numero_pedido VARCHAR(40) NOT NULL UNIQUE,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    email_cliente VARCHAR(180) NOT NULL,
    nome_cliente VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    metodo_pagamento VARCHAR(30) NOT NULL,
    modalidade_frete VARCHAR(50) NOT NULL,
    prazo_entrega_dias INT NOT NULL,
    codigo_rastreio VARCHAR(60),
    chave_operacao_estoque VARCHAR(120) NOT NULL UNIQUE,
    cupom_utilizado VARCHAR(60),
    promocoes_aplicadas VARCHAR(1000),
    subtotal_bruto NUMERIC(15, 2) NOT NULL,
    total_descontos NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_frete NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    frete_gratis BOOLEAN NOT NULL DEFAULT FALSE,
    valor_total_liquido NUMERIC(15, 2) NOT NULL,
    motivo_cancelamento VARCHAR(255),
    endereco_apelido VARCHAR(60),
    endereco_cep VARCHAR(8) NOT NULL,
    endereco_logradouro VARCHAR(180) NOT NULL,
    endereco_numero VARCHAR(30) NOT NULL,
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100) NOT NULL,
    endereco_cidade VARCHAR(100) NOT NULL,
    endereco_uf VARCHAR(2) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE itens_pedido_snapshot (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    tipo_item VARCHAR(40) NOT NULL,
    preco_unitario_tabela NUMERIC(15, 2) NOT NULL,
    quantidade INT NOT NULL,
    desconto_unitario NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    subtotal_liquido NUMERIC(15, 2) NOT NULL,
    peso_total_gramas INT NOT NULL
);

CREATE TABLE transacoes_pagamento (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    chave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
    metodo VARCHAR(30) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    identificador_gateway VARCHAR(120),
    comprovante_ou_qrcode TEXT,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE registro_idempotencia_pagamento (
    chave_operacao VARCHAR(120) PRIMARY KEY,
    status_resposta VARCHAR(40) NOT NULL,
    payload_resposta_json TEXT NOT NULL,
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pedidos_cliente ON pedidos(cliente_id);
CREATE INDEX idx_pedidos_status ON pedidos(status);
CREATE INDEX idx_itens_pedido_sku ON itens_pedido_snapshot(sku);
