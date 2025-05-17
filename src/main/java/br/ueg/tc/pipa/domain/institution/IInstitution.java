package br.ueg.tc.pipa.domain.institution;

import java.util.List;

public interface IInstitution {

   Long getId();
   String getShortName();
   String getLongName();
   List<Persona> getPersonas();
}
