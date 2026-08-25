package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import org.springframework.stereotype.Component;

@Component
public class ObservabilityLogMapper {

    public ObservabilityLogDTO toDTO(ToolExecutionLog log) {
        return new ObservabilityLogDTO(
                log.getId(),
                log.getToolName(),
                log.getToolVersion(),
                log.getPersona(),
                log.getResult(),
                log.getTimestamp()
        );
    }
}
