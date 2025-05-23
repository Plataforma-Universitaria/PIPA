package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.domain.preference.Preference;
import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.PersonaDTO;
import br.ueg.tc.pipa_integrator.enums.Persona;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    @Mappings({
            @Mapping(target = "salutationPhrase", source = "preference.salutationPhrase"),
            @Mapping(target = "usernameFieldName", source = "preference.usernameFieldName"),
            @Mapping(target = "passwordFieldName", source = "preference.passwordFieldName"),
            @Mapping(target = "providerClass", source = "preference.providerClass"),
            @Mapping(target = "providerPath", source = "preference.providerPath")
    })
    InstitutionCreateUpdateDTO toDTO(Institution institution);

    @Mappings({
            @Mapping(target = "personas", source = "personas")
    })
    InstitutionDTO toSimpleDTO(Institution institution);

    default PersonaDTO map(Persona persona) {
        return new PersonaDTO(persona.name(), persona.getDescription());
    }

    @Mapping(target = "preference", source = ".", qualifiedByName = "mapPreference")
    Institution toEntity(InstitutionCreateUpdateDTO dto);

    @Named("mapPreference")
    default Preference mapPreference(InstitutionCreateUpdateDTO dto) {
        if (dto == null) return null;

        return Preference.builder()
                .salutationPhrase(dto.salutationPhrase())
                .usernameFieldName(dto.usernameFieldName())
                .passwordFieldName(dto.passwordFieldName())
                .providerClass(dto.providerClass())
                .build();
    }
}
