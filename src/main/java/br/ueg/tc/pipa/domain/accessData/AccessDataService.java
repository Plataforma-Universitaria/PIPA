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
        if (keyValueList != null && !keyValueList.isEmpty()) {
            for (KeyValue keyValue : keyValueList) {
                AccessData accessData = accessDataRepository
                        .findByUserAndKey(user, keyValue.getKey())
                        .orElseGet(() -> {
                            AccessData newAccessData = new AccessData();
                            newAccessData.setKey(keyValue.getKey());
                            newAccessData.setUser(user);
                            return newAccessData;
                        });

                accessData.setValue(keyValue.getValue());
                accessDataRepository.saveAndFlush(accessData);
            }
        }
    }

}
