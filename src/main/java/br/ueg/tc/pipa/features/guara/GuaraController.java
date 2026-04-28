package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guara")
public class GuaraController {

    @Autowired
    private GuaraService guaraService;

    @GetMapping("/tools/{userExternalId}")
    public ResponseEntity<List<GuaraToolDTO>> listTools(@PathVariable String userExternalId) {
        return ResponseEntity.ok(guaraService.listTools(userExternalId));
    }

    @GetMapping("/tools/guest")
    public ResponseEntity<List<GuaraToolDTO>> listGuestTools() {
        return ResponseEntity.ok(guaraService.listGuestTools());
    }

    @PostMapping("/execute/{toolName}/{userExternalId}")
    public ResponseEntity<Object> executeTool(
            @PathVariable String toolName,
            @PathVariable String userExternalId,
            @RequestBody(required = false) Map<String, String> params) {
        
        if (params == null) {
            params = Map.of();
        }
        
        return ResponseEntity.ok(guaraService.executeTool(toolName, userExternalId, params));
    }
}
