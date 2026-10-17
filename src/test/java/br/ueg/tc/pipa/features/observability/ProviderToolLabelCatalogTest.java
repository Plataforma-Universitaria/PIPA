package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProviderToolLabelCatalogTest {
    @Test
    void readsProviderAnnotationAndIncludesInheritedToolsWithoutInvokingMethods() {
        var catalog = new ProviderToolLabelCatalog(List.of(Provider.class));
        assertThat(catalog.labelsFor(List.of("consultar_acao_do_provedor", "acao_herdada", "sem_anotacao")))
                .containsEntry("consultar_acao_do_provedor", "Consultar ação do provedor")
                .containsEntry("acao_herdada", "Ação herdada")
                .doesNotContainKey("sem_anotacao");
    }

    @Test
    void readsRealProviderLabelsWithTheSameIdentifierUsedByGuara() {
        var catalog = new ProviderToolLabelCatalog(List.of(br.ueg.tc.ueg_provider.serviceprovider.StudentService.class));
        assertThat(catalog.labelsFor(List.of("obter_as_notas_de_uma_disciplina")))
                .containsEntry("obter_as_notas_de_uma_disciplina", "Obter as notas de uma disciplina");
    }

    public static class BaseProvider {
        @ServiceProviderMethod(actionName = "Ação herdada", activationPhrases = {})
        public void inherited() { throw new AssertionError("Não deve executar ferramentas"); }
    }

    public static class Provider extends BaseProvider {
        @ServiceProviderMethod(actionName = "Consultar ação do provedor", activationPhrases = {})
        public void tool() { throw new AssertionError("Não deve executar ferramentas"); }
        public void semAnotacao() { throw new AssertionError("Não deve executar ferramentas"); }
    }
}
