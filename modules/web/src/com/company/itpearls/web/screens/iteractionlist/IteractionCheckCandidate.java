package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;

import javax.inject.Inject;

@UiController("itpearls_CheckCandidate.browse")
@UiDescriptor("iteraction-check-candidate.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionCheckCandidate extends StandardLookup<IteractionList> {
    @Inject
    private LookupField<JobCandidate> lookupFieldCheckCandidate;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;

    @Subscribe("lookupFieldCheckCandidate")
    public void onLookupFieldCheckCandidateValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        if(lookupFieldCheckCandidate.getValue().getFullName().equals(""))
            iteractionListsDl.setParameter("fullName", "Null" );
        else
           iteractionListsDl.setParameter("fullName", lookupFieldCheckCandidate.getValue().getFullName());

        iteractionListsDl.load();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        iteractionListsDl.setParameter("fullName", "Null" );
        iteractionListsDl.load();
    }

    @Install(to = "lookupFieldCheckCandidate", subject = "optionIconProvider")
    private String lookupFieldCheckCandidateOptionIconProvider(JobCandidate jobCandidate) {
        return lookupFieldCheckCandidate.getIcon();
    }


    @Install(to = "iteractionListsTable", subject = "iconProvider")
    private String iteractionListsTableIconProvider(IteractionList iteractionList) {
        return iteractionList.getIteractionType().getPic();
    }
}