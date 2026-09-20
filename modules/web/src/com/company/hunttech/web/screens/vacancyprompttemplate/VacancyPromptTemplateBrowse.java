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
    private Label<String> sidebarCode;
    @Inject
    private Label<String> sidebarTemperatureBadge;
    @Inject
    private VBoxLayout quickActions;
    @Inject
    private Button openCardBtn;
    @Inject
    private VBoxLayout sidebarDetails;
    @Inject
    private Label<String> sidebarTemperatureVal;
    @Inject
    private Label<String> sidebarSystemContextVal;
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
            sidebarCode.setVisible(false);
            sidebarTemperatureBadge.setVisible(false);
            quickActions.setVisible(false);
            sidebarDetails.setVisible(false);
        } else {
            sidebarTitle.setValue(template.getName() != null ? template.getName() : "—");
            if (template.getCode() != null) {
                sidebarCode.setValue(template.getCode());
                sidebarCode.setVisible(true);
            } else {
                sidebarCode.setVisible(false);
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
                sidebarSystemContextVal.setValue(sysCtx.length() > 120 ? sysCtx.substring(0, 117) + "..." : sysCtx);
            } else {
                sidebarSystemContextVal.setValue("—");
            }

            String prompt = template.getPromptText();
            if (prompt != null && !prompt.trim().isEmpty()) {
                sidebarPromptTextVal.setValue(prompt.length() > 160 ? prompt.substring(0, 157) + "..." : prompt);
            } else {
                sidebarPromptTextVal.setValue("—");
            }

            quickActions.setVisible(true);
            sidebarDetails.setVisible(true);
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

