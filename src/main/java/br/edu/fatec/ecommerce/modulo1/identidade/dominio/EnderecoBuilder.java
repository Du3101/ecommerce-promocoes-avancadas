package br.edu.fatec.ecommerce.modulo1.identidade.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import java.util.Set;
import java.util.UUID;

/**
 * Construtor fluente e validado para objetos {@link Endereco}.
 *
 * <p><b>Design Pattern Aplicado: Builder (Padrão Criacional)</b></p>
 * <ul>
 *   <li><b>1. Qual problema de projeto existia?</b> Um endereço completo possui 9 atributos
 *   textuais (apelido, CEP, logradouro, número, complemento, bairro, cidade, UF) mais vínculos
 *   e flags. Um construtor direto com 8 strings seguidas causaria erros silenciosos por inversão
 *   de parâmetros (ex.: inverter bairro e cidade, ou CEP e número).</li>
 *   <li><b>2. Por que este padrão foi escolhido?</b> O padrão Builder permite configurar cada
 *   atributo por meio de métodos explícitos nomeados e concentra todas as validações de formato
 *   (CEP de 8 dígitos numéricos, UF brasileira válida, limites de tamanho) no método final
 *   {@link #construir()}.</li>
 *   <li><b>3. O que ficou mais simples de modificar?</b> Incluir novos campos opcionais de
 *   endereço (como ponto de referência ou coordenadas geográficas) exige apenas um novo método
 *   no Builder sem quebrar chamadas existentes.</li>
 * </ul>
 */
public final class EnderecoBuilder {

    /**
     * Conjunto oficial das 27 Unidades Federativas do Brasil para validação de região.
     */
    private static final Set<String> UFS_VALIDAS = Set.of(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
        "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
        "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private UUID clienteId;
    private String apelido;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private boolean selecionadoParaEntrega;

    /**
     * Inicia uma nova instância do Builder de endereço.
     *
     * <p>Objetivo Funcional: Criar um construtor passo a passo limpo.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> de entrada para o <b>Builder</b>.</p>
     *
     * @return nova instância de {@link EnderecoBuilder}
     */
    public static EnderecoBuilder novoBuilder() {
        return new EnderecoBuilder();
    }

    /**
     * Define o identificador do cliente proprietário do endereço.
     *
     * <p>Objetivo Funcional: Vincular o endereço ao cliente comprador.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param clienteId UUID do cliente
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder paraClienteId(UUID clienteId) {
        this.clienteId = clienteId;
        return this;
    }

    /**
     * Define o identificador do cliente proprietário do endereço (alias fluente).
     *
     * <p>Objetivo Funcional: Vincular o endereço ao cliente comprador.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param clienteId UUID do cliente
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder paraCliente(UUID clienteId) {
        return paraClienteId(clienteId);
    }

    /**
     * Define o apelido identificador do endereço (ex.: "Casa", "Trabalho").
     *
     * <p>Objetivo Funcional: Permitir identificação amigável do endereço na conta do cliente.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param apelido nome amigável (máx. 60 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comApelido(String apelido) {
        this.apelido = apelido;
        return this;
    }

    /**
     * Define o CEP do endereço (com ou sem hífen, normalizado para 8 dígitos).
     *
     * <p>Objetivo Funcional: Armazenar o código postal para cálculo de frete.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param cep CEP de 8 dígitos numéricos
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comCep(String cep) {
        this.cep = cep;
        return this;
    }

    /**
     * Define o logradouro (rua, avenida, praça) do endereço.
     *
     * <p>Objetivo Funcional: Registrar a via de entrega.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param logradouro nome da via (máx. 180 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comLogradouro(String logradouro) {
        this.logradouro = logradouro;
        return this;
    }

    /**
     * Define o número predial do endereço.
     *
     * <p>Objetivo Funcional: Registrar a numeração do imóvel ou "S/N".</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param numero número do imóvel (máx. 30 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comNumero(String numero) {
        this.numero = numero;
        return this;
    }

    /**
     * Define o complemento opcional do endereço (ex.: "Apto 42", "Bloco B").
     *
     * <p>Objetivo Funcional: Detalhar informações adicionais de localização.</p>
     * <p>Papel no Design Pattern: Etapa opcional de configuração fluente do <b>Builder</b>.</p>
     *
     * @param complemento texto opcional (máx. 100 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comComplemento(String complemento) {
        this.complemento = complemento;
        return this;
    }

    /**
     * Define o bairro do endereço.
     *
     * <p>Objetivo Funcional: Registrar o bairro de entrega.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param bairro nome do bairro (máx. 100 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comBairro(String bairro) {
        this.bairro = bairro;
        return this;
    }

    /**
     * Define a cidade (município) do endereço.
     *
     * <p>Objetivo Funcional: Registrar o município de entrega.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param cidade nome da cidade (máx. 100 caracteres)
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comCidade(String cidade) {
        this.cidade = cidade;
        return this;
    }

    /**
     * Define a sigla da Unidade Federativa (UF) de 2 letras.
     *
     * <p>Objetivo Funcional: Registrar o estado para cálculo de frete e elegibilidade regional.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param uf sigla de 2 letras (ex.: "SP", "RJ", "MG")
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder comUf(String uf) {
        this.uf = uf;
        return this;
    }

    /**
     * Define se o endereço já nasce marcado como selecionado para entrega.
     *
     * <p>Objetivo Funcional: Permitir seleção imediata ao cadastrar o endereço.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param selecionado {@code true} para marcar como endereço ativo de entrega
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder selecionadoParaEntrega(boolean selecionado) {
        this.selecionadoParaEntrega = selecionado;
        return this;
    }

    /**
     * Define se o endereço nasce marcado como selecionado para entrega (alias fluente).
     *
     * <p>Objetivo Funcional: Permitir seleção imediata ao cadastrar o endereço.</p>
     * <p>Papel no Design Pattern: Etapa de configuração fluente do <b>Builder</b>.</p>
     *
     * @param selecionado {@code true} para marcar como endereço ativo de entrega
     * @return o próprio {@link EnderecoBuilder}
     */
    public EnderecoBuilder marcarComoSelecionadoParaEntrega(boolean selecionado) {
        return selecionadoParaEntrega(selecionado);
    }

    /**
     * Valida todas as invariantes de domínio e constrói a instância final de {@link Endereco}.
     *
     * <p>Objetivo Funcional: Garantir que nenhum endereço seja criado com CEP inválido, UF
     * inexistente ou campos obrigatórios vazios.</p>
     * <p>Papel no Design Pattern: Método finalizador (<i>build</i>) do padrão <b>Builder</b>.</p>
     *
     * @return instância válida de {@link Endereco}
     * @throws RegraNegocioModulo1Exception caso qualquer campo obrigatório ou formato seja inválido
     */
    public Endereco construir() {
        if (this.clienteId == null) {
            throw new RegraNegocioModulo1Exception("O cliente proprietário do endereço é obrigatório.");
        }
        String apelidoLimpo = validarCampoObrigatorio(this.apelido, "Apelido do endereço", 60);
        String logradouroLimpo = validarCampoObrigatorio(this.logradouro, "Logradouro", 180);
        String numeroLimpo = validarCampoObrigatorio(this.numero, "Número", 30);
        String bairroLimpo = validarCampoObrigatorio(this.bairro, "Bairro", 100);
        String cidadeLimpa = validarCampoObrigatorio(this.cidade, "Cidade", 100);

        String cepSomenteDigitos = normalizarEValidarCep(this.cep);
        String ufNormalizada = normalizarEValidarUf(this.uf);
        String complementoLimpo = normalizarComplementoOpcional(this.complemento);

        return new Endereco(
            UUID.randomUUID(),
            this.clienteId,
            apelidoLimpo,
            cepSomenteDigitos,
            logradouroLimpo,
            numeroLimpo,
            complementoLimpo,
            bairroLimpo,
            cidadeLimpa,
            ufNormalizada,
            this.selecionadoParaEntrega
        );
    }

    /**
     * Valida se um campo textual obrigatório foi preenchido e respeita o tamanho máximo do banco.
     *
     * <p>Objetivo Funcional: Impedir textos vazios ou truncamento silencioso no banco.</p>
     * <p>Papel no Design Pattern: Método auxiliar privado de validação do <b>Builder</b>.</p>
     *
     * @param valor texto informado
     * @param nomeCampo nome legível do campo para mensagem de erro
     * @param tamanhoMaximo limite máximo de caracteres definido no modelo de dados
     * @return texto limpo sem espaços nas extremidades
     */
    private String validarCampoObrigatorio(String valor, String nomeCampo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioModulo1Exception(nomeCampo + " é obrigatório.");
        }
        String limpo = valor.trim();
        if (limpo.length() > tamanhoMaximo) {
            throw new RegraNegocioModulo1Exception(nomeCampo + " excede o limite de " + tamanhoMaximo + " caracteres.");
        }
        return limpo;
    }

