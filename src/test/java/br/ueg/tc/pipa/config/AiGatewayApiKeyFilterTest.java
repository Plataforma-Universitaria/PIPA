package br.ueg.tc.pipa.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiGatewayApiKeyFilterTest {
    private static final String SERVICE_KEY = "service-secret-with-at-least-32-characters";

    @Test
    void rejectsMissingOrIncorrectServiceKey() throws Exception {
        AiGatewayApiKeyFilter filter = new AiGatewayApiKeyFilter(SERVICE_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/v1/chat/completions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());

        request = new MockHttpServletRequest("POST", "/api/ai/v1/chat/completions");
        request.addHeader("Authorization", "Bearer wrong");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
    }

    @Test
    void acceptsConfiguredServiceKey() throws Exception {
        AiGatewayApiKeyFilter filter = new AiGatewayApiKeyFilter(SERVICE_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/v1/chat/completions");
        request.addHeader("Authorization", "Bearer " + SERVICE_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }

    @Test
    void neverEnablesGatewayWithShortConfiguredKey() throws Exception {
        AiGatewayApiKeyFilter filter = new AiGatewayApiKeyFilter("short-key");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/v1/chat/completions");
        request.addHeader("Authorization", "Bearer short-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
    }
}
