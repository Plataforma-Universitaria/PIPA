package br.ueg.tc.pipa.intentManagement.domain;

import br.ueg.tc.pipa.intentManagement.interfaces.IAIPrompt;
import org.springframework.stereotype.Service;

import static br.ueg.tc.pipa.intentManagement.domain.PromptDefinition.GET_SERVICE;


@Service
public class AIPromptImpl implements IAIPrompt {

    @Override
    public String getRequestSpecificationPrompt() {
        return GET_SERVICE.name();
    }

    @Override
    public String getInformationsPrompt() {
        return "";
    }
}
