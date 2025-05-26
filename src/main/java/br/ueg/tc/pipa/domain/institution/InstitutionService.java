package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionNotFoundException;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionPackageNotFoundException;
import br.ueg.tc.pipa_integrator.institutions.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.institutions.definations.IInstitution;
import br.ueg.tc.pipa_integrator.institutions.definations.IUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static br.ueg.tc.pipa_integrator.exceptions.UtilExceptionHandler.handleException;

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
        if (Objects.nonNull(institution)) {
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institution.getProviderClass(), institution);
            return institutionProvider.getSalutationPhrase();
        }
        return null;
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionShortName) {

        Institution institution = institutionRepository.findByShortNameIgnoreCase(institutionShortName).orElse(null);

        if (Objects.nonNull(institution)) {
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institution.getProviderClass(), institution);
            return new InstitutionLoginFieldsDTO(institutionProvider.getSalutationPhrase(),
                    institutionProvider.getUsernameFieldName(), institutionProvider.getPasswordFieldName(), institutionProvider.getPersonas(), HttpStatus.OK.value());

        }

        throw new InstitutionNotFoundException();
    }

    public InstitutionCreateUpdateDTO getInstitutionDTO(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        return institutionMapper.toDTO(institution);
    }

    public InstitutionCreateUpdateDTO create(InstitutionCreateUpdateDTO dto) {
        Institution institutionEntity = institutionMapper.toEntity(dto);
        Institution saved = institutionRepository.save(institutionEntity);
        return institutionMapper.toDTO(saved);
    }


    public @NotNull List<InstitutionDTO> getAll() {
        return institutionRepository.findAll().stream().map(institutionMapper::toSimpleDTO).collect(Collectors.toList());
    }

    public IBaseInstitutionProvider getInstitutionProvider(String institutionPackage, IUser user) {
        try {
            Class<?> institutionRequestClass = getInstitutionProviderClass(institutionPackage, user.getEducationalInstitution());
            Constructor<?> institutionRequestConstructor = institutionRequestClass.getConstructor(IUser.class);

            return (IBaseInstitutionProvider) institutionRequestConstructor.newInstance(user);
        } catch (Exception e) {
            handleException(e, new InstitutionPackageNotFoundException());
        }
        return null;
    }

    public Class<?> getInstitutionProviderClass(String institutionPackage, IInstitution educationalInstitution) {
        String institutionProviderClassName = educationalInstitution.getProviderClass();
        try {
            return Class.forName(institutionPackage + institutionProviderClassName);
        } catch (Exception e) {
            throw new InstitutionPackageNotFoundException(new Object[]{educationalInstitution.getShortName()});
        }
    }

    public IBaseInstitutionProvider getInstitutionProvider(String institutionPackage, IInstitution educationalInstitution) {
        try {
            Class<?> institutionRequestClass = getInstitutionProviderClass(institutionPackage, educationalInstitution);
            Constructor<?> institutionRequestConstructor = institutionRequestClass.getConstructor();

            return (IBaseInstitutionProvider) institutionRequestConstructor.newInstance();
        } catch (Exception e) {
            handleException(e, new InstitutionPackageNotFoundException());
        }
        return null;
    }
}
