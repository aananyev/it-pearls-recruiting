package com.company.hunttech.web.screens.candidatevacancymatch;

import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.service.CandidateVacancyWorkflowService;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.screen.*;
import com.vaadin.ui.JavaScript;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UiController("hunttech_CandidateOutreachDraftDialog")
@UiDescriptor("candidate-outreach-draft-dialog.xml")
@DialogMode(width = "820px", height = "640px", modal = true, forceDialog = true)
public class CandidateOutreachDraftDialog extends Screen {

    @Inject
    private CandidateVacancyWorkflowService workflowService;

    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private Notifications notifications;

    @Inject
    private Label<String> subHeaderLabel;

    @Inject
    private TextArea<String> messageArea;

    private UUID candidateId;
    private UUID vacancyId;
    private String candidateName;
    private String vacancyTitle;
    private List<String> reasonsToOffer = new ArrayList<>();
    private List<String> matchedSkills = new ArrayList<>();

    public void setContext(UUID candidateId, UUID vacancyId, String candidateName, String vacancyTitle,
                           List<String> reasonsToOffer, List<String> matchedSkills) {
        this.candidateId = candidateId;
        this.vacancyId = vacancyId;
        this.candidateName = candidateName;
        this.vacancyTitle = vacancyTitle;
        this.reasonsToOffer = reasonsToOffer != null ? reasonsToOffer : new ArrayList<>();
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
    }

    public void initParams(UUID candidateId, UUID vacancyId, String candidateName, String vacancyTitle,
                           List<String> reasonsToOffer, List<String> matchedSkills, Integer score) {
        setContext(candidateId, vacancyId, candidateName, vacancyTitle, reasonsToOffer, matchedSkills);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (candidateName != null && vacancyTitle != null) {
            subHeaderLabel.setValue("Кандидат: " + candidateName + " | Вакансия: " + vacancyTitle);
        }

        generateMessage();
    }

    private void generateMessage() {
        if (candidateId != null && vacancyId != null) {
            String draft = workflowService.generateOutreachDraft(candidateId, vacancyId, reasonsToOffer, matchedSkills);
            messageArea.setValue(draft);
        }
    }

    @Subscribe("regenerateBtn")
    public void onRegenerateBtnClick(Button.ClickEvent event) {
        generateMessage();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Черновик обновлен")
                .show();
    }

    @Subscribe("copyBtn")
    public void onCopyBtnClick(Button.ClickEvent event) {
        String text = messageArea.getValue();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            // Экранирование для безопасного JavaScript копирования в буфер обмена
            String escaped = text.replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$");
            JavaScript.getCurrent().execute("navigator.clipboard.writeText(`" + escaped + "`);");
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Скопировано в буфер обмена")
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Выделите и скопируйте текст вручную")
                    .show();
        }
    }

    @Subscribe("createInteractionBtn")
    public void onCreateInteractionBtnClick(Button.ClickEvent event) {
        if (candidateId == null || vacancyId == null) {
            return;
        }

        IteractionList draft = workflowService.prepareInteractionDraft(candidateId, vacancyId, null, matchedSkills, null);
        String editedText = messageArea.getValue();
        if (editedText != null && !editedText.trim().isEmpty()) {
            draft.setComment(editedText.trim());
        }

        close(StandardOutcome.COMMIT);

        screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(IteractionListEdit.class)
                .editEntity(draft)
                .withOpenMode(OpenMode.NEW_TAB)
                .build()
                .show();
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }
}
