package br.ueg.tc.pipa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Credencial M2M da rota de IA, distinta do JWT acadêmico. */
public class AiGatewayApiKeyFilter extends OncePerRequestFilter {
    private final String expectedKey;

    public AiGatewayApiKeyFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String suppliedKey = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()) : "";
        if (expectedKey == null || expectedKey.length() < 32 || suppliedKey.isBlank()
                || !MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credencial de serviço inválida");
            return;
        }
        chain.doFilter(request, response);
    }
}
