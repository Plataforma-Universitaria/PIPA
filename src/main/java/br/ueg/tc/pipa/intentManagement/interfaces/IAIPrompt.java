package br.ueg.tc.pipa.intentManagement.interfaces;

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
