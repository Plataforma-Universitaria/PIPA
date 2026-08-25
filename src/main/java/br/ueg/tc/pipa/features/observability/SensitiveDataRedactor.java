package br.ueg.tc.pipa.features.observability;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Remove credenciais e identificadores sensiveis antes da persistencia de
 * detalhes tecnicos da observabilidade.
 */
@Component
public class SensitiveDataRedactor {

    private static final String REDACTED = "[REDACTED]";

    private static final List<Pattern> VALUE_PATTERNS = List.of(
            Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+"),
            Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"),
            Pattern.compile("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)")
    );

    private static final Pattern NAMED_CREDENTIAL = Pattern.compile(
            "(?i)(\\b(?:password|senha|token|access[_-]?token|refresh[_-]?token|"
                    + "api[_-]?key|secret|credential|credencial|cpf)\\b\\s*[:=]\\s*)"
                    + "(\"[^\"]*\"|'[^']*'|[^\\s,;}&]+)"
    );

    public String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String redacted = NAMED_CREDENTIAL.matcher(value).replaceAll("$1" + REDACTED);
        for (Pattern pattern : VALUE_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll(REDACTED);
        }
        return redacted;
    }
}
