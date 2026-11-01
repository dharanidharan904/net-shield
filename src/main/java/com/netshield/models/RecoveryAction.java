package com.netshield.models;

/**
 * Step-by-step, non-destructive recovery recommendation for the user.
 * Designed to adhere to safety constraints: no automated admin modifications.
 */
public class RecoveryAction {
    private final int stepNumber;
    private final String title;
    private final String description;
    private final String safeCommand;
    private final String helperActionId;
    private final boolean requiresAdmin;
    private final String explanation;

    public RecoveryAction(int stepNumber, String title, String description,
                          String safeCommand, String helperActionId,
                          boolean requiresAdmin, String explanation) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
        this.safeCommand = safeCommand;
        this.helperActionId = helperActionId;
        this.requiresAdmin = requiresAdmin;
        this.explanation = explanation;
    }

    public static RecoveryAction manual(int stepNumber, String title, String description, String explanation) {
        return new RecoveryAction(stepNumber, title, description, null, null, false, explanation);
    }

    public static RecoveryAction withCommand(int stepNumber, String title, String description,
                                             String safeCommand, String helperActionId,
                                             boolean requiresAdmin, String explanation) {
        return new RecoveryAction(stepNumber, title, description, safeCommand, helperActionId, requiresAdmin, explanation);
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSafeCommand() {
        return safeCommand;
    }

    public String getHelperActionId() {
        return helperActionId;
    }

    public boolean requiresAdmin() {
        return requiresAdmin;
    }

    public String getExplanation() {
        return explanation;
    }
}
