package br.ueg.tc.pipa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GuaraApiKeyFilter extends OncePerRequestFilter {

    @Value("${guara.api-key}")
    private String guaraApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (request.getRequestURI().startsWith("/api/guara/")) {
            String apiKey = request.getHeader("x-api-key");
            if (!guaraApiKey.equals(apiKey)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API Key inválida ou ausente");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
