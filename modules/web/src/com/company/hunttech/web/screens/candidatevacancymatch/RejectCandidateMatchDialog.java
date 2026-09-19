package com.company.hunttech.web.screens.candidatevacancymatch;

import com.company.hunttech.entity.Iteraction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UiController("hunttech_RejectCandidateMatchDialog")
@UiDescriptor("reject-candidate-match-dialog.xml")
@DialogMode(width = "520px", height = "AUTO", modal = true, forceDialog = true)
public class RejectCandidateMatchDialog extends Screen {

    @Inject
    private DataManager dataManager;

    @Inject
    private Notifications notifications;

    @Inject
    private Label<String> candidateLabel;

    @Inject
    private Label<String> vacancyLabel;

    @Inject
    private LookupField<String> reasonLookup;

    @Inject
    private TextArea<String> commentField;

    private String candidateName;
    private String vacancyName;
    private String selectedReason;
    private String comment;

    public void setContext(String candidateName, String vacancyName) {
        this.candidateName = candidateName;
        this.vacancyName = vacancyName;
    }

    public void initParams(String candidateName, String vacancyName) {
        setContext(candidateName, vacancyName);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (candidateName != null) {
            candidateLabel.setValue("Кандидат: " + candidateName);
        }
        if (vacancyName != null) {
            vacancyLabel.setValue("Вакансия: " + vacancyName);
        }

        initReasonOptions();
    }

    private void initReasonOptions() {
        Map<String, String> options = new LinkedHashMap<>();

        // Загружаем существующие причины отказов из справочника hunttech_iteraction
        try {
            List<Iteraction> iteractions = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where (e.iterationName like 'Отказ%' or e.iterationName like '%не подход%') and e.deleteTs is null order by e.iterationName asc")
                    .list();

            for (Iteraction it : iteractions) {
                if (it.getIterationName() != null && !it.getIterationName().trim().isEmpty()) {
                    options.put(it.getIterationName().trim(), it.getIterationName().trim());
                }
            }
        } catch (Exception e) {
            // fallback
        }

        // Гарантируем стандартные понятные опции
        ensureOption(options, "Не соответствует ключевым требованиям");
        ensureOption(options, "Неактуальный / недостаточный опыт");
        ensureOption(options, "Не подходит уровень квалификации (грейд)");
        ensureOption(options, "Не подходит локация / формат работы");
        ensureOption(options, "Завышены зарплатные ожидания");
        ensureOption(options, "Уже общается с другими рекрутерами");
        ensureOption(options, "Другая причина");

        reasonLookup.setOptionsMap(options);
        if (!options.isEmpty()) {
            reasonLookup.setValue(options.values().iterator().next());
        }
    }

    private void ensureOption(Map<String, String> map, String val) {
        if (!map.containsKey(val)) {
            map.put(val, val);
        }
    }

    @Subscribe("confirmBtn")
    public void onConfirmBtnClick(Button.ClickEvent event) {
        String reason = reasonLookup.getValue();
        String cText = commentField.getValue();

        if (reason == null || reason.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Выберите причину")
                    .withDescription("Укажите причину, почему кандидат не подходит.")
                    .show();
            return;
        }

        if ("Другая причина".equalsIgnoreCase(reason.trim()) && (cText == null || cText.trim().isEmpty())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Укажите комментарий")
                    .withDescription("Для варианта 'Другая причина' необходимо заполнить поле комментария.")
                    .show();
            return;
        }

        this.selectedReason = reason.trim();
        this.comment = cText != null ? cText.trim() : "";

        close(StandardOutcome.COMMIT);
    }

    @Subscribe("cancelBtn")
    public void onCancelBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }

    public String getSelectedReason() {
        return selectedReason;
    }

    public String getComment() {
        return comment;
    }
}
