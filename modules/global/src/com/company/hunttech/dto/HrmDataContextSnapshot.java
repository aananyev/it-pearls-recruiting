package com.company.hunttech.dto;

import java.io.Serializable;

/**
 * Срез данных HRM HuntTech в режиме чтения (READ-ONLY) для передачи в LLM-чат.
 */
public class HrmDataContextSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String formattedContext;
    private final int totalEntitiesFound;
    private final boolean hasVacancies;
    private final boolean hasCandidates;
    private final boolean hasInteractions;
    private final boolean hasResumes;

    public HrmDataContextSnapshot(String formattedContext,
                                  int totalEntitiesFound,
                                  boolean hasVacancies,
                                  boolean hasCandidates,
                                  boolean hasInteractions,
                                  boolean hasResumes) {
        this.formattedContext = formattedContext != null ? formattedContext.trim() : "";
        this.totalEntitiesFound = totalEntitiesFound;
        this.hasVacancies = hasVacancies;
        this.hasCandidates = hasCandidates;
        this.hasInteractions = hasInteractions;
        this.hasResumes = hasResumes;
    }

    public static HrmDataContextSnapshot empty() {
        return new HrmDataContextSnapshot("", 0, false, false, false, false);
    }

    public boolean isEmpty() {
        return formattedContext.isEmpty() || totalEntitiesFound == 0;
    }

    public String getFormattedContext() {
        return formattedContext;
    }

    public int getTotalEntitiesFound() {
        return totalEntitiesFound;
    }

    public boolean isHasVacancies() {
        return hasVacancies;
    }

    public boolean isHasCandidates() {
        return hasCandidates;
    }

    public boolean isHasInteractions() {
        return hasInteractions;
    }

    public boolean isHasResumes() {
        return hasResumes;
    }

    @Override
    public String toString() {
        return "HrmDataContextSnapshot{" +
                "totalEntitiesFound=" + totalEntitiesFound +
                ", hasVacancies=" + hasVacancies +
                ", hasCandidates=" + hasCandidates +
                ", hasInteractions=" + hasInteractions +
                ", hasResumes=" + hasResumes +
                '}';
    }
}
