package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_CandidateCheckPaused")
@UiDescriptor("candidate-check-paused.xml")
@DashboardWidget(name = "Check Candidate in Pause")
public class CandidateCheckPaused extends ScreenFragment {

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    @WidgetParam
    @WindowParam
    protected String iteractionName;

    @Inject
    private CollectionLoader<Iteraction> candidatesPausedDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<IteractionList> candidatesPausedDc;
    @Inject
    private Label<String> widgetTitle;

    @Subscribe
    public void onInit(InitEvent event) {
        setWidgetTitle();
        setRecrutier();
        setIteractionName();
    }

    private void setIteractionName() {
        if(iteractionName != null)
            candidatesPausedDl.setParameter("iteractionName", iteractionName);
        else
            candidatesPausedDl.removeParameter("iteractionName");

        candidatesPausedDl.load();
    }

    private void setRecrutier() {
        candidatesPausedDl.setParameter("recrutier", userSession.getUser());
        candidatesPausedDl.load();
    }

    @Subscribe
    public void onAttach(AttachEvent event) {
        candidatesPausedDl.load();
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        candidatesPausedDl.load();
    }

    private void setWidgetTitle() {
        String title = "Кандидаты на паузе: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        Date date = new Date();

        widgetTitle.setValue(title + df.format(date));
    }
}