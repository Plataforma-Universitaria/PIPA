package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import br.ueg.tc.pipa.infra.utils.ServiceInjector;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa.publicServices.HelpService;
import br.ueg.tc.pipa.publicServices.PublicService;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Object executeTool(String toolName, String userExternalId, Map<String, String> params) {
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

        for (String serviceName : serviceNames) {
            try {
                Class<?> clazz = Class.forName(serviceName);
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(ServiceProviderMethod.class)) {
                        ServiceProviderMethod annotation = method.getAnnotation(ServiceProviderMethod.class);
                        if (annotation.actionName().equals(toolName)) {
                            Object serviceInstance;
                            if (clazz.equals(PublicService.class)) {
                                serviceInstance = publicService;
                            } else if (clazz.equals(HelpService.class)) {
                                serviceInstance = helpService;
                            } else {
                                serviceInstance = serviceInjector.createService(clazz, user);
                            }

                            Object[] methodArgs = buildMethodArgs(method, params);
                            return method.invoke(serviceInstance, methodArgs);
                        }
                    }
                }
            } catch (Exception e) {
                // Continue searching
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
}
