package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.features.observability.*;
import br.ueg.tc.pipa.infra.utils.ServiceInjector;
import br.ueg.tc.pipa.publicServices.HelpService;
import br.ueg.tc.pipa.publicServices.PublicService;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderClass;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionCommunicationException;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionPackageNotFoundException;
import br.ueg.tc.pipa_integrator.exceptions.serviceProvider.MandatoryParameterNotFilled;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuaraServiceObservabilityTest {

    private GuaraService service;
    private StubUserService userService;
    private StubInstitutionService institutionService;
    private StubServiceInjector serviceInjector;
    private CapturingObservabilityService observability;
    private StubProviderServiceCatalog catalog;
    private ThrowingPublicService publicService;
    private User user;
    private String externalId;

    @BeforeEach
    void setUp() {
        service = new GuaraService();
        userService = new StubUserService();
        institutionService = new StubInstitutionService();
        serviceInjector = new StubServiceInjector();
        observability = new CapturingObservabilityService();
        catalog = new StubProviderServiceCatalog();
        publicService = new ThrowingPublicService();

        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "institutionService", institutionService);
        ReflectionTestUtils.setField(service, "serviceInjector", serviceInjector);
        ReflectionTestUtils.setField(service, "observabilityService", observability);
        ReflectionTestUtils.setField(service, "providerServiceCatalog", catalog);
        ReflectionTestUtils.setField(service, "publicService", publicService);
        ReflectionTestUtils.setField(service, "helpService", new HelpService());

        externalId = UUID.randomUUID().toString();
        Institution institution = new Institution();
        institution.setProviderPath("ueg_provider");
        user = new User();
        user.setId(7L);
        user.setExternalKey(UUID.fromString(externalId));
        user.setInstitution(institution);
        user.setPersonas(new ArrayList<>(List.of("Aluno")));
        userService.user = user;
        institutionService.provider = provider(List.of());
    }

    @Test
    void shouldObserveInvalidExternalIdAtUserResolution() {
        assertThatThrownBy(() -> execute("ajuda", "uuid-invalido", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertFailure(ProviderFailureStage.USER_RESOLUTION, IllegalArgumentException.class);
    }

    @Test
    void shouldObserveUserNotFoundAtUserResolution() {
        userService.failure = new UserNotFoundException();
        assertThatThrownBy(() -> execute("ajuda", externalId, Map.of()))
                .isSameAs(userService.failure);
        assertFailure(ProviderFailureStage.USER_RESOLUTION, UserNotFoundException.class);
    }

    @Test
    void shouldObserveProviderResolutionFailure() {
        institutionService.failure = new InstitutionPackageNotFoundException();
        assertThatThrownBy(() -> execute("ajuda", externalId, Map.of()))
                .isSameAs(institutionService.failure);
        assertFailure(ProviderFailureStage.PROVIDER_RESOLUTION,
                InstitutionPackageNotFoundException.class);
    }

    @Test
    void shouldObserveConfiguredClassLoadingFailure() {
        catalog.services = List.of("br.ueg.tc.provider.ClasseInexistente");
        assertThatThrownBy(() -> execute("inexistente", externalId, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(ClassNotFoundException.class);
        assertFailure(ProviderFailureStage.SERVICE_DISCOVERY, IllegalStateException.class);
    }

    @Test
    void shouldObserveMissingToolAtServiceDiscovery() {
        assertThatThrownBy(() -> execute("ferramenta_inexistente", externalId, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertFailure(ProviderFailureStage.SERVICE_DISCOVERY, IllegalArgumentException.class);
    }

    @Test
    void shouldObserveServiceInstantiationFailure() {
        catalog.services = List.of(BrokenService.class.getName());
        serviceInjector.failure = new IllegalStateException("construtor indisponível");
        assertThatThrownBy(() -> execute("servico_quebrado", externalId, Map.of()))
                .isSameAs(serviceInjector.failure);
        assertFailure(ProviderFailureStage.SERVICE_INSTANTIATION, IllegalStateException.class);
    }

    @Test
    void shouldObserveMissingParameterAtArgumentBinding() {
        institutionService.provider = provider(List.of("Aluno"));
        assertThatThrownBy(() -> execute("adicionar_uma_anotacao", externalId, Map.of()))
                .isInstanceOf(MandatoryParameterNotFilled.class);
        assertFailure(ProviderFailureStage.ARGUMENT_BINDING, MandatoryParameterNotFilled.class);
    }

    @Test
    void shouldUnwrapAndObserveProviderFailureAtInvocation() throws Exception {
        InstitutionCommunicationException expected =
                new InstitutionCommunicationException("Falha segura");
        institutionService.provider = provider(List.of("Aluno"));
        publicService.failure = expected;
        Method method = PublicService.class.getDeclaredMethod("addTask", String.class, String.class);

        assertThatThrownBy(() -> execute(
                "adicionar_uma_anotacao", externalId,
                parameters(method, "2026-08-26T10:00:00", "teste")))
                .isSameAs(expected);
        assertFailure(ProviderFailureStage.TOOL_INVOCATION,
                InstitutionCommunicationException.class);
    }

    private Object execute(String toolName, String userId, Map<String, String> params) {
        return service.executeTool(toolName, userId, params, "", "TELEGRAM");
    }

    private void assertFailure(ProviderFailureStage stage, Class<? extends Throwable> failureType) {
        assertThat(observability.success).isFalse();
        assertThat(observability.stage).isEqualTo(stage);
        assertThat(observability.failure).isInstanceOf(failureType);
        assertThat(observability.durationMs).isNotNegative();
    }

    private Map<String, String> parameters(Method method, String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int index = 0; index < parameters.length; index++) {
            result.put(parameters[index].getName(), values[index]);
        }
        return result;
    }

    private IBaseInstitutionProvider provider(List<String> accessibleTasks) {
        return (IBaseInstitutionProvider) Proxy.newProxyInstance(
                IBaseInstitutionProvider.class.getClassLoader(),
                new Class<?>[]{IBaseInstitutionProvider.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("canAccessTask")) return accessibleTasks;
                    if (method.getReturnType().equals(List.class)) return List.of();
                    if (method.getReturnType().equals(String.class)) return "teste";
                    return null;
                });
    }

    private static class StubUserService extends UserService {
        private User user;
        private RuntimeException failure;

        private StubUserService() {
            super(null, null);
        }

        @Override
        public User findByExternalKey(UUID uuid) {
            if (failure != null) throw failure;
            return user;
        }
    }

    private static class StubInstitutionService extends InstitutionService {
        private IBaseInstitutionProvider provider;
        private RuntimeException failure;

        private StubInstitutionService() {
            super(null);
        }

        @Override
        public IBaseInstitutionProvider getInstitutionProvider(
                br.ueg.tc.pipa_integrator.interfaces.platform.IInstitution institution) {
            if (failure != null) throw failure;
            return provider;
        }
    }

    private static class StubProviderServiceCatalog extends ProviderServiceCatalog {
        private List<String> services = List.of();

        @Override
        public List<String> listServices(String providerPath, List<String> personas) {
            return services;
        }
    }

    private static class StubServiceInjector extends ServiceInjector {
        private RuntimeException failure;

        @Override
        public <T> T createService(Class<T> clazz, Object... constructorArgs) {
            if (failure != null) throw failure;
            throw new AssertionError("Instanciação inesperada");
        }
    }

    private static class ThrowingPublicService extends PublicService {
        private RuntimeException failure;

        @Override
        public String addTask(String date, String note) {
            if (failure != null) throw failure;
            return "ok";
        }
    }

    private static class CapturingObservabilityService extends ObservabilityService {
        private boolean success;
        private Long durationMs;
        private Throwable failure;
        private ProviderFailureStage stage;

        private CapturingObservabilityService() {
            super(null, null, null, null, null, null, new ObservabilitySessionProperties());
        }

        @Override
        public boolean logToolExecutionSafely(String toolName, String toolVersion, String sessionId,
                                               UserSession userSession, String persona, User user,
                                               boolean success, Long durationMs, Throwable failure,
                                               ProviderFailureStage fallbackStage) {
            this.success = success;
            this.durationMs = durationMs;
            this.failure = failure;
            this.stage = fallbackStage;
            return true;
        }
    }

    @ServiceProviderClass(personas = {"Aluno"})
    public static class BrokenService implements IServiceProvider {
        @ServiceProviderMethod(actionName = "Serviço quebrado", activationPhrases = {"quebrar"})
        public String execute() {
            return "não executado";
        }
    }
}
