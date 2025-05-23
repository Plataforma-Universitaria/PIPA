package br.ueg.tc.pipa.domain.user;

import br.ueg.tc.pipa.domain.accesdata.AccessDataService;
import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa_integrator.enums.Persona;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa_integrator.institutions.KeyValue;
import br.ueg.tc.pipa_integrator.institutions.definations.IUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccessDataService accessDataService;

    public UserService(UserRepository userRepository, AccessDataService accessDataService) {
        this.userRepository = userRepository;
        this.accessDataService = accessDataService;
    }

    public User create(List<KeyValue> keyValueList, Institution institution, Persona persona) {

        User user =  new User();
        user.setExternalKey(UUID.randomUUID());
        user.setInstitution(institution);
        user.setPersonas(List.of(persona));

        user = userRepository.saveAndFlush(user);
        accessDataService.saveAccessData(keyValueList, user);

        return userRepository.findById(user.getId()).orElseThrow(UserNotFoundException::new);
    }

    public IUser findByExternalKey(UUID uuid) {
        return userRepository.findByExternalKey(uuid)
                .orElseThrow(UserNotFoundException::new);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void refreshAccessData(User user, List<KeyValue> refreshedAccessData) {
        accessDataService.saveAccessData(refreshedAccessData, user);
    }

}
