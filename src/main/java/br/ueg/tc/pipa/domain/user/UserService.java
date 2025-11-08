package br.ueg.tc.pipa.domain.user;

import br.ueg.tc.pipa.domain.accessData.AccessDataService;
import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa_integrator.interfaces.providers.KeyValue;
import br.ueg.tc.pipa_integrator.interfaces.platform.IUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccessDataService accessDataService;

    public UserService(UserRepository userRepository, AccessDataService accessDataService) {
        this.userRepository = userRepository;
        this.accessDataService = accessDataService;
    }

    public User create(List<KeyValue> keyValueList, Institution institution, List<String> personas) {

        User user = new User();
        user.setExternalKey(UUID.randomUUID());
        user.setInstitution(institution);
        user.setPersonas(personas);

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

    @Transactional
    public boolean delete(String externalId) throws UserNotFoundException {
        Optional<User> user = userRepository.deleteByExternalKey(UUID.fromString(externalId));
        return user.isPresent();
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Nenhum usuário autenticado no contexto de segurança");
        }

        String principalName = authentication.getName();

        UUID externalKey;
        try {
            externalKey = UUID.fromString(principalName);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("O 'principal' do usuário no contexto não é um UUID válido: " + principalName, e);
        }

        User user = userRepository.findByExternalKey(externalKey)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco com a chave: " + externalKey));

        return user.getId();
    }


    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco de dados"));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
