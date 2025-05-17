package br.ueg.tc.pipa.domain.ai;

public interface IAIPrompt {

    /**
     * Method that returs the prompt needed so the AI can find the requestEspecification requested by the user
     * @return
     */
    String getRequestSpecificationPrompt();

    /**
     * @return
     */
    String getInformationsPrompt();
}
