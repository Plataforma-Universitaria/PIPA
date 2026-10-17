package br.ueg.tc.pipa.features.ai;

import java.util.UUID;

/** Metadados opacos de uma chamada; nunca contém mensagem ou identidade do usuário. */
public record AiInferenceContext(Operation operation, UUID turnId) {
    public enum Operation { AGENT_CHAT, INTERNAL_TEXT, INTERNAL_JSON }

    public AiInferenceContext {
        if (operation == null) {
            throw new IllegalArgumentException("Operação de inferência é obrigatória");
        }
    }
}
