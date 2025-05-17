package br.ueg.tc.pipa.features.organization;

import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.intentManagement.IntentRequestData;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import br.ueg.tc.pipa.domain.user.UserService;
import br.ueg.tc.pipa.features.dto.AuthenticationResponse;
import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import br.ueg.tc.pipa.features.dto.LoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/organization")
@RestController
public class organizationController {

    @Autowired
    RequestExecutorService requestExecutorService;

    @Autowired
    InstitutionService institutionService;

    @GetMapping(path = "/login-fields/{institutionName}")
    private ResponseEntity<InstitutionLoginFieldsDTO> getInstitutionLoginFields(@PathVariable String institutionName){
        return ResponseEntity.ok(requestExecutorService.getInstitutionLoginFields(institutionName));
    }

    @PostMapping(path = "/add-institution")
    private ResponseEntity<InstitutionCreateUpdateDTO> getInstitutionLoginFields(@RequestBody InstitutionCreateUpdateDTO institution){
        return ResponseEntity.ok(institutionService.create(institution));
    }

}
