package br.ueg.tc.pipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "br.ueg.tc.pipa_integrator",
        "br.ueg.tc.ueg_provider",
        "br.ueg.tc"
})
public class PipaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipaApplication.class, args);
    }

}
