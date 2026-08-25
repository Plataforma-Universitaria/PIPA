package br.ueg.tc.pipa.features.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataRedactorTest {

    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();

    @Test
    void shouldRedactCpfAndCredentials() {
        String value = "cpf=123.456.789-09 senha: secreta "
                + "Authorization: Bearer abc.def-123 api_key=chave-privada";

        String result = redactor.redact(value);

        assertThat(result)
                .doesNotContain("123.456.789-09")
                .doesNotContain("secreta")
                .doesNotContain("abc.def-123")
                .doesNotContain("chave-privada")
                .contains("[REDACTED]");
    }

    @Test
    void shouldRedactJwtWithoutRemovingOrdinaryMessage() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvIn0.assinatura";

        assertThat(redactor.redact("Falha ao usar " + jwt))
                .isEqualTo("Falha ao usar [REDACTED]");
        assertThat(redactor.redact("Ferramenta indisponivel"))
                .isEqualTo("Ferramenta indisponivel");
    }
}
