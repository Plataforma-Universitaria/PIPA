package br.ueg.tc.pipa.domain.intentManagement;

import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa_integrator.ai.AIClient;
import br.ueg.tc.pipa.domain.institution.InstitutionService;
import br.ueg.tc.pipa.domain.user.UserService;
import javassist.tools.reflect.Reflection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class ResponseService extends Reflection {



    @Autowired
    private AiService<AIClient> aiService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private InstitutionService baseInstitutionService;
    @Autowired
    private UserService userService;
    @Value("${institution.package}")
    private String institutionPackage;


}
