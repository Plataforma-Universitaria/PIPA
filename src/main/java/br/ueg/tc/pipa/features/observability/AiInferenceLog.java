package br.ueg.tc.pipa.features.observability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "ai_inference_log", indexes = {
        @Index(name = "idx_ai_inference_timestamp", columnList = "timestamp"),
        @Index(name = "idx_ai_inference_turn", columnList = "turn_id"),
        @Index(name = "idx_ai_inference_provider_model_time", columnList = "provider,model,timestamp")
})
public class AiInferenceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "turn_id", length = 36)
    private String turnId;

    @Column(name = "operation", nullable = false, length = 32)
    private String operation;

    @Column(name = "routing_mode", nullable = false, length = 20)
    private String routingMode;

    @Column(name = "routing_reason", nullable = false, length = 32)
    private String routingReason;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "result", nullable = false, length = 12)
    private String result;

    @Column(name = "failure_code", length = 40)
    private String failureCode;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public AiInferenceLog(AiInferenceEvent event) {
        this.requestId = event.requestId();
        this.turnId = event.turnId();
        this.operation = event.operation();
        this.routingMode = event.routingMode();
        this.routingReason = event.routingReason();
        this.provider = event.provider();
        this.model = event.model();
        this.durationMs = event.durationMs();
        this.inputTokens = event.inputTokens();
        this.outputTokens = event.outputTokens();
        this.totalTokens = event.totalTokens();
        this.result = event.result();
        this.failureCode = event.failureCode();
        this.timestamp = Instant.now();
    }
}
