package br.edu.fatec.ecommerce.modulo1.auditoria.aplicacao;

import br.edu.fatec.ecommerce.compartilhado.excecao.AcessoNegadoModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.RegistroAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.dominio.TipoOperacaoAuditoria;
import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.ContextoSegurancaUsuario;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.PerfilUsuario;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação para consulta filtrada da trilha de auditoria administrativa (FR-012).
 *
 * <p>Restrito exclusivamente ao perfil {@link PerfilUsuario#ADMIN}.</p>
 */
@Service
public class ServicoConsultaAuditoria {

    private final RegistroAuditoriaJpaRepository auditoriaRepository;

    /**
     * Construtor com injeção explícita do repositório JPA de auditoria.
     *
     * <p>Objetivo Funcional: Permitir consulta aos eventos auditados pelos Proxies.</p>
     * <p>Papel no Design Pattern: Serviço de consulta da trilha alimentada pelo padrão <b>Proxy</b>.</p>
     *
     * @param auditoriaRepository repositório de registros de auditoria
     */
    public ServicoConsultaAuditoria(RegistroAuditoriaJpaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Consulta os registros da trilha de auditoria aplicando filtros opcionais por usuário,
     * tipo de operação e período temporal, ordenados do mais recente para o mais antigo (FR-012).
     *
     * <p>Objetivo Funcional: Permitir que o administrador audite quem alterou preços, categorias,
     * status ou estoque e quais eram os valores anteriores e novos.</p>
     * <p>Papel no Design Pattern: Consulta de verificação dos registros gerados pelos <b>Proxies</b>.</p>
     *
     * @param usuarioResponsavelId filtro opcional por UUID do usuário autor
     * @param tipoOperacao filtro opcional por {@link TipoOperacaoAuditoria}
     * @param inicio filtro opcional de data/hora inicial (inclusive)
     * @param fim filtro opcional de data/hora final (inclusive)
     * @return lista ordenada de {@link RegistroAuditoria}
     * @throws AcessoNegadoModulo1Exception se o requisitante não possuir o perfil {@code ADMIN}
     */
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultarTrilhaAuditoria(
        UUID usuarioResponsavelId,
        TipoOperacaoAuditoria tipoOperacao,
        Instant inicio,
        Instant fim
    ) {
        if (!ContextoSegurancaUsuario.possuiAlgumPerfil(PerfilUsuario.ADMIN)) {
            throw new AcessoNegadoModulo1Exception(
                "Acesso negado: apenas usuários com perfil ADMIN podem consultar a trilha de auditoria."
            );
        }
        return this.auditoriaRepository.findAllByOrderByOcorridoEmAsc().stream()
            .filter(r -> usuarioResponsavelId == null
                || usuarioResponsavelId.equals(r.obterUsuarioResponsavelId()))
            .filter(r -> tipoOperacao == null
                || tipoOperacao == r.obterTipoOperacao())
            .filter(r -> inicio == null
                || !r.obterOcorridoEm().isBefore(inicio))
            .filter(r -> fim == null
                || !r.obterOcorridoEm().isAfter(fim))
            .collect(Collectors.toList());
    }
}
