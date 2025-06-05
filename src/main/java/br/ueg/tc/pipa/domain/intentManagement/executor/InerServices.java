package br.ueg.tc.pipa.domain.intentManagement.executor;

import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InerServices {
    public List<String> getAllProviders() throws BusinessException {
        return ServiceProviderUtils.listAllServiceProviderBeans().stream().toList();
    }

    public Class<? extends IServiceProvider> getServiceProvider(String serviceProviderName) throws BusinessException {
        return ServiceProviderUtils.getServiceProviderByName(serviceProviderName);
    }

    public List<String> getServiceProvidersByProvider(String provider) throws BusinessException {
        return ServiceProviderUtils.listAllProviderServicesByProvider(provider);
    }

    public List<String> getMethodsByServiceProviders(String service) throws BusinessException {
        return ServiceProviderUtils.listAllMethodsByServiceProvider(service);
    }

}

