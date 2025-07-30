package br.ueg.tc.pipa.domain.intentManagement.executor;

import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.intentManagement.IntentResponseData;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.AIExecutionPlan;
import br.ueg.tc.pipa.features.dto.AuthenticationResponse;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa.infra.utils.ServiceInjector;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.ai.AIClient;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderClass;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.enums.Persona;
import br.ueg.tc.pipa_integrator.enums.PromptDefinition;
import br.ueg.tc.pipa_integrator.exceptions.intent.IntentNotSupportedException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotAuthenticatedException;
import br.ueg.tc.pipa_integrator.interfaces.platform.IInstitution;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.interfaces.providers.KeyValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;

import static br.ueg.tc.pipa_integrator.exceptions.UtilExceptionHandler.handleException;

@Service
public class RequestExecutorService {

    @Autowired
    private AiService<AIClient> aiService;

    @Autowired
    private ServiceInjector serviceInjector;

    @Autowired
    private UserService userService;

    @Autowired
    private InstitutionService baseInstitutionService;

    @Value("${root.package}")
    private String institutionPackage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentResponseData requestAI(IntentRequestData intentRequestData) {
        try {
            IUser user = userService.findByExternalKey(UUID.fromString(intentRequestData.externalId()));

            return executeIntent(intentRequestData, user, user.getEducationalInstitution().getFormatResponse());

        } catch (Exception exception) {
            String error = (exception.getCause() != null) ? exception.getCause().toString() : exception.toString();
            return new IntentResponseData(error, "error", "error");
        }
    }

    private IntentResponseData executeIntent(IntentRequestData intentRequestData, IUser user, boolean formattedResponse) {
        String serviceNameJson = getServiceName(intentRequestData, user);
        ServiceDesc serviceDesc = objectMapper.convertValue(serviceNameJson, ServiceDesc.class);
        String serviceClassName = serviceDesc.getServiceName();

        try {
            Class<?> serviceClass = Class.forName(serviceClassName);
            Object serviceInstance = serviceInjector.createService(serviceClass, user);
            Method[] methods = serviceClass.getDeclaredMethods();

            String methodPrompt = buildMethodPrompt(serviceClass, methods, intentRequestData.action());
            String aiJson = aiService.sendPrompt(methodPrompt, getFormatMethod());

            AIExecutionPlan executionPlan = objectMapper.readValue(aiJson, AIExecutionPlan.class);
            Method targetMethod = resolveMethod(methods, executionPlan, serviceClassName);
            Object result = targetMethod.invoke(serviceInstance, executionPlan.parameters().toArray());

            if(formattedResponse)
                return new IntentResponseData(aiService.sendPrompt(
                        PromptDefinition.TREAT_INTENT + (result != null ? result.toString(): "null")
                                + "Pergunta que foi feita: "
                                + intentRequestData.action()),
                        serviceClassName, executionPlan.methodName());

            return new IntentResponseData((result != null ? result: "null"),
                    serviceClassName, executionPlan.methodName());


        } catch (Exception e) {
            throw new RuntimeException("Erro: " + e.getMessage(), e);
        }
    }

    private String getServiceName(IntentRequestData intentRequestData, IUser user) {
        String intent = intentRequestData.action();
        Institution institution = (Institution) user.getEducationalInstitution();
        String provider = institution.getProviderPath();
        List<String> personas = user.getPersonas();
        List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider, personas);

        return aiService.sendPrompt(PromptDefinition.GET_SERVICE.getPromptText()
                + "\nintent: " + intent + "\nservices: " + services + "\npersona: " + personas);
    }

    private String buildMethodPrompt(Class<?> serviceClass, Method[] methods, String intent) {
        StringBuilder methodSignatures = new StringBuilder();

        if (serviceClass.isAnnotationPresent(ServiceProviderClass.class)) {
            for (Method method : methods) {
                if (method.isAnnotationPresent(ServiceProviderMethod.class)) {
                    methodSignatures.append(method.getName())
                            .append("(")
                            .append(String.join(", ",
                                    Arrays.stream(method.getParameterTypes())
                                            .map(Class::getSimpleName)
                                            .toList()))
                            .append(") - Exemplos: ")
                            .append(Arrays.toString(method.getAnnotation(ServiceProviderMethod.class).activationPhrases()))
                            .append("\n");
                }
            }
            methodSignatures.append("Os serviços da classe são permitidos às personas: ")
                    .append(Arrays.toString(serviceClass.getAnnotation(ServiceProviderClass.class).personas()))
                    .append("\n");
        }

        return PromptDefinition.GET_METHOD.getPromptText() +
                "\nIntenção: " + intent +
                "\nMétodos disponíveis:\n" + methodSignatures;
    }

    private static @NotNull ResponseFormat getFormatMethod() {
        var outputConverter = new BeanOutputConverter<>(AIExecutionPlan.class);
        return new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, outputConverter.getJsonSchema());
    }

    private static Method resolveMethod(Method[] methods, AIExecutionPlan plan, String serviceClassName) throws NoSuchMethodException {
        return Arrays.stream(methods)
                .filter(m -> m.getName().equals(plan.methodName()) && m.getParameterCount() == plan.parameters().size())
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException("Serviço não encontrado pode ser mais específico?"));
    }

    public AuthenticationResponse authenticateUser(String username, String password, String institutionName, List<String> personas) {
        try {
            List<String> personasList = new ArrayList<>(personas);

            if (!personasList.contains(Persona.GUEST.getDescription())) {
                personasList.add(Persona.GUEST.getDescription());
            }

            Institution baseInstitution = baseInstitutionService.getInstitutionByInstitutionName(institutionName);
            IBaseInstitutionProvider provider = getInstitutionProvider(baseInstitution);
            List<KeyValue> userAccessData = Objects.requireNonNull(provider).authenticateUser(username, password);
            UUID externalKey = userService.create(userAccessData, baseInstitution, personasList).getExternalKey();

            return new AuthenticationResponse(externalKey.toString());
        } catch (RuntimeException e) {
            handleException(e, new UserNotAuthenticatedException());
            return null;
        }
    }

    private IBaseInstitutionProvider getInstitutionProvider(IInstitution educationalInstitution) {
        return baseInstitutionService.getInstitutionProvider(educationalInstitution);
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionName, String persona) {
        return baseInstitutionService.getInstitutionLoginFields(institutionName, persona);
    }
}
