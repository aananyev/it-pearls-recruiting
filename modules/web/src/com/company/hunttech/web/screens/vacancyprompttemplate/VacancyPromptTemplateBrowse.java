package com.company.hunttech.web.screens.vacancyprompttemplate;

import com.company.hunttech.entity.VacancyPromptTemplate;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("hunttech_VacancyPromptTemplate.browse")
@UiDescriptor("vacancy-prompt-template-browse.xml")
@LookupComponent("vacancyPromptTemplatesTable")
@LoadDataBeforeShow
public class VacancyPromptTemplateBrowse extends StandardLookup<VacancyPromptTemplate> {

    @Inject
    private GroupTable<VacancyPromptTemplate> vacancyPromptTemplatesTable;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Messages messages;

    @Inject
    private Label<String> sidebarTitle;
    @Inject
    private Label<String> sidebarSubtitle;
    @Inject
    private Label<String> sidebarCode;
    @Inject
    private Label<String> sidebarTemperatureBadge;
    @Inject
    private VBoxLayout quickActions;
    @Inject
    private Button openCardBtn;

    @Inject
    private VBoxLayout parametersCard;
    @Inject
    private Label<String> sidebarCodeVal;
    @Inject
    private Label<String> sidebarTemperatureVal;

    @Inject
    private VBoxLayout roleCard;
    @Inject
    private Label<String> sidebarSystemContextVal;

    @Inject
    private VBoxLayout taskCard;
    @Inject
    private Label<String> sidebarPromptTextVal;

    @Subscribe
    public void onInit(InitEvent event) {
        vacancyPromptTemplatesTable.addSelectionListener(e -> {
            VacancyPromptTemplate selected = vacancyPromptTemplatesTable.getSingleSelected();
            updateSidebar(selected);
        });

        openCardBtn.addClickListener(e -> {
            VacancyPromptTemplate selected = vacancyPromptTemplatesTable.getSingleSelected();
            if (selected != null) {
                openEditor(selected);
            }
        });
    }

    private void updateSidebar(VacancyPromptTemplate template) {
        if (template == null) {
            sidebarTitle.setValue(messages.getMessage(getClass(), "selectTemplatePrompt"));
            sidebarSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));
            sidebarCode.setVisible(false);
            sidebarTemperatureBadge.setVisible(false);
            quickActions.setVisible(false);
            parametersCard.setVisible(false);
            roleCard.setVisible(false);
            taskCard.setVisible(false);
        } else {
            String name = template.getName();
            sidebarTitle.setValue(name != null && !name.trim().isEmpty() ? name : "—");
            sidebarSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));

            if (template.getCode() != null) {
                sidebarCode.setValue(template.getCode());
                sidebarCode.setVisible(true);
                sidebarCodeVal.setValue(template.getCode());
            } else {
                sidebarCode.setVisible(false);
                sidebarCodeVal.setValue("—");
            }

            if (template.getTemperature() != null) {
                sidebarTemperatureBadge.setValue("t = " + template.getTemperature());
                sidebarTemperatureBadge.setVisible(true);
                sidebarTemperatureVal.setValue(String.valueOf(template.getTemperature()));
            } else {
                sidebarTemperatureBadge.setVisible(false);
                sidebarTemperatureVal.setValue("—");
            }

            String sysCtx = template.getSystemContext();
            if (sysCtx != null && !sysCtx.trim().isEmpty()) {
                sidebarSystemContextVal.setValue(sysCtx);
                roleCard.setVisible(true);
            } else {
                sidebarSystemContextVal.setValue("—");
                roleCard.setVisible(false);
            }

            String prompt = template.getPromptText();
            if (prompt != null && !prompt.trim().isEmpty()) {
                sidebarPromptTextVal.setValue(prompt.length() > 280 ? prompt.substring(0, 277) + "..." : prompt);
                taskCard.setVisible(true);
            } else {
                sidebarPromptTextVal.setValue("—");
                taskCard.setVisible(false);
            }

            quickActions.setVisible(true);
            parametersCard.setVisible(true);
        }
    }

    private void openEditor(VacancyPromptTemplate template) {
        screenBuilders.editor(vacancyPromptTemplatesTable)
                .editEntity(template)
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }
}

