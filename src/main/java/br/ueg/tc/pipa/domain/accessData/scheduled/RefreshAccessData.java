package br.ueg.tc.pipa.domain.accessData.scheduled;

import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@EnableScheduling
public class RefreshAccessData {

    private final UserService userService;
    private final InstitutionService institutionService;
    public RefreshAccessData(UserService userService, InstitutionService institutionService) {
        this.userService = userService;
        this.institutionService = institutionService;
    }

    private final long SECOND = 1000;
    private final long MINUTE = SECOND * 60;

    @Scheduled(fixedDelay = MINUTE * 20)
    public void refreshStudentsAccessData() {
        System.out.println("\nINICIANDO REFRESH DOS COOKIES DE USUARIOS \nHORA: " + LocalTime.now() + "\n");
        List<User> users = userService.findAll();
        for (User user : users) {
            try {
                IBaseInstitutionProvider baseInstitutionProvider = institutionService.getInstitutionProvider(user.getEducationalInstitution().getProviderPath(), user);
                userService.refreshAccessData(user, baseInstitutionProvider.refreshUserAccessData(user.getKeyValueList(), user.getPersonas()));
                System.out.println("\nREFRESH DOS COOKIES DO USUARIO: " + user.getId().toString() +
                        "\nHORA: " + LocalTime.now() + "\n");
            } catch (Exception e) {
                System.out.println("\nERRO REFRESH DOS COOKIES DO USUARIO: " + user.getId().toString() +
                        "\nHORA: " + LocalTime.now() + "\n");
            }
        }
        System.out.println("\nENCERRANDO REFRESH DOS COOKIES DE USUARIOS \nHORA: " + LocalTime.now() + "\n");

    }
}
