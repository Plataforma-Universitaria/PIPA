package br.ueg.tc.pipa.publicServices;


import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HelpService implements IServiceProvider {

    @Autowired
    UserService userService;

    @Autowired
    private InstitutionService baseInstitutionService;

    @ServiceProviderMethod(activationPhrases = {
            "Ajuda",
            "Funcionalidades",
            "O que pode fazer",
            "Ações disponíveis"
    }, actionName = "Consultar serviços disponíveis")
    public String getFunctionalities() {
        User user = userService.getCurrentUser();
        StringBuilder functionalities = new StringBuilder();
        functionalities.append("Você pode:\n");
        List<String> actionNamesList = ServiceProviderUtils.getActionNamesByPersonaAndInstitution(user.getPersonas(), user.getInstitution(), baseInstitutionService.getInstitutionProvider(user.getInstitution()));
        String formattedActionNames = String.join("\n", actionNamesList);
        functionalities.append(formattedActionNames);
        return functionalities.toString();
    }

}
