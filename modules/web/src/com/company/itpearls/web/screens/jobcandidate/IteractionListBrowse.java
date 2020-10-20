package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_IteractionListBrowse")
@UiDescriptor("iteraction-list-browse.xml")
public class IteractionListBrowse extends ScreenFragment {
    @Inject
    private Fragments fragments;

    @Subscribe
    public void onInit(InitEvent event) {
    }
}