    /**
     * Remove caracteres não numéricos e valida se o CEP contém exatamente 8 dígitos.
     *
     * <p>Objetivo Funcional: Padronizar o CEP em 8 dígitos numéricos conforme data-model.md.</p>
     * <p>Papel no Design Pattern: Validação de formato no <b>Builder</b>.</p>
     *
     * @param cepBruto CEP informado pelo cliente
     * @return CEP contendo exatamente 8 dígitos numéricos
     */
    private String normalizarEValidarCep(String cepBruto) {
        if (cepBruto == null || cepBruto.isBlank()) {
            throw new RegraNegocioModulo1Exception("O CEP do endereço é obrigatório.");
        }
        String digitos = cepBruto.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            throw new RegraNegocioModulo1Exception("O CEP deve conter exatamente 8 dígitos numéricos.");
        }
        return digitos;
    }

    /**
     * Normaliza a sigla da UF para 2 letras maiúsculas e verifica se pertence às 27 UFs brasileiras.
     *
     * <p>Objetivo Funcional: Garantir UF válida para regras de frete e promoções regionais.</p>
     * <p>Papel no Design Pattern: Validação de domínio no <b>Builder</b>.</p>
     *
     * @param ufBruta UF informada
     * @return sigla da UF de 2 letras maiúsculas
     */
    private String normalizarEValidarUf(String ufBruta) {
        if (ufBruta == null || ufBruta.isBlank()) {
            throw new RegraNegocioModulo1Exception("A UF (Estado) do endereço é obrigatória.");
        }
        String sigla = ufBruta.trim().toUpperCase();
        if (!UFS_VALIDAS.contains(sigla)) {
            throw new RegraNegocioModulo1Exception("A UF informada ('" + ufBruta + "') não é uma unidade federativa válida.");
        }
        return sigla;
    }

    /**
     * Limpa o complemento opcional e verifica o limite de 100 caracteres.
     *
     * <p>Objetivo Funcional: Tratar campo opcional do endereço.</p>
     * <p>Papel no Design Pattern: Normalização interna no <b>Builder</b>.</p>
     *
     * @param comp complemento informado
     * @return complemento limpo ou {@code null} se vazio
     */
    private String normalizarComplementoOpcional(String comp) {
        if (comp == null || comp.isBlank()) {
            return null;
        }
        String limpo = comp.trim();
        if (limpo.length() > 100) {
            throw new RegraNegocioModulo1Exception("O complemento do endereço excede o limite de 100 caracteres.");
        }
        return limpo;
    }
}

