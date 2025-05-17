package br.ueg.tc.pipa.domain.preference;

import br.ueg.tc.pipa.domain.institution.Persona;

import java.util.List;

public interface IPreference {
    String getSalutationPhrase();
    String getProviderClass();
    String getUsernameFieldName();
    String getPasswordFieldName();
}
