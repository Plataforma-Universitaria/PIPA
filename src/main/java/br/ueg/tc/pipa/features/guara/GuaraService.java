package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import br.ueg.tc.pipa.features.observability.ObservabilityService;
import br.ueg.tc.pipa.infra.utils.ServiceInjector;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa.publicServices.HelpService;
import br.ueg.tc.pipa.publicServices.PublicService;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionPackageNotFoundException;
import br.ueg.tc.pipa_integrator.exceptions.serviceProvider.MandatoryParameterNotFilled;
import br.ueg.tc.pipa_integrator.exceptions.serviceProvider.ParameterTypeNotSupported;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.Normalizer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Service
public class GuaraService {

    @Autowired
    private UserService userService;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private GuaraToolMapper guaraToolMapper;

    @Autowired
    private ServiceInjector serviceInjector;

    @Autowired
    private PublicService publicService;

    @Autowired
    private HelpService helpService;

    @Autowired
    private ObservabilityService observabilityService;

    @Autowired
    private ProviderServiceCatalog providerServiceCatalog;

    public List<GuaraToolDTO> listTools(String userExternalId) {
        IUser user = userService.findByExternalKey(UUID.fromString(userExternalId));
        Institution institution = (Institution) user.getEducationalInstitution();
        String providerPath = institution.getProviderPath();
        List<String> personas = user.getPersonas();
        
        IBaseInstitutionProvider providerClass = institutionService.getInstitutionProvider(institution);

        List<String> serviceNames = ServiceProviderUtils.listAllProviderServicesByProvider(providerPath, personas);
        
        if (providerClass != null && providerClass.canAccessTask().stream().anyMatch(personas::contains)) {
            serviceNames.add(PublicService.class.getName());
        }
        serviceNames.add(HelpService.class.getName());

        return guaraToolMapper.mapTools(serviceNames, personas);
    }

    public List<GuaraToolDTO> listGuestTools() {
        List<String> personas = List.of("Convidado");
        List<String> serviceNames = ServiceProviderUtils.listAllProviderServicesByProvider("", personas);
        
        serviceNames.add(HelpService.class.getName());

        return guaraToolMapper.mapTools(serviceNames, personas);
    }

