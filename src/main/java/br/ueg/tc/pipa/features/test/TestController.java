package br.ueg.tc.pipa.features.test;

import br.ueg.tc.pipa.domain.intentManagement.executor.InnerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RequestMapping("/api/test")
@RestController
public class TestController {

    @Autowired
    InnerServices innerServices;

    @GetMapping("/all-services-providers")
    public List<String> findInstitution() {
        return innerServices.getAllProviders();
    }

    @GetMapping("/provider-service-name/{serviceProviderName}")
    public String findProviderDependence(@PathVariable String serviceProviderName) {
        return innerServices.getServiceProvider(serviceProviderName).toString();
    }

    @GetMapping("/all-provider-service-name/{providerName}")
    public List<String>  findAllServiceProvidersByProvider(@PathVariable String providerName) {
        return Collections.singletonList(innerServices.getServiceProvidersByProvider(providerName).toString());
    }

    @GetMapping("/all-methods-by-service/{serviceName}")
    public List<String>  findAllMethodsByService(@PathVariable String serviceName) {
        return innerServices.getMethodsByServiceProviders(serviceName);
    }

}
