package br.ueg.tc.pipa.controllers;

import br.ueg.tc.pipa.intentManagement.domain.IntentRequestData;
import br.ueg.tc.pipa.intentManagement.executor.RequestExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/intent")
@RestController
public class TesteAPIIA {

    @Autowired
    RequestExecutorService requestExecutorService;

    @PostMapping()
    public String generation(@RequestBody IntentRequestData intentRequestData) {
        return requestExecutorService.requestAI(intentRequestData);
    }

    @PostMapping("/institution")
    public List<String> findInstitution() {
        return requestExecutorService.getAllProviders();
    }
}
