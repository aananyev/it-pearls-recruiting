package com.company.itpearls.web.screens.vacancyprompttemplate;

import com.company.itpearls.entity.VacancyPromptTemplate;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("itpearls_VacancyPromptTemplate.browse")
@UiDescriptor("vacancy-prompt-template-browse.xml")
@LookupComponent("vacancyPromptTemplatesTable")
@LoadDataBeforeShow
public class VacancyPromptTemplateBrowse extends StandardLookup<VacancyPromptTemplate> {
}
