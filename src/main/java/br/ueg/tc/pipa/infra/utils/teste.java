package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.institutions.info.IUserData;
import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class teste implements IServiceProvider {
    @Override
    public String doService(String activationPhrase, IUserData userData) throws BusinessException {
        return "";
    }

}
