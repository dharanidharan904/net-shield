package com.netshield.models;

import java.util.Collections;
import java.util.List;

/**
 * Article model representing an offline troubleshooting guide.
 */
public class TroubleshootingArticle {
    private final String id;
    private final String title;
    private final String category;
    private final String symptoms;
    private final String technicalCause;
    private final List<String> recoverySteps;
    private final List<String> safeCommands;

    public TroubleshootingArticle(String id, String title, String category,
                                  String symptoms, String technicalCause,
                                  List<String> recoverySteps, List<String> safeCommands) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.symptoms = symptoms;
        this.technicalCause = technicalCause;
        this.recoverySteps = recoverySteps != null ? Collections.unmodifiableList(recoverySteps) : Collections.emptyList();
        this.safeCommands = safeCommands != null ? Collections.unmodifiableList(safeCommands) : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public String getTechnicalCause() {
        return technicalCause;
    }

    public List<String> getRecoverySteps() {
        return recoverySteps;
    }

    public List<String> getSafeCommands() {
        return safeCommands;
    }
}
