package com.company.hunttech.web.screens.vacancyprompttemplate;

import com.company.hunttech.entity.VacancyPromptTemplate;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_VacancyPromptTemplate.edit")
@UiDescriptor("vacancy-prompt-template-edit.xml")
@EditedEntityContainer("vacancyPromptTemplateDc")
@LoadDataBeforeShow
public class VacancyPromptTemplateEdit extends StandardEditor<VacancyPromptTemplate> {
}
