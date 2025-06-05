package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class teste implements IServiceProvider {

    @Override
    public List<String> getValidPersonas() {
        return List.of("Aluno, Professor, Anonimo");
    }

    @Override
    public Boolean isValidPersona(String persona) {
        return true;
    }

    @Override
    public Boolean manipulatesData() {
        return true;
    }

}
