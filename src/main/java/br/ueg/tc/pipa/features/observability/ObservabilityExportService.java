package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ObservabilityExportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ToolExecutionLogRepository repository;
    private final ObservabilityLogMapper mapper;

    public ObservabilityExportService(ToolExecutionLogRepository repository,
                                      ObservabilityLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ExportedFile export(ObservabilityFilter filter, String requestedFormat) {
        ExportFormat format = ExportFormat.parse(requestedFormat);
        List<ObservabilityLogDTO> logs = repository.findAll(
                        ToolExecutionLogSpecification.from(filter),
                        Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .map(mapper::toDTO)
                .toList();
        return switch (format) {
            case CSV -> new ExportedFile(toCsv(logs), "text/csv", "csv");
            case PDF -> new ExportedFile(toPdf(logs), MediaType.APPLICATION_PDF_VALUE, "pdf");
        };
    }

    private byte[] toCsv(List<ObservabilityLogDTO> logs) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,toolName,toolVersion,persona,result,durationMs,failureCode,")
                .append("failureCategory,failureStage,retryable,timestamp\r\n");
        for (ObservabilityLogDTO log : logs) {
            appendCsvRow(csv,
                    log.id(), log.toolName(), log.toolVersion(), log.persona(), log.result(),
                    log.durationMs(), log.failureCode(), log.failureCategory(), log.failureStage(),
                    log.retryable(), formatTimestamp(log.timestamp()));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            csv.append(csvCell(values[index]));
        }
        csv.append("\r\n");
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private byte[] toPdf(List<ObservabilityLogDTO> logs) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"/><style>")
                .append("@page { size: A4 landscape; margin: 18mm; }")
                .append("body { font-family: sans-serif; color: #222; font-size: 9px; }")
                .append("h1 { font-size: 18px; margin-bottom: 4px; }")
                .append("p { color: #555; }")
                .append("table { width: 100%; border-collapse: collapse; }")
                .append("th,td { border: 1px solid #bbb; padding: 4px; vertical-align: top; }")
                .append("th { background: #e8eef5; }")
                .append("</style></head><body><h1>Observabilidade PIPA</h1>")
                .append("<p>Registros filtrados: ").append(logs.size()).append("</p>")
                .append("<table><thead><tr>")
                .append("<th>Data</th><th>Ferramenta</th><th>Versão</th><th>Persona</th>")
                .append("<th>Resultado</th><th>Duração (ms)</th><th>Código</th>")
                .append("<th>Categoria</th><th>Etapa</th><th>Retry</th>")
                .append("</tr></thead><tbody>");
        for (ObservabilityLogDTO log : logs) {
            html.append("<tr>");
            appendHtmlCell(html, formatTimestamp(log.timestamp()));
            appendHtmlCell(html, log.toolName());
            appendHtmlCell(html, log.toolVersion());
            appendHtmlCell(html, log.persona());
            appendHtmlCell(html, log.result());
            appendHtmlCell(html, log.durationMs());
            appendHtmlCell(html, log.failureCode());
            appendHtmlCell(html, log.failureCategory());
            appendHtmlCell(html, log.failureStage());
            appendHtmlCell(html, log.retryable());
            html.append("</tr>");
        }
        html.append("</tbody></table></body></html>");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html.toString());
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF de observabilidade", exception);
        }
    }

    private void appendHtmlCell(StringBuilder html, Object value) {
        html.append("<td>").append(xml(value)).append("</td>");
    }

    private String xml(Object value) {
        if (value == null) return "";
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatTimestamp(java.time.LocalDateTime timestamp) {
        return timestamp == null ? "" : DATE_TIME_FORMAT.format(timestamp);
    }

    public record ExportedFile(byte[] content, String mediaType, String extension) {
    }

    private enum ExportFormat {
        CSV, PDF;

        private static ExportFormat parse(String value) {
            try {
                return valueOf(value == null ? "CSV" : value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Formato de exportação deve ser csv ou pdf");
            }
        }
    }
}
