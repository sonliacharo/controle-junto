package br.ucsal.controlejunto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada do back-end do ControleJunto.
 */
/* O login é feito por JWT próprio; o usuário padrão do Spring Security não é necessário. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ControleJuntoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControleJuntoApplication.class, args);
    }
}
