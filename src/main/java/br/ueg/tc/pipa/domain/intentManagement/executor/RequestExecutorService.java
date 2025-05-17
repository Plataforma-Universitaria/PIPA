package br.ueg.tc.pipa.domain.intentManagement.executor;

import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.domain.accesdata.AccessDataRepository;
import br.ueg.tc.pipa.domain.ai.AIClient;
import br.ueg.tc.pipa.domain.ai.PromptDefinition;
import br.ueg.tc.pipa.domain.institution.*;
import br.ueg.tc.pipa.domain.user.IUser;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserRepository;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.AuthenticationResponse;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotAuthenticatedException;
import br.ueg.tc.pipa_integrator.institutions.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.institutions.KeyValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static br.ueg.tc.pipa_integrator.exceptions.UtilExceptionHandler.handleException;

@Service
public class RequestExecutorService {

    @Autowired
    AiService<AIClient> aiService;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;
    
    @Autowired
    AccessDataRepository accessDataRepository;

    @Autowired
    InstitutionService baseInstitutionService;

    @Value("${institution.package}")
    private String institutionPackage;


    public String requestAI(IntentRequestData intentRequestData, String externalID) throws BusinessException {
        try {
            IUser user = userService.findByExternalKey(UUID.fromString(externalID));
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institutionPackage, user.getEducationalInstitution());
            String response = buildIntent(intentRequestData);
            return aiService.sendPrompt(PromptDefinition.TREAT_INTENT.getPromptText() + response);

        }catch (Exception exception){
            throw new RuntimeException((exception.getCause() != null) ? exception.getCause(): exception);
        }

    }


    private Object invokeResponseMethodByIntent(String intent, IBaseInstitutionProvider institutionProvider,
                                                List<String> parameters) throws Exception {
        String methodName = "get" +
                intent.substring(0, 1).toUpperCase() +
                intent.substring(1) +
                "Response";
        try {
            Method method = this.getClass().getDeclaredMethod(methodName, IBaseInstitutionProvider.class, List.class);
            return method.invoke(this, institutionProvider, parameters);
        } catch (Exception e) {
            methodName = methodName.substring(methodName.indexOf("get") + 3, methodName.indexOf("Response"));
            handleException(e, new RuntimeException("Method "+ methodName +" not found"));
        }
        return null;
    }

    private String buildIntent(IntentRequestData intentRequestData) {
        if (!intentRequestData.externalId().isEmpty()) {
            User user = userRepository.getByExternalKey(UUID.fromString(intentRequestData.externalId()));
            Institution institution = user.getInstitution();

            String provider = institution.getPreference().getProviderClass();
            String intent = intentRequestData.action();

            List<String> personas = user.getPersonas().stream().map(Enum::toString).toList();
            List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider);

            String serviceName =  aiService.sendPrompt(PromptDefinition.GET_SERVICE.getPromptText() +
                    "\nprovider: " + provider + "\nintent: " + intent + "\nservices: " + services +  " \nuserPersonas: " + personas);

            ObjectMapper objectMapper = new ObjectMapper();
            ServiceDesc serviceDesc = objectMapper.convertValue(serviceName, ServiceDesc.class);
            return serviceDesc.getServiceName();

        }

        String provider = "ueg_provider";
        String intent = "quais minhas aulas hoje";
        List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider);
        Object scv = aiService.sendPrompt(PromptDefinition.GET_SERVICE.getPromptText()
                + "\nprovider: " + provider + "\nintent: " + intent + "\nservices: " + services);
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceDesc serviceDesc = objectMapper.convertValue(scv, ServiceDesc.class);
        return serviceDesc.getServiceName();

    }

    public AuthenticationResponse authenticateUser(String username, String password, String institutionName, Persona persona) {
        try {
            Institution baseInstitution = baseInstitutionService.getInstitutionByInstitutionName(institutionName);
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(institutionPackage, baseInstitution);
            assert institutionProvider != null;
            List<KeyValue> userAccessData = institutionProvider.authenticateUser(username, password);
            return new AuthenticationResponse(userService
                    .create(userAccessData, baseInstitution).getExternalKey().toString());
        }catch (RuntimeException e ){
            handleException(e, new UserNotAuthenticatedException());
            return null;
        }
    }

    private IBaseInstitutionProvider getInstitutionProvider(String institutionPackage, IInstitution baseInstitution) {

        return null;
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionName) {
        return baseInstitutionService.getInstitutionLoginFields(institutionName);
    }
}
