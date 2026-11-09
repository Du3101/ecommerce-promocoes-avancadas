package br.edu.fatec.ecommerce.modulo2.cupons.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.ConflitoDeEstadoOuDuplicidadeException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomNulo;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.TipoDescontoCupom;
import br.edu.fatec.ecommerce.modulo2.cupons.infraestrutura.CupomPromocionalJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de aplicação para gestão e validação de cupons promocionais.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Coordena o ciclo de vida dos cupons de desconto, consumindo o padrão <i>Null Object</i>
 * ({@link CupomNulo}) para respostas seguras e controlando cotas de utilização concorrente.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Service
@Transactional
public class ServicoCuponsPromocionais {

    private final CupomPromocionalJpaRepository cupomRepository;

    /**
     * Construtor com injeção do repositório.
     *
     * @param cupomRepository Repositório JPA de cupons.
     */
    public ServicoCuponsPromocionais(CupomPromocionalJpaRepository cupomRepository) {
        this.cupomRepository = Objects.requireNonNull(cupomRepository, "cupomRepository não pode ser nulo");
    }

    /**
     * Cadastra um novo cupom promocional no sistema.
     *
     * <p>Objetivo funcional: Criação administrativa de código de desconto.
     *
     * @param codigo Código alfanumérico único.
     * @param tipo Desconto percentual ou fixo.
     * @param valor Valor ou alíquota.
     * @param valorMinimo Subtotal mínimo para ativação.
     * @param qtdMaxUsos Limite de utilizações.
     * @param expiraEm Data de expiração.
     * @param cumulativo Se acumula com outras promoções.
     * @return Cupom persistido.
     */
    public CupomPromocional criarCupom(
            String codigo,
            TipoDescontoCupom tipo,
            BigDecimal valor,
            BigDecimal valorMinimo,
            int qtdMaxUsos,
            Instant expiraEm,
            boolean cumulativo
    ) {
        Objects.requireNonNull(codigo, "Código do cupom não pode ser nulo");
        String codigoNormalizado = codigo.trim().toUpperCase();

        if (cupomRepository.findByCodigoIgnoreCase(codigoNormalizado).isPresent()) {
            throw new ConflitoDeEstadoOuDuplicidadeException("Já existe um cupom cadastrado com o código '" + codigoNormalizado + "'.");
        }

        CupomPromocional cupom = new CupomPromocional(
                UUID.randomUUID(),
                codigoNormalizado,
                tipo,
                valor,
                valorMinimo,
                qtdMaxUsos,
                expiraEm,
                cumulativo,
                true
        );

        return cupomRepository.save(cupom);
    }

    /**
     * Lista todos os cupons cadastrados.
     *
     * @return Lista de cupons.
     */
    @Transactional(readOnly = true)
    public List<CupomPromocional> listarCupons() {
        return cupomRepository.findAll();
    }

    /**
     * Busca um cupom pelo código ou retorna o Null Object caso não exista.
     *
     * <p>Papel no padrão: Uso do Null Object {@link CupomNulo}.
     *
     * @param codigo Código procurado.
     * @return CupomPromocional real ou CupomNulo.
     */
    @Transactional(readOnly = true)
    public CupomPromocional buscarOuRetornarNulo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return CupomNulo.obterInstancia();
        }
        return cupomRepository.findByCodigoIgnoreCase(codigo.trim())
                .orElse(CupomNulo.obterInstancia());
    }

    /**
     * Confirma definitivamente o consumo de uma cota de uso do cupom (ex.: no checkout).
     *
     * @param codigo Código do cupom utilizado.
     */
    public void registrarUsoEfetivo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        Optional<CupomPromocional> cupomOpt = cupomRepository.findByCodigoIgnoreCase(codigo.trim());
        if (cupomOpt.isPresent()) {
            CupomPromocional cupom = cupomOpt.get();
            if (!cupom.isDisponivelParaUso(Instant.now(), BigDecimal.ZERO)) {
                throw new RegraNegocioModulo1Exception("Cupom '" + codigo + "' não pode mais ser utilizado (esgotado ou expirado).");
            }
            cupom.registrarUso();
            cupomRepository.save(cupom);
        }
    }
}

