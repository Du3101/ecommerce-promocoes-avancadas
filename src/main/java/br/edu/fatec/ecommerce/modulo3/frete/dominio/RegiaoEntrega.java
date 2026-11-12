package br.edu.fatec.ecommerce.modulo3.frete.dominio;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import java.math.BigDecimal;

/**
 * Faixas regionais de entrega derivadas do primeiro dígito do CEP de destino.
 *
 * <p>No Brasil, o primeiro dígito do CEP identifica a região postal (0–1 São Paulo, 2–3 RJ/ES/MG,
 * 4–5 Nordeste litorâneo, 6 Norte e parte do Nordeste, 7 Centro-Oeste, 8–9 Sul). Quanto mais longe do centro de
 * distribuição (São Paulo), maior o multiplicador de preço e o prazo adicional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> tabela de dados usada pelas "APIs externas" simuladas das
 * transportadoras. A resolução {@link #deCep(String)} usa um vetor indexado pelo dígito, sem cadeia de {@code if}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum RegiaoEntrega {

    /** CEPs 0xxxx e 1xxxx — mesma região do centro de distribuição. */
    SAO_PAULO("São Paulo", new BigDecimal("1.00"), 0),

    /** CEPs 2xxxx e 3xxxx — RJ, ES e MG. */
    SUDESTE("Sudeste (RJ/ES/MG)", new BigDecimal("1.15"), 1),

    /** CEPs 4xxxx e 5xxxx — BA, SE, PE, AL, PB, RN. */
    NORDESTE("Nordeste", new BigDecimal("1.40"), 3),

    /** CEPs 6xxxx — CE, PI, MA e Região Norte. */
    NORTE("Norte e Nordeste setentrional", new BigDecimal("1.70"), 5),

    /** CEPs 7xxxx — DF, GO, TO, MT, MS, RO, AC. */
    CENTRO_OESTE("Centro-Oeste", new BigDecimal("1.30"), 2),

    /** CEPs 8xxxx e 9xxxx — PR, SC e RS. */
    SUL("Sul", new BigDecimal("1.20"), 2);

    /** Mapeamento direto: posição = primeiro dígito do CEP. */
    private static final RegiaoEntrega[] POR_PRIMEIRO_DIGITO = {
        SAO_PAULO, SAO_PAULO, SUDESTE, SUDESTE, NORDESTE, NORDESTE, NORTE, CENTRO_OESTE, SUL, SUL
    };

    private final String descricao;
    private final BigDecimal fatorPreco;
    private final int diasAdicionais;

    /**
     * Construtor do enum.
     *
     * @param descricao nome legível da região
     * @param fatorPreco multiplicador aplicado ao preço base
     * @param diasAdicionais dias úteis somados ao prazo base
     */
    RegiaoEntrega(String descricao, BigDecimal fatorPreco, int diasAdicionais) {
        this.descricao = descricao;
        this.fatorPreco = fatorPreco;
        this.diasAdicionais = diasAdicionais;
    }

    /**
     * Resolve a região a partir do CEP (com ou sem máscara).
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> com validação do CEP.</p>
     *
     * @param cep CEP de destino
     * @return região correspondente
     * @throws RegraNegocioModulo3Exception se o CEP não tiver 8 dígitos
     */
    public static RegiaoEntrega deCep(String cep) {
        String somenteDigitos = normalizarCep(cep);
        return POR_PRIMEIRO_DIGITO[somenteDigitos.charAt(0) - '0'];
    }

    /**
     * Remove a máscara do CEP e valida que restaram exatamente 8 dígitos.
     *
     * @param cep CEP informado (ex.: {@code 01310-100})
     * @return CEP com 8 dígitos (ex.: {@code 01310100})
     * @throws RegraNegocioModulo3Exception se o CEP for inválido
     */
    public static String normalizarCep(String cep) {
        String somenteDigitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (somenteDigitos.length() != 8) {
            throw new RegraNegocioModulo3Exception("CEP de destino inválido: informe 8 dígitos (ex.: 01310-100).");
        }
        return somenteDigitos;
    }

    /**
     * Formata um CEP de 8 dígitos com hífen.
     *
     * @param cep CEP com ou sem máscara
     * @return CEP no formato {@code 00000-000}
     */
    public static String formatarCep(String cep) {
        String digitos = normalizarCep(cep);
        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }

    /**
     * Retorna o nome legível da região.
     *
     * @return descrição
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o multiplicador de preço da região.
     *
     * @return fator de preço
     */
    public BigDecimal getFatorPreco() {
        return fatorPreco;
    }

    /**
     * Retorna os dias úteis adicionais da região.
     *
     * @return dias adicionais
     */
    public int getDiasAdicionais() {
        return diasAdicionais;
    }
}
