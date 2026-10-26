package br.edu.fatec.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal de inicialização do Sistema de E-commerce com Promoções Avançadas.
 *
 * <p>Responsabilidade Comercial: Ponto de entrada da aplicação backend modular
 * dividida entre os 3 integrantes (Módulo 1: Identidade/Catálogo/Estoque/Auditoria,
 * Módulo 2: Carrinho/Promoções/Cupons/Preço, Módulo 3: Checkout/Pedidos/Pagamentos/Frete).</p>
 *
 * <p>Papel Arquitetural: Bootstrapper do container de inversão de controle do Spring Boot.</p>
 */
@SpringBootApplication
public class EcommercePromocoesApplication {

    /**
     * Método principal explícito de partida da aplicação Java.
     *
     * <p>Objetivo Funcional: Inicializar o contexto Spring Boot e expor a API REST
     * e os serviços de domínio da loja virtual.</p>
     * <p>Papel no Design Pattern: Ponto de entrada que dispara a criação dos Singletons,
     * Proxies e Fábricas gerenciados pela aplicação.</p>
     *
     * @param args argumentos de linha de comando recebidos pela JVM
     */
    public static void main(String[] args) {
        // Inicia explicitamente o contexto da aplicação Spring Boot com os argumentos fornecidos
        SpringApplication.run(EcommercePromocoesApplication.class, args);
    }
}

