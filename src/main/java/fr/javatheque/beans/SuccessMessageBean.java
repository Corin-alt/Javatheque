package fr.javatheque.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.util.stream.Stream;

/**
 * A request-scoped bean that represents a success message.
 */
@Named
@RequestScoped
public class SuccessMessageBean {
    private String successMessage;

    /**
     * Retrieves the success message.
     *
     * @return the success message
     */
    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Sets the success message.
     *
     * @param successMessage the success message to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
}