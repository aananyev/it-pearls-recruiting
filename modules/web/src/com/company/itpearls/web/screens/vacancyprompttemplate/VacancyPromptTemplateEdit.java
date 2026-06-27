package com.company.itpearls.web.screens.vacancyprompttemplate;

import com.company.itpearls.entity.VacancyPromptTemplate;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("itpearls_VacancyPromptTemplate.edit")
@UiDescriptor("vacancy-prompt-template-edit.xml")
@EditedEntityContainer("vacancyPromptTemplateDc")
@LoadDataBeforeShow
public class VacancyPromptTemplateEdit extends StandardEditor<VacancyPromptTemplate> {
}
