package br.ueg.tc.pipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.ueg.tc")
public class PipaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipaApplication.class, args);
    }

}
