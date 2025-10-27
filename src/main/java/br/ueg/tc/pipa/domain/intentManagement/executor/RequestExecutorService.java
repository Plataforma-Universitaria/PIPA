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
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotAuthenticatedException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa_integrator.interfaces.platform.IInstitution;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.interfaces.providers.KeyValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("Executando instancia do intent");
        try {
            IUser user = userService.findByExternalKey(UUID.fromString(intentRequestData.externalId()));
            IntentResponseData response = executeIntent(intentRequestData, user, user.getEducationalInstitution().getFormatResponse());
            log.info("Intenção processada");
            return response;

        } catch (Exception exception) {
            String error = (exception.getCause() != null) ? exception.getCause().toString() : exception.toString();
            log.error(exception.toString());
            return new IntentResponseData(error, "error", "error");
        }
    }

    private IntentResponseData executeIntent(IntentRequestData intentRequestData, IUser user, boolean formattedResponse) {
        try {
            String salutation = isIntentSalutation(intentRequestData.action());
            if(salutation.length() < 4) {
                String unifiedPrompt = buildUnifiedPrompt(intentRequestData, user);

                String aiJson = aiService.sendPromptWithSystemMessage(unifiedPrompt,
                        "            Você é um classificador de intenções. \" +\n" +
                        "            Sua tarefa é ler a frase do usuário e identificar a intenção principal.\n" +
                        "            Não invente nada além do JSON.", getFormatMethod());

                AIExecutionPlan executionPlan = objectMapper.readValue(aiJson, AIExecutionPlan.class);

                Class<?> serviceClass = Class.forName(executionPlan.serviceName());
                Object serviceInstance = serviceInjector.createService(serviceClass, user);

                Method targetMethod = resolveMethod(
                        serviceClass.getDeclaredMethods(),
                        executionPlan,
                        executionPlan.serviceName()
                );
                log.info("Método selecionado: {}", targetMethod.toString());
                log.info("Parametros: {}", Arrays.toString(executionPlan.parameters().toArray()));

                Object result = targetMethod.invoke(serviceInstance, executionPlan.parameters().toArray());
                if (formattedResponse) {
                    return new IntentResponseData(aiService.sendPrompt(
                            PromptDefinition.TREAT_INTENT + (result != null ? result.toString() : "null")
                                    + "Pergunta que foi feita: "
                                    + intentRequestData.action()),
                            executionPlan.serviceName(), executionPlan.methodName());
                }

                return new IntentResponseData(
                        (result != null ? result : "null"),
                        executionPlan.serviceName(),
                        executionPlan.methodName());
            }
            return new IntentResponseData(salutation,
                    "salutationDetected",
                    "salutation");

        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException("Erro: " + e.getMessage(), e);
        }
    }

    private String isIntentSalutation(String action) {
        return aiService.sendPrompt(PromptDefinition.VERIFY_INTENT.getPromptText() + action);
    }

    private String buildUnifiedPrompt(IntentRequestData intentRequestData, IUser user) {
        String intent = intentRequestData.action();
        Institution institution = (Institution) user.getEducationalInstitution();
        String provider = institution.getProviderPath();
        List<String> personas = user.getPersonas();
        List<String> services = ServiceProviderUtils.listAllProviderServicesByProvider(provider, personas);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Intenção: ").append(intent).append("\n");
        prompt.append("Persona(s): ").append(personas).append("\n");
        prompt.append("Abaixo estão os serviços disponíveis com seus métodos:\n\n");

        for (String serviceClassName : services) {
            try {
                Class<?> serviceClass = Class.forName(serviceClassName);
                if (serviceClass.isAnnotationPresent(ServiceProviderClass.class)) {
                    prompt.append("Serviço: ").append(serviceClassName).append("\n");
                    prompt.append("Permitido para personas: ")
                            .append(Arrays.toString(serviceClass.getAnnotation(ServiceProviderClass.class).personas()))
                            .append("\n");

                    for (Method method : serviceClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(ServiceProviderMethod.class)) {
                            prompt.append("  Assinatura do Método: ").append(method.getName()).append("(")
                                    .append(String.join(", ",
                                            Arrays.stream(method.getParameterTypes())
                                                    .map(Class::getSimpleName)
                                                    .toList()))
                                    .append(") - Exemplos: ")
                                    .append(Arrays.toString(method.getAnnotation(ServiceProviderMethod.class).activationPhrases()));
                            if(method.getAnnotation(ServiceProviderMethod.class).manipulatesData()){
                                prompt.append("OBS: Este método **manipula dados críticos**. " +
                                        "Só deve ser ativado se houver **mais de 97% de certeza** de que a intenção do usuário corresponde exatamente a este método. " +
                                        "Se houver qualquer dúvida, NÃO ative e responda solicitando confirmação.\n");
                            }
                            if(Arrays.stream(method.getAnnotation(ServiceProviderMethod.class).addSpec()).toList().size() > 1){
                                prompt.append("Este método tem parâmetros e essa é a especificação deles:\n").append(Arrays.toString(method.getAnnotation(ServiceProviderMethod.class).addSpec()));
                            }
                        }
                    }
                    prompt.append("\n").append(PromptDefinition.TREAT_PARAMETER.getPromptText());
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        prompt.append("\nResponda APENAS em JSON no formato: ")
                .append("{ \"serviceName\": \"full.class.Name\", \"methodName\": \"metodo\", \"parameters\": [ ... ] }");
        return prompt.toString();
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
            List<KeyValue> userAccessData = Objects.requireNonNull(provider)
                    .authenticateUser(username, password, personas);
            UUID externalKey = userService.create(userAccessData, baseInstitution, personasList).getExternalKey();

            return new AuthenticationResponse(externalKey.toString());
        } catch (UserNotAuthenticatedException e) {
            throw new RuntimeException("Credenciais in válidas");
        }catch (RuntimeException e) {
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

    public String logoutUser(String externalId) {
        try {
            return userService.delete(externalId)? "Usuário deslogado": "Erro a deslogar usuário";

        } catch (UserNotFoundException e) {
            return "Erro a deslogar usuário";
        }
    }
}
