package br.ueg.tc.pipa.domain.user;

import br.ueg.tc.pipa.domain.institution.IInstitution;
import br.ueg.tc.pipa.domain.institution.Persona;
import br.ueg.tc.pipa.domain.accesdata.AccessData;
import br.ueg.tc.pipa_integrator.institutions.KeyValue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IUser {

    Long getId();
    UUID getExternalKey();
    Set<AccessData> getAccessData();
    void setAccessData(Set<AccessData> accessData);
    IInstitution getEducationalInstitution();
    List<KeyValue> getKeyValueList();
    List<Persona> getPersonas();

}
