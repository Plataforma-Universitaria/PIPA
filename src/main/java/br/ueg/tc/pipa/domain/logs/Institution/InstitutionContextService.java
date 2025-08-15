package br.ueg.tc.pipa.domain.logs.Institution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstitutionContextService {

    @Autowired
    private InstitutionContextRepository institutionContextRepository;

    public void save(InstitutionContext institutionContext){
        institutionContextRepository.save(institutionContext);
    }
}
