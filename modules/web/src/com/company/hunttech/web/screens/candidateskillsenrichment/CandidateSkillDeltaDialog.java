package com.company.hunttech.web.screens.candidateskillsenrichment;

import com.company.hunttech.entity.CandidateCvSkillAnalysis;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;

@UiController("hunttech_CandidateSkillDeltaDialog")
@UiDescriptor("candidate-skill-delta-dialog.xml")
public class CandidateSkillDeltaDialog extends Screen {

    private static final Logger log = LoggerFactory.getLogger(CandidateSkillDeltaDialog.class);

    @Inject
    private Label<String> candidateNameLabel;
    @Inject
    private Label<String> metaInfoLabel;
    @Inject
    private Label<String> addedBadge;
    @Inject
    private Label<String> updatedBadge;
    @Inject
    private Label<String> unchangedBadge;
    @Inject
    private Label<String> skippedBadge;
    @Inject
    private FlowBoxLayout addedFlow;
    @Inject
    private FlowBoxLayout updatedFlow;
    @Inject
    private FlowBoxLayout unchangedFlow;
    @Inject
    private FlowBoxLayout skippedFlow;
    @Inject
    private UiComponents uiComponents;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public void initData(CandidateCvSkillAnalysis analysis) {
        if (analysis == null) return;

        String candName = analysis.getCandidate() != null ? analysis.getCandidate().getFullName() : "—";
        candidateNameLabel.setValue(candName);

        String cvDateStr = (analysis.getCandidateCv() != null && analysis.getCandidateCv().getDatePost() != null)
                ? new SimpleDateFormat("dd.MM.yyyy").format(analysis.getCandidateCv().getDatePost())
                : "—";

        String analyzedAtStr = analysis.getSkillsAnalyzedAt() != null
                ? dateFormat.format(analysis.getSkillsAnalyzedAt())
                : "—";

        String meta = String.format("Резюме от: %s | Анализ выполнен: %s | Модель: %s (%s) | Токены: %d | Длительность: %d мс",
                cvDateStr, analyzedAtStr,
                analysis.getModelName() != null ? analysis.getModelName() : "—",
                analysis.getProviderCode() != null ? analysis.getProviderCode() : "—",
                analysis.getTotalTokens() != null ? analysis.getTotalTokens() : 0,
                analysis.getDurationMs() != null ? analysis.getDurationMs() : 0);
        metaInfoLabel.setValue(meta);

        String deltaJson = analysis.getDeltaDetailsJson();
        if (deltaJson != null && !deltaJson.trim().isEmpty()) {
            try {
                CandidateSkillsScanResult scanResult = objectMapper.readValue(deltaJson, CandidateSkillsScanResult.class);
                populateDelta(scanResult);
                return;
            } catch (Exception e) {
                log.warn("Failed to parse deltaDetailsJson for analysis id={}: {}", analysis.getId(), e.getMessage());
            }
        }

        // Если JSON отсутствует или пуст — показываем счетчики
        addedBadge.setValue(String.valueOf(analysis.getSkillsAddedCount() != null ? analysis.getSkillsAddedCount() : 0));
        updatedBadge.setValue(String.valueOf(analysis.getSkillsUpdatedCount() != null ? analysis.getSkillsUpdatedCount() : 0));
        unchangedBadge.setValue(String.valueOf(analysis.getSkillsUnchangedCount() != null ? analysis.getSkillsUnchangedCount() : 0));
        skippedBadge.setValue(String.valueOf(analysis.getSkillsSkippedCount() != null ? analysis.getSkillsSkippedCount() : 0));
    }

    private void populateDelta(CandidateSkillsScanResult scanResult) {
        if (scanResult == null) return;

        // 1. ADDED
        addedBadge.setValue(String.valueOf(scanResult.getAddedSkills().size()));
        for (CandidateSkillsScanResult.SkillDeltaItem item : scanResult.getAddedSkills()) {
            Label<String> lbl = createChipLabel(item.getSkillName() + " (" + priorityText(item.getPriority()) + ")", "ai-chip-badge badge-green");
            addedFlow.add(lbl);
        }
        if (scanResult.getAddedSkills().isEmpty()) {
            addedFlow.add(createEmptyLabel("Нет добавленных навыков"));
        }

        // 2. UPDATED
        updatedBadge.setValue(String.valueOf(scanResult.getUpdatedSkills().size()));
        for (CandidateSkillsScanResult.SkillDeltaItem item : scanResult.getUpdatedSkills()) {
            String text = String.format("%s: %s → %s", item.getSkillName(), priorityText(item.getOldPriority()), priorityText(item.getPriority()));
            Label<String> lbl = createChipLabel(text, "ai-chip-badge badge-blue");
            updatedFlow.add(lbl);
        }
        if (scanResult.getUpdatedSkills().isEmpty()) {
            updatedFlow.add(createEmptyLabel("Нет обновленных навыков"));
        }

        // 3. UNCHANGED
        unchangedBadge.setValue(String.valueOf(scanResult.getUnchangedSkills().size()));
        for (CandidateSkillsScanResult.SkillDeltaItem item : scanResult.getUnchangedSkills()) {
            Label<String> lbl = createChipLabel(item.getSkillName(), "ai-chip-badge");
            unchangedFlow.add(lbl);
        }
        if (scanResult.getUnchangedSkills().isEmpty()) {
            unchangedFlow.add(createEmptyLabel("Нет навыков без изменений"));
        }

        // 4. SKIPPED
        skippedBadge.setValue(String.valueOf(scanResult.getSkippedSkills().size()));
        for (CandidateSkillsScanResult.SkillDeltaItem item : scanResult.getSkippedSkills()) {
            String text = item.getSkillName() + (item.getReason() != null ? " (" + item.getReason() + ")" : "");
            Label<String> lbl = createChipLabel(text, "ai-chip-badge badge-orange");
            skippedFlow.add(lbl);
        }
        if (scanResult.getSkippedSkills().isEmpty()) {
            skippedFlow.add(createEmptyLabel("Нет пропущенных навыков"));
        }
    }

    private Label<String> createChipLabel(String text, String styleName) {
        Label<String> label = uiComponents.create(Label.TYPE_STRING);
        label.setValue(text);
        label.setStyleName(styleName);
        return label;
    }

    private Label<String> createEmptyLabel(String text) {
        Label<String> label = uiComponents.create(Label.TYPE_STRING);
        label.setValue(text);
        label.setStyleName("subheading");
        return label;
    }

    private String priorityText(CandidateSkillPriority priority) {
        if (priority == null) return "—";
        switch (priority) {
            case MAIN:
                return "Основной";
            case SECONDARY:
                return "Второстепенный";
            case TERTIARY:
                return "Третьестепенный";
            default:
                return priority.name();
        }
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }
}
