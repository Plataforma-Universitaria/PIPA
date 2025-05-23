package br.ueg.tc.pipa.features.organization;

import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.intentManagement.executor.RequestExecutorService;
import br.ueg.tc.pipa.features.dto.InstitutionCreateUpdateDTO;
import br.ueg.tc.pipa.features.dto.InstitutionDTO;
import br.ueg.tc.pipa.features.dto.InstitutionLoginFieldsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/organization")
@RestController
@CrossOrigin("*")
public class organizationController {

    @Autowired
    RequestExecutorService requestExecutorService;

    @Autowired
    InstitutionService institutionService;

    @GetMapping(path = "/login-fields/{institutionName}/{persona}")
    private ResponseEntity<InstitutionLoginFieldsDTO> getInstitutionLoginFields(@PathVariable String institutionName, @PathVariable String persona){
        return ResponseEntity.ok(requestExecutorService.getInstitutionLoginFields(institutionName, persona));
    }

    @PostMapping(path = "/add-institution")
    private ResponseEntity<InstitutionCreateUpdateDTO> addInstitution(@RequestBody InstitutionCreateUpdateDTO institution){
        return ResponseEntity.ok(institutionService.create(institution));
    }

    @GetMapping(path = "/institutions")
    private ResponseEntity<List<InstitutionDTO>> getAllInstitutions(){
        return ResponseEntity.ok(institutionService.getAll());
    }

}
