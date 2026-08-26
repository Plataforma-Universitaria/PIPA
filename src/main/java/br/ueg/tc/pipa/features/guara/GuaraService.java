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
        IUser iUser = userService.findByExternalKey(UUID.fromString(userExternalId));
        User user = (User) iUser;
        Institution institution = (Institution) iUser.getEducationalInstitution();
        String providerPath = institution.getProviderPath();
        List<String> personas = iUser.getPersonas();
        
        IBaseInstitutionProvider providerClass = institutionService.getInstitutionProvider(institution);

        List<String> serviceNames = ServiceProviderUtils.listAllProviderServicesByProvider(providerPath, personas);
        
        if (providerClass != null && providerClass.canAccessTask().stream().anyMatch(personas::contains)) {
            serviceNames.add(PublicService.class.getName());
        }
        serviceNames.add(HelpService.class.getName());

        for (String serviceName : serviceNames) {
            try {
                Class<?> clazz = Class.forName(serviceName);
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(ServiceProviderMethod.class)) {
                        ServiceProviderMethod annotation = method.getAnnotation(ServiceProviderMethod.class);
                        if (sanitizeToolName(annotation.actionName()).equals(toolName)) {
                            Object serviceInstance;
                            if (clazz.equals(PublicService.class)) {
                                serviceInstance = publicService;
                            } else if (clazz.equals(HelpService.class)) {
                                serviceInstance = helpService;
                            } else {
                                serviceInstance = serviceInjector.createService(clazz, user);
                            }

                            String toolVersion = annotation.version();
                            String persona = personas.isEmpty() ? "Desconhecido" : personas.get(0);

                            UserSession observabilitySession = null;
                            if (sessionId != null && !sessionId.isBlank()) {
                                observabilitySession = observabilityService.startSession(user, sessionId, channel);
                            }

                            Object[] methodArgs = buildMethodArgs(method, params);
                            Object result = null;
                            boolean success = true;
                            Throwable failure = null;
                            long startedAtNanos = System.nanoTime();

                            try {
                                result = method.invoke(serviceInstance, methodArgs);
                            } catch (InvocationTargetException e) {
                                success = false;
                                Throwable cause = e.getCause();
                                failure = cause != null ? cause : e;
                                if (cause instanceof RuntimeException) {
                                    throw (RuntimeException) cause;
                                }
                                throw new RuntimeException("Erro ao executar a ferramenta: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
                            } catch (IllegalAccessException e) {
                                success = false;
                                failure = e;
                                throw new RuntimeException(e);
                            } finally {
                                long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
                                observabilityService.logToolExecution(
                                        toolName, toolVersion, sessionId, observabilitySession,
                                        persona, user, success, durationMs, failure,
                                        ProviderFailureStage.TOOL_INVOCATION);
                            }

                            return result;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        }
        throw new IllegalArgumentException("Ferramenta não encontrada ou não autorizada: " + toolName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object[] buildMethodArgs(Method method, Map<String, String> params) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String valueStr = params.get(param.getName());
            if (valueStr == null) {
                args[i] = null;
                continue;
            }

            Class<?> pType = param.getType();
            if (pType == String.class) {
                args[i] = valueStr;
            } else if (pType == int.class || pType == Integer.class) {
                args[i] = Integer.parseInt(valueStr);
            } else if (pType == double.class || pType == Double.class) {
                args[i] = Double.parseDouble(valueStr);
            } else if (pType == boolean.class || pType == Boolean.class) {
                args[i] = Boolean.parseBoolean(valueStr);
            } else if (pType == long.class || pType == Long.class) {
                args[i] = Long.parseLong(valueStr);
            } else if (pType.isEnum()) {
                args[i] = Enum.valueOf((Class<Enum>) pType, valueStr);
            } else {
                args[i] = valueStr; // Fallback
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
