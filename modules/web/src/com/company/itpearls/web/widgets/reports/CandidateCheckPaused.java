package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
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
    private Label<String> widgetTitle;
    @Inject
    private CollectionLoader<Iteraction> candidatesPausedDl;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onInit(InitEvent event) {
        setWidgetTitle();
//        setRecrutier();
//        setIteractionName();
    }

    private void setIteractionName() {
        if(iteractionName != null)
            candidatesPausedDl.setParameter("iteractionTypeName", iteractionName);
        else
            candidatesPausedDl.removeParameter("iteractionTypeName");
        candidatesPausedDl.load();
    }

    private void setRecrutier() {
        candidatesPausedDl.setParameter("userName", userSession.getUser());
        candidatesPausedDl.removeParameter("userName");
        candidatesPausedDl.load();
    }

    private void setWidgetTitle() {
        String title = "Кандидаты на паузе: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        Date date = new Date();

        widgetTitle.setValue(title + df.format(date));
    }
}