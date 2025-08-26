package br.ueg.tc.pipa.features.organization;

import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import br.ueg.tc.pipa.features.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/organization")
@RestController
@CrossOrigin("${cross}")
public class organizationController {

    @Autowired
    RequestExecutorService requestExecutorService;

    @Autowired
    InstitutionService institutionService;


    @PostMapping(value = "/authenticate-user", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "External Key of authenticated user",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error trying to authenticate user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = String.class
                                    )
                            )
                    )
            }
    )
    private ResponseEntity<AuthenticationResponse> authenticateUser(@RequestBody LoginDTO loginDTO){
        return ResponseEntity.ok(requestExecutorService.authenticateUser(
                loginDTO.username(), loginDTO.password(), loginDTO.institutionName(), List.of(loginDTO.persona())));
    }

    @GetMapping(value = "/authenticate-user/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "External Key of authenticated user",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error trying to authenticate user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = String.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<String> validation(){
        return ResponseEntity.ok("OK");
    }


    @GetMapping(path = "/login-fields/{institutionName}/{persona}")
    public ResponseEntity<InstitutionLoginFieldsDTO> getInstitutionLoginFields(@PathVariable String institutionName, @PathVariable String persona){
        return ResponseEntity.ok(requestExecutorService.getInstitutionLoginFields(institutionName, persona));
    }


    @PostMapping(path = "/add-institution")
    public ResponseEntity<InstitutionCreateUpdateDTO> addInstitution(@RequestBody InstitutionCreateUpdateDTO institution){
        return ResponseEntity.ok(institutionService.create(institution));
    }

    @PostMapping(path = "/update-institution")
    public ResponseEntity<InstitutionCreateUpdateDTO> updateInstitution(@RequestBody InstitutionCreateUpdateDTO institution){
        return ResponseEntity.ok(institutionService.update(institution));
    }

    @GetMapping(path = "/institutions")
    public ResponseEntity<List<InstitutionDTO>> getAllInstitutions(){
        return ResponseEntity.ok(institutionService.getAll());
    }

    @PostMapping(value = "/logout")
    @Operation(
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "logout success",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = String.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error trying to logout user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = String.class
                                    )
                            )
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    private ResponseEntity<String> logoutUser(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(requestExecutorService.logoutUser(jwt.getSubject()));
    }

}
