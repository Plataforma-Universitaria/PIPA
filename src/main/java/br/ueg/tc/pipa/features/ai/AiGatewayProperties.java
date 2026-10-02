package br.ueg.tc.pipa.features.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ai.gateway")
public class AiGatewayProperties {
    private String mode = "EXTERNAL_ONLY";
    private String serviceKey = "";
    private String localBaseUrl = "http://localhost:11434/v1";
    private String localModel = "";
    private String externalBaseUrl = "https://api.openai.com/v1";
    private String externalModel = "gpt-4o-mini";
    private String externalApiKey = "";
    private Duration timeout = Duration.ofSeconds(90);

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public String getLocalBaseUrl() { return localBaseUrl; }
    public void setLocalBaseUrl(String localBaseUrl) { this.localBaseUrl = localBaseUrl; }
    public String getLocalModel() { return localModel; }
    public void setLocalModel(String localModel) { this.localModel = localModel; }
    public String getExternalBaseUrl() { return externalBaseUrl; }
    public void setExternalBaseUrl(String externalBaseUrl) { this.externalBaseUrl = externalBaseUrl; }
    public String getExternalModel() { return externalModel; }
    public void setExternalModel(String externalModel) { this.externalModel = externalModel; }
    public String getExternalApiKey() { return externalApiKey; }
    public void setExternalApiKey(String externalApiKey) { this.externalApiKey = externalApiKey; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