    /**
     * Executa uma ferramenta identificada pelo toolName para o usuário dado.
     *
     * @param toolName       nome sanitizado (snake_case) da ferramenta
     * @param userExternalId UUID externo do usuário no PIPA
     * @param params         parâmetros da ferramenta recebidos do Guará
     * @param sessionId      fingerprint da sessão Redis (chat.id do Telegram); pode ser vazio
     * @param channel        canal de origem (ex: "TELEGRAM", "TYPEBOT")
     */
    public Object executeTool(String toolName, String userExternalId,
                               Map<String, String> params,
                               String sessionId, String channel) {
        long startedAtNanos = System.nanoTime();
        String observedToolName = sanitizeToolName(toolName);
        String toolVersion = null;
        String persona = "Desconhecido";
        User user = null;
        UserSession observabilitySession = null;
        ProviderFailureStage stage = ProviderFailureStage.USER_RESOLUTION;
        Throwable failure = null;
        boolean success = false;

        try {
            UUID externalId = UUID.fromString(userExternalId);
            IUser iUser = userService.findByExternalKey(externalId);
            user = (User) iUser;
            persona = iUser.getPersonas().isEmpty() ? "Desconhecido" : iUser.getPersonas().get(0);

            if (sessionId != null && !sessionId.isBlank()) {
                observabilitySession = observabilityService.startSession(user, sessionId, channel);
            }

            stage = ProviderFailureStage.PROVIDER_RESOLUTION;
            Institution institution = (Institution) iUser.getEducationalInstitution();
            if (institution == null) {
                throw new InstitutionPackageNotFoundException();
            }
            IBaseInstitutionProvider providerClass = institutionService.getInstitutionProvider(institution);
            if (providerClass == null) {
                throw new InstitutionPackageNotFoundException();
            }

            stage = ProviderFailureStage.SERVICE_DISCOVERY;
            List<String> personas = iUser.getPersonas();
            List<String> serviceNames = new ArrayList<>(providerServiceCatalog.listServices(
                    institution.getProviderPath(), personas));
            if (providerClass.canAccessTask().stream().anyMatch(personas::contains)) {
                serviceNames.add(PublicService.class.getName());
            }
            serviceNames.add(HelpService.class.getName());

            ClassNotFoundException classLoadingFailure = null;
            for (String serviceName : serviceNames) {
                Class<?> clazz;
                try {
                    clazz = Class.forName(serviceName);
                } catch (ClassNotFoundException exception) {
                    if (classLoadingFailure == null) {
                        classLoadingFailure = exception;
                    }
                    continue;
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(ServiceProviderMethod.class)) {
                        continue;
                    }
                    ServiceProviderMethod annotation = method.getAnnotation(ServiceProviderMethod.class);
                    if (!sanitizeToolName(annotation.actionName()).equals(observedToolName)) {
                        continue;
                    }

                    toolVersion = annotation.version();
                    stage = ProviderFailureStage.SERVICE_INSTANTIATION;
                    Object serviceInstance = createServiceInstance(clazz, user);

                    stage = ProviderFailureStage.ARGUMENT_BINDING;
                    Object[] methodArgs = buildMethodArgs(method, params != null ? params : Map.of());

                    stage = ProviderFailureStage.TOOL_INVOCATION;
                    Object result = invokeTool(method, serviceInstance, methodArgs);
                    success = true;
                    return result;
                }
            }

            if (classLoadingFailure != null) {
                throw new IllegalStateException("Uma classe de serviço configurada não pôde ser carregada", classLoadingFailure);
            }
            throw new IllegalArgumentException("Ferramenta não encontrada ou não autorizada");
        } catch (Throwable caught) {
            failure = caught;
            if (caught instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (caught instanceof Error error) {
                throw error;
            }
            throw new RuntimeException("Erro ao executar a ferramenta", caught);
        } finally {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            observabilityService.logToolExecutionSafely(
                    observedToolName, toolVersion, sessionId, observabilitySession,
                    persona, user, success, durationMs, failure, stage);
        }
    }

    private Object createServiceInstance(Class<?> clazz, User user) {
        if (clazz.equals(PublicService.class)) {
            return publicService;
        }
        if (clazz.equals(HelpService.class)) {
            return helpService;
        }
        return serviceInjector.createService(clazz, user);
    }

    private Object invokeTool(Method method, Object serviceInstance, Object[] methodArgs) throws Throwable {
        try {
            return method.invoke(serviceInstance, methodArgs);
        } catch (InvocationTargetException exception) {
            throw exception.getCause() != null ? exception.getCause() : exception;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object[] buildMethodArgs(Method method, Map<String, String> params) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String valueStr = params.get(param.getName());
            if (valueStr == null) {
                throw new MandatoryParameterNotFilled("Parâmetro obrigatório ausente: " + param.getName());
            }

            Class<?> pType = param.getType();
            try {
                if (pType == String.class) {
                    args[i] = valueStr;
                } else if (pType == int.class || pType == Integer.class) {
                    args[i] = Integer.parseInt(valueStr);
                } else if (pType == double.class || pType == Double.class) {
                    args[i] = Double.parseDouble(valueStr);
                } else if (pType == boolean.class || pType == Boolean.class) {
                    if (!valueStr.equalsIgnoreCase("true") && !valueStr.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("boolean inválido");
                    }
                    args[i] = Boolean.parseBoolean(valueStr);
                } else if (pType == long.class || pType == Long.class) {
                    args[i] = Long.parseLong(valueStr);
                } else if (pType.isEnum()) {
                    args[i] = Enum.valueOf((Class<Enum>) pType, valueStr);
                } else {
                    throw new IllegalArgumentException("tipo não suportado");
                }
            } catch (IllegalArgumentException exception) {
                throw new ParameterTypeNotSupported("Valor inválido para o parâmetro: " + param.getName());
            }
        }
        return args;
    }

    private String sanitizeToolName(String name) {
        if (name == null) return "unknown_tool";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String ascii = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return ascii.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
