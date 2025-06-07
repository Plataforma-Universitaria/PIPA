package br.ueg.tc.pipa.domain.accessData;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa_integrator.interfaces.providers.KeyValue;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessDataService {

    private final AccessDataRepository accessDataRepository;

    public AccessDataService(AccessDataRepository accessDataRepository) {
        this.accessDataRepository = accessDataRepository;
    }

    public void saveAccessData(List<KeyValue> keyValueList, User user) {
        for (KeyValue keyValue : keyValueList) {
            AccessData accessData = new AccessData();
            accessData.setKey(keyValue.getKey());
            accessData.setValue(keyValue.getValue());
            accessData.setUser(user);
            accessDataRepository.saveAndFlush(accessData);
        }
    }
}
