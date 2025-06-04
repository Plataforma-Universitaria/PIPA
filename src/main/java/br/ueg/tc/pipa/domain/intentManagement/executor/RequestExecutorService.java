package br.ueg.tc.pipa.domain.intentManagement.executor;

import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.AIExecutionPlan;
import br.ueg.tc.pipa.features.dto.AuthenticationResponse;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa.infra.utils.ServiceInjector;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.ai.AIClient;
import br.ueg.tc.pipa_integrator.ai.PromptDefinition;
import br.ueg.tc.pipa_integrator.annotations.ActivationPhrases;
import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotAuthenticatedException;
import br.ueg.tc.pipa_integrator.institutions.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.institutions.KeyValue;
import br.ueg.tc.pipa_integrator.institutions.definations.IInstitution;
import br.ueg.tc.pipa_integrator.institutions.definations.IUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static br.ueg.tc.pipa_integrator.exceptions.UtilExceptionHandler.handleException;

@Service
public class RequestExecutorService {

    @Autowired
    AiService<AIClient> aiService;

    @Autowired
    private ServiceInjector serviceInjector;

    @Autowired
    UserService userService;

    @Autowired
    InstitutionService baseInstitutionService;

    @Value("${root.package}")
    private String institutionPackage;


    public String requestAI(IntentRequestData intentRequestData) throws BusinessException {
        try {
            IUser user = userService.findByExternalKey(UUID.fromString(intentRequestData.externalId()));
            return aiService.sendPrompt(PromptDefinition.TREAT_INTENT.getPromptText() + buildService(intentRequestData, user));
        } catch (Exception exception) {
            return aiService.sendPrompt(PromptDefinition.TREAT_ERROR.getPromptText() + intentRequestData.action() + "erro:" + ((exception.getCause() != null) ? exception.getCause().toString() : exception.toString()));
        }
    }

    private String getMethodsByService(String serviceName, IntentRequestData intentRequestData, IUser user) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServiceDesc serviceDesc = objectMapper.convertValue(serviceName, ServiceDesc.class);

            String serviceClassName = serviceDesc.getServiceName();

            Class<?> serviceClass = Class.forName(serviceClassName);
            Object serviceInstance = serviceInjector.createService(serviceClass, user);

            Method[] methods = serviceClass.getDeclaredMethods();

            StringBuilder methodSignatures = new StringBuilder();
            for (Method method : methods) {
                if (method.isAnnotationPresent(ActivationPhrases.class)) {
                    methodSignatures.append(method.getName())
                            .append("(")
                            .append(String.join(", ",
                                    Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList()))
                            .append(") ")
                            .append("Exemplos de ativação do método:")
                            .append(Arrays.toString(method.getAnnotation(ActivationPhrases.class).value()))
                            .append("--------------------------------\n");
                }

            }

            String prompt = PromptDefinition.GET_METHOD.getPromptText() +
                    "\nIntent: " + intentRequestData.action() +
                    "\nAvailable Methods:\n" + methodSignatures;

            ResponseFormat responseFormat = getFormatMethod();

            String aiJson = aiService.sendPrompt(prompt, responseFormat);

            AIExecutionPlan executionPlan = new ObjectMapper().readValue(aiJson, AIExecutionPlan.class);

            Method targetMethod = getMethod(methods, executionPlan, serviceClassName);

            Object[] args = executionPlan.parameters().toArray();

            Object result = targetMethod.invoke(serviceInstance, args);
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            throw new RuntimeException(e.getCause() + "Erro ao invocar método do serviço: " + serviceName);
        }
    }

    private static @NotNull ResponseFormat getFormatMethod() {
        var outputConverter = new BeanOutputConverter<>(AIExecutionPlan.class);

        var jsonSchema = outputConverter.getJsonSchema();

        return new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema);
    }


    private static @NotNull Method getMethod(Method[] methods, AIExecutionPlan executionPlan, String serviceClassName) throws NoSuchMethodException {
        Method targetMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(executionPlan.methodName()) &&
                    method.getParameterCount() == executionPlan.parameters().size()) {
                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {
            throw new NoSuchMethodException("Método " + executionPlan.methodName() + " não encontrado na classe " + serviceClassName);
        }
        return targetMethod;
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
            handleException(e, new RuntimeException("Method " + methodName + " not found"));
        }
        return null;
    }

    private String buildService(IntentRequestData intentRequestData, IUser user) {
        if (!intentRequestData.externalId().isEmpty()) {
            Institution institution = (Institution) user.getEducationalInstitution();
            String provider = institution.getProviderPath();
            String intent = intentRequestData.action();

            List<String> personas = user.getPersonas();
            List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider);

            String serviceName = aiService.sendPrompt(PromptDefinition.GET_SERVICE.getPromptText() +
                    "\nintent: " + intent + "\nservices: " + services + "\npersona: " + personas);
            String result = getMethodsByService(serviceName, intentRequestData, user);
            return result;

        }

        String provider = "ueg_provider";
        String intent = intentRequestData.action();
        List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider);
        String serviceName = aiService.sendPrompt(PromptDefinition.FREE_ACCESS.getPromptText()
                + "\nprovider: " + provider + "\nintent: " + intent + "\nservices: " + services);
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceDesc serviceDesc = objectMapper.convertValue(serviceName, ServiceDesc.class);
        return getMethodsByService(serviceName, intentRequestData, user);

    }

    private String getJSONResponse(String request) {
        String schema = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "methodName": { "type": "string" },
                    "parameters": {
                      "type": "object",
                      "additionalProperties": {
                        "type": "string"
                      }
                    }
                  },
                  "required": ["methodName", "parameters"],
                  "additionalProperties": false
                }
                """;


        ResponseFormat responseFormat = new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema);
        return aiService.sendPrompt(request, responseFormat);


    }

    public AuthenticationResponse authenticateUser(String username, String password, String institutionName, List<String> personas) {
        try {
            Institution baseInstitution = baseInstitutionService.getInstitutionByInstitutionName(institutionName);
            IBaseInstitutionProvider institutionProvider = getInstitutionProvider(baseInstitution);
            assert institutionProvider != null;
            List<KeyValue> userAccessData = institutionProvider.authenticateUser(username, password);
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(userService
                    .create(userAccessData, baseInstitution, personas).getExternalKey().toString());

            return authenticationResponse;
        } catch (RuntimeException e) {
            handleException(e, new UserNotAuthenticatedException());
            return null;
        }
    }

    private IBaseInstitutionProvider getInstitutionProvider(IInstitution educationalInstitution) {
        return baseInstitutionService.getInstitutionProvider(educationalInstitution);
    }

    public InstitutionLoginFieldsDTO getInstitutionLoginFields(String institutionName, String persona) {
        return baseInstitutionService.getInstitutionLoginFields(institutionName);
    }
}
