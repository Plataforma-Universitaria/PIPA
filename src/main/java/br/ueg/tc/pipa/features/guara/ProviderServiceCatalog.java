package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProviderServiceCatalog {

    public List<String> listServices(String providerPath, List<String> personas) {
        return ServiceProviderUtils.listAllProviderServicesByProvider(providerPath, personas);
    }
}
