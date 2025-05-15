package br.ueg.tc.pipa.intentManagement.domain.definations;

import br.ueg.tc.pipa.intentManagement.domain.definations.enums.Persona;
import br.ueg.tc.pipa.intentManagement.domain.definations.impl.AccessData;
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
