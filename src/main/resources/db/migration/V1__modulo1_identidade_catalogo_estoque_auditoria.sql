-- ============================================================================
-- Migração V1: Módulo 1 (Integrante 1)
-- Identidade, Clientes/Endereços, Catálogo, Categorias, Variações, Combos,
-- Estoque Concorrente e Trilha de Auditoria Imutável
-- ============================================================================

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE usuario_perfis (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    perfil VARCHAR(40) NOT NULL,
    PRIMARY KEY (usuario_id, perfil)
);

CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    nome_completo VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    primeira_compra_concluida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE enderecos_cliente (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    apelido VARCHAR(60) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    logradouro VARCHAR(180) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(100),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    selecionado_para_entrega BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE categorias (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    categoria_pai_id UUID REFERENCES categorias(id),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE produtos (
    id UUID PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    descricao TEXT NOT NULL,
    marca VARCHAR(100) NOT NULL,
    preco_base NUMERIC(15, 2) NOT NULL,
    categoria_id UUID NOT NULL REFERENCES categorias(id),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE produto_atributos_especificos (
    produto_id UUID NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    chave_atributo VARCHAR(80) NOT NULL,
    valor_atributo VARCHAR(255) NOT NULL,
    PRIMARY KEY (produto_id, chave_atributo)
);

CREATE TABLE atributos_variacao_flyweight (
    id UUID PRIMARY KEY,
    tipo VARCHAR(40) NOT NULL,
    valor VARCHAR(80) NOT NULL,
    codigo_apresentacao VARCHAR(80) NOT NULL,
    CONSTRAINT uk_flyweight_tipo_valor UNIQUE (tipo, valor)
);

CREATE TABLE variacoes_produto (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    sku VARCHAR(64) NOT NULL UNIQUE,
    codigo_barras VARCHAR(64),
    nome_variacao VARCHAR(180) NOT NULL,
    adicional_preco NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    peso_gramas INTEGER NOT NULL,
    altura_cm NUMERIC(8, 2) NOT NULL,
    largura_cm NUMERIC(8, 2) NOT NULL,
    profundidade_cm NUMERIC(8, 2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE variacao_atributos_flyweight (
    variacao_id UUID NOT NULL REFERENCES variacoes_produto(id) ON DELETE CASCADE,
    flyweight_id UUID NOT NULL REFERENCES atributos_variacao_flyweight(id)
);

CREATE TABLE combos_kits (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    nome VARCHAR(180) NOT NULL,
    descricao TEXT NOT NULL,
    categoria_id UUID NOT NULL REFERENCES categorias(id),
    modo_preco VARCHAR(40) NOT NULL,
    percentual_desconto_kit NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    preco_fixo_opcional NUMERIC(15, 2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE combo_itens_componentes (
    id UUID PRIMARY KEY,
    combo_id UUID NOT NULL REFERENCES combos_kits(id) ON DELETE CASCADE,
    componente_sku VARCHAR(64) NOT NULL,
    quantidade_por_kit INTEGER NOT NULL
);

CREATE TABLE itens_estoque (
    sku VARCHAR(64) PRIMARY KEY,
    quantidade_disponivel INTEGER NOT NULL DEFAULT 0,
    quantidade_reservada INTEGER NOT NULL DEFAULT 0,
    versao_lock BIGINT NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE reservas_estoque (
    id UUID PRIMARY KEY,
    chave_operacao_pedido VARCHAR(100) NOT NULL UNIQUE,
    cliente_id UUID NOT NULL,
    estado VARCHAR(30) NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    finalizada_em TIMESTAMP WITH TIME ZONE
);

CREATE TABLE itens_reserva_estoque (
    id UUID PRIMARY KEY,
    reserva_id UUID NOT NULL REFERENCES reservas_estoque(id) ON DELETE CASCADE,
    sku VARCHAR(64) NOT NULL,
    quantidade_reservada INTEGER NOT NULL
);

CREATE TABLE movimentacoes_estoque (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    tipo_movimentacao VARCHAR(40) NOT NULL,
    quantidade INTEGER NOT NULL,
    saldo_disponivel_anterior INTEGER NOT NULL,
    saldo_disponivel_posterior INTEGER NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    autor_operacao VARCHAR(180) NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE registros_auditoria (
    id UUID PRIMARY KEY,
    usuario_responsavel_id UUID NOT NULL,
    email_responsavel VARCHAR(180) NOT NULL,
    perfil_responsavel VARCHAR(40) NOT NULL,
    tipo_operacao VARCHAR(60) NOT NULL,
    tipo_entidade_alvo VARCHAR(80) NOT NULL,
    identificador_alvo VARCHAR(100) NOT NULL,
    valor_anterior_resumo TEXT NOT NULL,
    valor_novo_resumo TEXT NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL
);
