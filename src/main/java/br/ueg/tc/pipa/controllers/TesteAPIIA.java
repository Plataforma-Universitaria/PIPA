package br.ueg.tc.pipa.controllers;

import br.ueg.tc.apiai.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/teste")
@RestController
public class TesteAPIIA {

    @Autowired
    AiService<ClientTest> aiService;

    @PostMapping()
    public String generation(@RequestBody String userInput) {
        return aiService.sendPrompt(userInput);
    }
}
