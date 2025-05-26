package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.PersonaDTO;
import br.ueg.tc.pipa_integrator.enums.Persona;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    InstitutionCreateUpdateDTO toDTO(Institution institution);

    InstitutionDTO toSimpleDTO(Institution institution);

    default PersonaDTO map(Persona persona) {
        return new PersonaDTO(persona.name(), persona.getDescription());
    }

    Institution toEntity(InstitutionCreateUpdateDTO dto);
}
