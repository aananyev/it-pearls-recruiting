package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.QueryUtils;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;

import javax.inject.Inject;

@UiController("itpearls_InternalEmailerGroupTemplate.edit")
@UiDescriptor("internal-emailer-group-template-edit.xml")
public class InternalEmailerGroupTemplateEdit extends InternalEmailerTemplateEdit {
    @Inject
    private SuggestionPickerField<JobCandidate> toEmailField;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onBeforeShow2(BeforeShowEvent event) {
        setSuggestionPiskerField();
    }

    private void setSuggestionPiskerField() {
        /* toEmailField.setSearchExecutor((searchString, searchParams) -> {
            searchString = QueryUtils.escapeForLike(searchString);
            return dataManager.load(JobCandidate.class)
                    .query("e.name like ?1 order by e.name escape '\\'",
                            "%" + searchString + "%")
                    .list();
        }); */
    }
}