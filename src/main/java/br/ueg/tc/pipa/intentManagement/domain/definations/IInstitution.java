package br.ueg.tc.pipa.intentManagement.domain.definations;

import br.ueg.tc.pipa.intentManagement.domain.definations.enums.Persona;

import java.util.List;

public interface IInstitution {

   Long getId();
   String getShortName();
   String getSalutationPhrase();
   String getProviderClass();
   List<Persona> getPersonas();
   String getUsernameFieldName();
   String getPasswordFieldName();
}
