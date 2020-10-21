package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;

import javax.inject.Inject;

@UiController("itpearls_IteractionListSimple.browse")
@UiDescriptor("iteraction-list-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListSimpleBrowse extends StandardLookup<IteractionList> {

    @Inject
    private CollectionLoader<IteractionList> iteractionListDl;

    public void setSelectedCandidate(JobCandidate jobCandidate) {
        iteractionListDl.setParameter("candidate", jobCandidate);
        iteractionListDl.load();
    }
}