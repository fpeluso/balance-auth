package it.peluso.balanceauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class BalanceAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(BalanceAuthApplication.class, args);
    }

}
