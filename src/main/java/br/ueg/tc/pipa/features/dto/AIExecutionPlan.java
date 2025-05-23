package br.ueg.tc.pipa.features.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AIExecutionPlan (
        @JsonProperty(required = true, value = "methodName") String methodName, @JsonProperty(required = true, value = "parameters")List<String> parameters){
}
