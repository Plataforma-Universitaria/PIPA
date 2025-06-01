package br.ueg.tc.pipa.features.intent;

import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import br.ueg.tc.pipa.features.dto.AuthenticationResponse;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa.features.dto.LoginDTO;
import br.ueg.tc.pipa_integrator.enums.Persona;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/intent")
@RestController
@CrossOrigin("*")
@SecurityRequirement(name = "bearerAuth")
public class IntentController {

    @Autowired
    RequestExecutorService requestExecutorService;
    @PostMapping()
    public String generation(@AuthenticationPrincipal Jwt jwt, @RequestBody IntentRequestData intentRequestData) {
        return requestExecutorService.requestAI(intentRequestData, jwt.getSubject());
    }

    @PostMapping(value = "intent/generate-response", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Generate a saudation",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error trying to generate a saudation user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = String.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<String> generateResponse(@RequestBody IntentRequestData intentRequestData) {
        return ResponseEntity.ok("OK");
    }

}
