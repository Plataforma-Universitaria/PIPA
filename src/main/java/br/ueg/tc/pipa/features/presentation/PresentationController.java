package br.ueg.tc.pipa.features.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class PresentationController {

    @GetMapping("/home")
    public String getPresentation() {
        return "home";
    }
}
