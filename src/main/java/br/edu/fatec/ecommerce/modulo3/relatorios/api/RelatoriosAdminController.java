package br.edu.fatec.ecommerce.modulo3.relatorios.api;

import br.edu.fatec.ecommerce.modulo3.relatorios.aplicacao.ServicoRelatoriosGerenciais;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.ItemMaisVendidoDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioCancelamentosDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioTicketMedioDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioVendasConsolidadasDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios gerenciais (somente perfil {@code ADMIN}, garantido pela configuração de segurança).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@RestController
@RequestMapping("/api/v1/admin/relatorios")
@Tag(name = "Relatórios", description = "Módulo 3 — Relatórios gerenciais de vendas, ticket médio, produtos e cancelamentos")
public class RelatoriosAdminController {

    private final ServicoRelatoriosGerenciais servicoRelatorios;

    /**
     * Construtor com o serviço injetado.
     *
     * @param servicoRelatorios serviço de relatórios
     */
    public RelatoriosAdminController(ServicoRelatoriosGerenciais servicoRelatorios) {
        this.servicoRelatorios = servicoRelatorios;
    }

    /**
     * Vendas consolidadas.
     *
     * @return faturamento bruto, descontos, líquido e descontos por cupom
     */
    @GetMapping("/vendas-consolidadas")
    @Operation(summary = "Faturamento bruto, descontos concedidos, faturamento líquido e descontos por cupom")
    public RelatorioVendasConsolidadasDTO vendasConsolidadas() {
        return servicoRelatorios.vendasConsolidadas();
    }

    /**
     * Ticket médio.
     *
     * @return ticket médio por pedido e por cliente
     */
    @GetMapping("/ticket-medio")
    @Operation(summary = "Ticket médio por pedido e valor médio por cliente")
    public RelatorioTicketMedioDTO ticketMedio() {
        return servicoRelatorios.ticketMedio();
    }

    /**
     * Produtos mais vendidos.
     *
     * @param limite quantidade de linhas (padrão 10)
     * @return ranking por quantidade
     */
    @GetMapping("/produtos-mais-vendidos")
    @Operation(summary = "Ranking dos SKUs mais vendidos por quantidade e receita")
    public List<ItemMaisVendidoDTO> produtosMaisVendidos(@RequestParam(defaultValue = "10") int limite) {
        return servicoRelatorios.produtosMaisVendidos(limite);
    }

    /**
     * Cancelamentos.
     *
     * @return taxa de cancelamento e motivos mais frequentes
     */
    @GetMapping("/cancelamentos")
    @Operation(summary = "Taxa de cancelamentos/recusas e motivos mais frequentes")
    public RelatorioCancelamentosDTO cancelamentos() {
        return servicoRelatorios.cancelamentos();
    }
}
