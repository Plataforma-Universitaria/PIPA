package br.ueg.tc.pipa.features.test;

import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.intentManagement.executor.InerServices;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RequestMapping("/api/test")
@RestController
public class TestController {

    @Autowired
    RequestExecutorService requestExecutorService;

    @Autowired
    InerServices inerServices;

    @PostMapping("/ia")
    public String generation(@RequestBody IntentRequestData intentRequestData) {
        return requestExecutorService.requestAI(intentRequestData, "test");
    }

    @GetMapping("/all-services-providers")
    public List<String> findInstitution() {
        return inerServices.getAllProviders();
    }

    @GetMapping("/provider-service-name/{serviceProviderName}")
    public String findProviderDependence(@PathVariable String serviceProviderName) {
        return inerServices.getServiceProvider(serviceProviderName).toString();
    }

    @GetMapping("/all-provider-service-name/{providerName}")
    public List<String>  findAllServiceProvidersByProvider(@PathVariable String providerName) {
        return Collections.singletonList(inerServices.getServiceProvidersByProvider(providerName).toString());
    }

    @GetMapping("/all-methods-by-service/{serviceName}")
    public List<String>  findAllMethodsByService(@PathVariable String serviceName) {
        return inerServices.getMethodsByServiceProviders(serviceName);
    }

    @PostMapping("/service")
    public String  serviceByUser(@RequestBody IntentRequestData intentRequestData) {
        return inerServices.getServiceDone(intentRequestData);
    }
}
