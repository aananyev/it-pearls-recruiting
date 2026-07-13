package com.company.hunttech.web.screens.jobcandidate;

import java.util.Date;
import java.util.UUID;

/**
 * Pre-computed data for a single row in the "История рассмотрения" table.
 *
 * Содержит все значения, которые ранее вычислялись генераторами колонок
 * (lastInteractionGeneratorColumn, whoIsResearcherGeneratorColumn и др.)
 * путём полного перебора jobCandidateIteractionDc для каждой строки таблицы.
 *
 * Теперь данные агрегируются один раз в BackgroundTask.run() за O(N) проход
 * по взаимодействиям кандидата, а генераторы читают готовые значения из
 * Map<UUID, HistoryRowData> за O(1). Это устранило ensureInteractionsLoaded()
 * из генераторов и O(N*M) сложность на каждый рендер.
 *
 * Safe to pass between threads (no UI references).
 */
public class HistoryRowData {
    public final UUID vacancyId;
    public final String vacancyName;
    public final Date maxDate;
    public final String lastInteractionName;
    public final String researcherName;
    public final String recruiterName;

    public HistoryRowData(UUID vacancyId, String vacancyName, Date maxDate,
                          String lastInteractionName, String researcherName,
                          String recruiterName) {
        this.vacancyId = vacancyId;
        this.vacancyName = vacancyName;
        this.maxDate = maxDate;
        this.lastInteractionName = lastInteractionName;
        this.researcherName = researcherName;
        this.recruiterName = recruiterName;
    }
}
