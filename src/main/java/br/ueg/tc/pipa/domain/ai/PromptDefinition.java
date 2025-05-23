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
            - Retornar EXCLUSIVAMENTE, ou seja, não precisa justificar nada, envie unicamente em string simples, o serviço correspondente se for possível identificá-lo com clareza.
            - Atente-se que o prefixo da classe é correspondente ao tipo de persona que poe acessá-lo
            - Caso o serviço não seja encontrado ou haja ambiguidade, retorne um JSON de erro apropriado.
            exemplo da sua resposta:
                    meu-servico
                Formato do JSON de erro:
                    Pode ser mais específico?

            ➡   Segue a lista de serviços, a persona e a intenção do usuário:
            """
    ), GET_METHOD(
            """
            Como especialista em correspondência de intenções e metodos, considere:
            
            1. A lista de metodos disponíveis, exemplos de como ativar eles e seus parâmetros.
            2. A solicitação/intenção do usuário.

                Objetivo:
            - Retornar EXCLUSIVAMENTE, ou seja, não precisa justificar nada,
              o método correspondente se for possível identificá-lo com clareza.
             É importante entender que se há por exemplo hoje? trata-se da data de hoje no formato DateTime do java ex: YYYY-MM-DD HH:mm:ss
             Segue a lista de métodos e a intenção do usuário:
            
            """
    ), TREAT_INTENT("Você é especialista em comunicação humana," +
            " baseado nisso elabore uma frase de resposta humanizada apenas a resposta mesmo, com no máximo 100 palavras," +
            " mas preze pelo mais curto possível, Ex: para a resposta ´´Matemática;07:15;11:30´´ deve sair algo como ''No dia tal você tem ala de matemática das 07:15 às 11:30 '' " +
            "para a seguinte informação: "),
    FREE_ACCESS("""
            Como especialista em correspondência de intenções e serviços, considere:
            
            1. A lista de serviços disponíveis com acesso livre.
            2. A solicitação/intenção do usuário.

                Objetivo:
            - Retornar EXCLUSIVAMENTE, ou seja, não precisa justificar nada, envie unicamente o json desserializado com sua resposta, em JSON, o serviço correspondente se for possível identificá-lo com clareza.
            - Caso o serviço não seja encontrado ou haja ambiguidade, retorne um JSON de erro apropriado.
            exemplo da sua resposta:
                    "{\\"serviceName\\": \\"meu-servico\\"}
                Formato do JSON de erro:
                    "{\\"erro\\": \\"serviço não encontrado\\"}

            ➡   Segue a lista de serviços, a persona e a intenção do usuário:
            """);

    private final String promptText;

    PromptDefinition(String promptText) {
        this.promptText = promptText;
    }

}
