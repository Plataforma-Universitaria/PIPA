package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionNotFoundException;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionPackageNotFoundException;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.interfaces.platform.IInstitution;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import br.ueg.tc.ueg_provider.UEGProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

import static br.ueg.tc.pipa_integrator.exceptions.UtilExceptionHandler.handleException;

@Service
public class InstitutionService {

    @Value("${root.package}")
    private String rootPackage;

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

    public String getInstitutionSalutationPhraseByInstitutionName(String institutionShortName, String persona) {

        Institution institution = institutionRepository.findByShortNameIgnoreCase(institutionShortName).orElse(null);
        if (Objects.nonNull(institution)) {
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institution);
            return institutionProvider.getSalutationPhrase(persona);
        }
        return null;
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionShortName, String persona) {

        Institution institution = institutionRepository.findByShortNameIgnoreCase(institutionShortName).orElse(null);

        if (Objects.nonNull(institution)) {
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institution);
            return new InstitutionLoginFieldsDTO(institutionProvider.getSalutationPhrase(persona),
                    institutionProvider.getUsernameFieldName(persona), institutionProvider.getPasswordFieldName(persona), institutionProvider.getPersonas(), HttpStatus.OK.value());

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

    public InstitutionCreateUpdateDTO update(InstitutionCreateUpdateDTO dto) {
        Institution institutionEntity = institutionRepository.findByShortNameIgnoreCase(dto.shortName())
                .orElseThrow(InstitutionNotFoundException::new);

        institutionEntity.setProviderClass(dto.providerClass());
        institutionEntity.setProviderPath(dto.providerPath());
        institutionEntity.setFormatResponse(dto.formatResponse());

        Institution saved = institutionRepository.save(institutionEntity);
        return institutionMapper.toDTO(saved);
    }


    public @NotNull List<InstitutionDTO> getAll() {
        List<Institution> institutions = institutionRepository.findAll();
        System.out.println("Institutions: " + institutions.toString());
        List<InstitutionDTO> institutionDTOS = new java.util.ArrayList<>(List.of());
        for (Institution institution : institutions) {
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institution);
            institutionDTOS.add(new InstitutionDTO(
                    institution.getShortName(),
                    institutionProvider.getInstitutionName(),
                    institutionProvider.getLoginData()));
        }
        return institutionDTOS;
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
            System.out.println("Root: " + rootPackage);
            System.out.println("Institution pkg data: " + institutionPackage);
            System.out.println("Institution institutionProviderClassName: " + institutionProviderClassName);
            System.out.println("Class for name: " + rootPackage + institutionPackage + "." + institutionProviderClassName);
            UEGProvider inst = new UEGProvider();
            System.out.println("Inst UEG provider pkg: " + inst.getClass().getPackage().toString());
            System.out.println("Institution UEG provider name: " + inst.getClass().getName());
            return Class.forName(rootPackage + institutionPackage + "." + institutionProviderClassName);
        } catch (Exception e) {
            throw new InstitutionPackageNotFoundException(new Object[]{educationalInstitution.getShortName()});
        }
    }

    public IBaseInstitutionProvider getInstitutionProvider(IInstitution educationalInstitution) {
        try {
            System.out.println("Institution getProviderClass: " + educationalInstitution.getProviderClass());
            System.out.println("Institution getShortName: " + educationalInstitution.getShortName());
            System.out.println("Institution getProviderPath: " + educationalInstitution.getProviderPath());

            Class<?> institutionRequestClass = getInstitutionProviderClass(educationalInstitution.getProviderPath(), educationalInstitution);
            Constructor<?> institutionRequestConstructor = institutionRequestClass.getConstructor();

            return (IBaseInstitutionProvider) institutionRequestConstructor.newInstance();
        } catch (Exception e) {
            handleException(e, new InstitutionPackageNotFoundException());
        }
        return null;
    }
}
