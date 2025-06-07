package br.ueg.tc.pipa.features.intent;

import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/intent")
@RestController
@CrossOrigin("${cross}")
public class IntentController {

    @Autowired
    RequestExecutorService requestExecutorService;
    @Operation(
            summary = "Controller responsável por receber as intenções dos usuários",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping()
    public ResponseEntity<String> generation(@AuthenticationPrincipal Jwt jwt, @RequestBody String intentRequest) {
        IntentRequestData constructRequest = new IntentRequestData( jwt.getSubject(), intentRequest);
        return ResponseEntity.ok(requestExecutorService.requestAI(constructRequest));
    }


}
