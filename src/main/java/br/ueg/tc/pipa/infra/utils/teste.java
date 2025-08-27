package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.annotations.ServiceProviderClass;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;
import org.springframework.stereotype.Service;

@Service
@ServiceProviderClass(personas = {"Teste"})
public class teste implements IServiceProvider {

    @ServiceProviderMethod(activationPhrases = {"Mande uma mensagem de teste"}, actionName = "teste")
    public String test(){
        return "Teste bem sucedido!";
    }
}
