package br.ueg.tc.pipa.intentManagement.domain;

import lombok.Getter;

@Getter
public enum PromptDefinition {

    GET_SERVICE(
            """
            Como especialista em correspondência de intenções e serviços, considere:
            
            1. A lista de serviços disponíveis.
            2. A solicitação/intenção do usuário.
            3. O tipo de acesso fornecido.

                Objetivo:
            - Retornar, em JSON, o serviço correspondente se for possível identificá-lo com clareza.
            - Caso o serviço não seja encontrado ou haja ambiguidade, retorne um JSON de erro apropriado.

                Formato do JSON de resposta encontrada:
            {
              "serviceName": "nomeDoServico",
              "parameters": {
                // parâmetros esperados
              }
            }

                Formato do JSON de erro:
            {
              "erro": "serviço não encontrado"
            }

            ➡   Segue a lista de serviços e a intenção do usuário:
            """
    );

    private final String promptText;

    PromptDefinition(String promptText) {
        this.promptText = promptText;
    }

}
