package org.example.parsercompanies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("org.example.parsercompanies.model.db")
public class ParserCompaniesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParserCompaniesApplication.class, args);
    }

}
