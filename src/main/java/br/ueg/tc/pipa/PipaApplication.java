package br.ueg.tc.pipa;

import br.ueg.tc.pipa_integrator.interfaces.providers.IPlatformService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "br.ueg.tc")
public class PipaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipaApplication.class, args);
    }
    @Bean
    CommandLineRunner checkBeans(ApplicationContext ctx) {
        return args -> {
            System.out.println("Beans encontrados para IPlatformService:");
            String[] beans = ctx.getBeanNamesForType(IPlatformService.class);
            for (String bean : beans) {
                System.out.println(" - " + bean);
            }
        };
    }


}
