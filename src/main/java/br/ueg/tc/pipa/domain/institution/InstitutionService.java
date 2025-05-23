package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.domain.preference.Preference;
import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    @Autowired
    private InstitutionMapper institutionMapper;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public Institution getInstitutionByInstitutionName(String institutionName) {

        return institutionRepository.findByShortNameIgnoreCase(institutionName)
                .orElseThrow(InstitutionNotFoundException::new);
    }

    public String getInstitutionSalutationPhraseByInstitutionName(String institutionShortName) {
        Institution institution = institutionRepository.findByShortNameIgnoreCase(institutionShortName).orElse(null);

        if (Objects.nonNull(institution))
            return institution.getPreference().getSalutationPhrase();

        throw  new InstitutionNotFoundException();
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionShortName) {

        Institution institution = institutionRepository.findByShortNameIgnoreCase(institutionShortName).orElse(null);
        if (Objects.nonNull(institution))
            return new InstitutionLoginFieldsDTO(institution.getPreference().getSalutationPhrase(),
                    institution.getPreference().getUsernameFieldName(), institution.getPreference().getPasswordFieldName(), institution.getPersonas(), HttpStatus.OK.value());

        throw  new InstitutionNotFoundException();
    }

    public InstitutionCreateUpdateDTO getInstitutionDTO(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        return institutionMapper.toDTO(institution);
    }

    public InstitutionCreateUpdateDTO create(InstitutionCreateUpdateDTO dto) {
        Institution institutionEntity = institutionMapper.toEntity(dto);

        Preference preference = new Preference();
        preference.setSalutationPhrase(dto.salutationPhrase());
        preference.setUsernameFieldName(dto.usernameFieldName());
        preference.setPasswordFieldName(dto.passwordFieldName());
        preference.setProviderClass(dto.providerClass());
        preference.setInstitution(institutionEntity);
        preference.setProviderPath(dto.providerPath());

        institutionEntity.setPreference(preference);

        Institution saved = institutionRepository.save(institutionEntity);
        return institutionMapper.toDTO(saved);
    }


    public @NotNull List<InstitutionDTO> getAll() {
        return institutionRepository.findAll().stream().map(institutionMapper::toSimpleDTO).collect(Collectors.toList());
    }
}
