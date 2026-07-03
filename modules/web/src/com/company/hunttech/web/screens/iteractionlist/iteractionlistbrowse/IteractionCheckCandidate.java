package com.company.hunttech.web.screens.iteractionlist.iteractionlistbrowse;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.IteractionList;

import javax.inject.Inject;

@UiController("hunttech_CheckCandidate.browse")
@UiDescriptor("iteraction-check-candidate.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionCheckCandidate extends StandardLookup<IteractionList> {
    @Inject
    private LookupField<JobCandidate> lookupFieldCheckCandidate;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private GroupTable<IteractionList> iteractionListsTable;

    @Subscribe("lookupFieldCheckCandidate")
    public void onLookupFieldCheckCandidateValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        if (lookupFieldCheckCandidate.getValue().getFullName().equals("")) {
            iteractionListsDl.removeParameter("name");
            iteractionListsTable.setVisible(false);
        } else {
            iteractionListsTable.setVisible(true);
            iteractionListsDl.setParameter("name", lookupFieldCheckCandidate.getValue());
        }

        iteractionListsDl.load();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        iteractionListsTable.setVisible(false);
    }

    @Install(to = "lookupFieldCheckCandidate", subject = "optionIconProvider")
    private String lookupFieldCheckCandidateOptionIconProvider(JobCandidate jobCandidate) {
        return lookupFieldCheckCandidate.getIcon();
    }


    @Install(to = "iteractionListsTable", subject = "iconProvider")
    private String iteractionListsTableIconProvider(IteractionList iteractionList) {
        if (iteractionList.getIteractionType() != null)
            return iteractionList.getIteractionType().getPic();
        else
            return null;
    }
}