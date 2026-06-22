package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.OpenPosition;

/**
 * Вычисление CSS-класса строки по состоянию вакансии и числу активных подписок рекрутёров.
 */
public final class OpenPositionRowStyleHelper {

    private OpenPositionRowStyleHelper() {
    }

    public static String resolveStyle(OpenPosition openPosition, int recruiterTaskCount) {
        if (openPosition == null) {
            return null;
        }

        boolean isDraft = openPosition.getSignDraft() != null && openPosition.getSignDraft();
        if (isDraft) {
            return "open-position-draft";
        }

        boolean hasRecruiter = recruiterTaskCount > 0;
        int commandCandidate = openPosition.getCommandCandidate() != null
                ? openPosition.getCommandCandidate() : 2;

        if (Boolean.TRUE.equals(openPosition.getInternalProject())) {
            return hasRecruiter
                    ? "open-position-internal-project-job-recrutier"
                    : "open-position-internal-project";
        }

        if (commandCandidate == 1) {
            return "open-position-job-command";
        }

        return hasRecruiter
                ? "open-position-job-recruitier"
                : "open-position-empty-recrutier";
    }
}
