package com.company.itpearls.web.screens.iteractionlist;

import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;

import javax.inject.Inject;

@UiController("itpearls_myCandidate.browse")
@UiDescriptor("my-candidate-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class MyCandidateBrowse extends StandardLookup<IteractionList> {

    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        iteractionListsDl.removeParameter( "currentUser" );
        iteractionListsDl.load();
    }
}