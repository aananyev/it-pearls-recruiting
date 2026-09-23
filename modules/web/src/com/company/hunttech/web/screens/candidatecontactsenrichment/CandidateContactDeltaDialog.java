package com.company.hunttech.web.screens.candidatecontactsenrichment;

import com.company.hunttech.entity.CandidateCvContactAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.text.SimpleDateFormat;

@UiController("hunttech_CandidateContactDeltaDialog")
@UiDescriptor("candidate-contact-delta-dialog.xml")
public class CandidateContactDeltaDialog extends Screen {

    @Inject
    private Label<String> candidateNameLabel;
    @Inject
    private Label<String> metaInfoLabel;
    @Inject
    private Label<String> contactsCountBadge;
    @Inject
    private TextField<String> phoneField;
    @Inject
    private TextField<String> emailField;
    @Inject
    private TextField<String> telegramField;
    @Inject
    private TextField<String> cityField;
    @Inject
    private Label<String> photoBadge;
    @Inject
    private Label<String> photoDetailLabel;
    @Inject
    private Label<String> updatedCountBadge;
    @Inject
    private FlowBoxLayout updatedFieldsFlow;
    @Inject
    private UiComponents uiComponents;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void initData(CandidateCvContactAnalysis analysis) {
        if (analysis == null) return;

        String name = analysis.getCandidate() != null ? analysis.getCandidate().getFullName() : "—";
        candidateNameLabel.setValue(name);

        String cvDate = (analysis.getCandidateCv() != null && analysis.getCandidateCv().getDatePost() != null)
                ? new SimpleDateFormat("dd.MM.yyyy").format(analysis.getCandidateCv().getDatePost()) : "—";
        String analyzedAt = analysis.getContactsAnalyzedAt() != null
                ? new SimpleDateFormat("dd.MM.yyyy HH:mm").format(analysis.getContactsAnalyzedAt()) : "—";
        metaInfoLabel.setValue(String.format("Дата резюме: %s | Анализ: %s | Статус: %s",
                cvDate, analyzedAt, analysis.getStatus() != null ? analysis.getStatus().name() : "—"));

        phoneField.setValue(analysis.getExtractedPhone() != null ? analysis.getExtractedPhone() : "—");
        emailField.setValue(analysis.getExtractedEmail() != null ? analysis.getExtractedEmail() : "—");
        telegramField.setValue(analysis.getExtractedTelegram() != null ? analysis.getExtractedTelegram() : "—");
        cityField.setValue(analysis.getExtractedCity() != null ? analysis.getExtractedCity() : "—");

        int contactsCount = analysis.getContactsFoundCount() != null ? analysis.getContactsFoundCount() : 0;
        contactsCountBadge.setValue(String.valueOf(contactsCount));

        if (Boolean.TRUE.equals(analysis.getPhotoExtracted())) {
            photoBadge.setValue("ИЗВЛЕЧЕНО");
            photoBadge.setStyleName("ai-chip-badge badge-green");
            photoDetailLabel.setValue("Портретная фотография кандидата обнаружена и сохранена в карточку кандидата.");
        } else {
            photoBadge.setValue("НЕ НАЙДЕНО");
            photoBadge.setStyleName("ai-chip-badge");
            photoDetailLabel.setValue("Фотография в резюме отсутствует либо является посторонним логотипом.");
        }

        int updatedCount = analysis.getContactsUpdatedCount() != null ? analysis.getContactsUpdatedCount() : 0;
        updatedCountBadge.setValue(String.valueOf(updatedCount));

        // Разбор обновленных полей из deltaDetailsJson
        if (analysis.getDeltaDetailsJson() != null) {
            try {
                JsonNode root = MAPPER.readTree(analysis.getDeltaDetailsJson());
                JsonNode fieldsNode = root.get("updatedFields");
                if (fieldsNode != null && fieldsNode.isArray()) {
                    for (JsonNode f : fieldsNode) {
                        Label<String> chip = uiComponents.create(Label.TYPE_STRING);
                        chip.setValue("✓ " + f.asText());
                        chip.setStyleName("ai-chip-badge badge-blue");
                        updatedFieldsFlow.add(chip);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }
}
