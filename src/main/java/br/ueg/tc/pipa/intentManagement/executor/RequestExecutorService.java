package br.ueg.tc.pipa.intentManagement.executor;

import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.controllers.ClientTest;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa.intentManagement.domain.IntentRequestData;
import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.institutions.info.IUserData;
import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RequestExecutorService {

    @Autowired
    AiService<ClientTest> aiService;

    @Autowired
    ServiceProviderUtils serviceProviderUtils;

    public String requestAI(IntentRequestData intentRequestData) throws BusinessException {
        return aiService.sendPrompt(intentRequestData.toString());

    }

    public List<String> getAllProviders() throws BusinessException {
        return serviceProviderUtils.listAllServiceProviderBeans()
                .stream()
                .toList();
    }


}
