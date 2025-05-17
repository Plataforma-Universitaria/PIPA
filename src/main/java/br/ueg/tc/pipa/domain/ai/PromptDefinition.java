package br.ueg.tc.pipa.domain.ai;

import lombok.Getter;

@Getter
public enum PromptDefinition {

    GET_SERVICE(
            """
            Como especialista em correspondência de intenções e serviços, considere:
            
            1. A lista de serviços disponíveis.
            2. A solicitação/intenção do usuário.

                Objetivo:
            - Retornar EXCLUSIVAMENTE, ou seja, não precisa justificar nada, envie unicamente o json desserializado com sua resposta, em JSON, o serviço correspondente se for possível identificá-lo com clareza.
            - Caso o serviço não seja encontrado ou haja ambiguidade, retorne um JSON de erro apropriado.
            exemplo da sua resposta:
                    "{\\"serviceName\\": \\"meu-servico\\"}
                Formato do JSON de erro:
                    "{\\"erro\\": \\"serviço não encontrado\\"}

            ➡   Segue a lista de serviços e a intenção do usuário:
            """
    ), GET_METHOD(
            """
            Como especialista em correspondência de intenções e metodos, considere:
            
            1. A lista de metodos disponíveis.
            2. A solicitação/intenção do usuário.

                Objetivo:
            - Retornar EXCLUSIVAMENTE, ou seja, não precisa justificar nada,
             envie unicamente o json com sua resposta, em JSON,
              o método correspondente se for possível
             identificá-lo com clareza.
            - Caso o método não seja encontrado ou haja ambiguidade, retorne um JSON de erro apropriado.
            exemplo da sua resposta:
                    {
                      "methodName": "br.ueg.tc.provider.serviceprovider.providerselecionado.getScheduleByDay",
                      "parameters": {
                      "day": "05/03/2020"},
                    }
                    ou
                    {
                      "erro": "Método não encontrado, tente ser mais específico",
                    }

             Segue a lista de métodos e a intenção do usuário:
            """
    ), TREAT_INTENT("Você é especialista em comunicação humana," +
            " baseado nisso elabore uma frase de resposta humanizada para a seguinte informação: ");

    private final String promptText;

    PromptDefinition(String promptText) {
        this.promptText = promptText;
    }

}
