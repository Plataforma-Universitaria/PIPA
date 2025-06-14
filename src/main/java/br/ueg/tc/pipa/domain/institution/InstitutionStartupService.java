package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InstitutionStartupService implements ApplicationRunner {
    @Autowired
    InstitutionRepository institutionRepository;
    @Autowired
    InstitutionMapper institutionMapper;
    @Autowired
    InstitutionService institutionService;

    @Override
    public void run(ApplicationArguments args) {
        String institutionName = "ueg";

        if (institutionRepository.findByShortNameIgnoreCase(institutionName).isEmpty()) {
            InstitutionCreateUpdateDTO dto = new InstitutionCreateUpdateDTO(institutionName, "UEGProvider", "ueg_provider", false);
            institutionRepository.save(institutionMapper.toEntity(dto));
            System.out.println(institutionService.getAll().toString());

            System.out.println("UEG inserida com sucesso!");
        } else {
            System.out.println("UEG já existe.");
            System.out.println("Intitutions: " + institutionService.getAll().toString());

        }
    }
}
