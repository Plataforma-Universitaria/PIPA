package br.ueg.tc.pipa.publicServices;

import br.ueg.tc.pipa.features.guara.GuaraToolMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PublicServiceTaskTest {
    @Test
    void dateLookupAndFullAgendaExposeDifferentTools() {
        var tools = new GuaraToolMapper().mapTools(List.of(PublicService.class.getName()), List.of("Aluno"));
        assertThat(tools).extracting(tool -> tool.name()).doesNotHaveDuplicates();
        var one = tools.stream().filter(tool -> tool.name().equals("consultar_uma_anotacao")).findFirst().orElseThrow();
        var all = tools.stream().filter(tool -> tool.name().equals("consultar_todas_as_anotacoes")).findFirst().orElseThrow();
        assertThat(one.parameters()).containsKey("date");
        assertThat(all.parameters()).isEmpty();
    }
}
