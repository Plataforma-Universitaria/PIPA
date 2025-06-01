package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    InstitutionCreateUpdateDTO toDTO(Institution institution);

    Institution toEntity(InstitutionCreateUpdateDTO dto);
}
