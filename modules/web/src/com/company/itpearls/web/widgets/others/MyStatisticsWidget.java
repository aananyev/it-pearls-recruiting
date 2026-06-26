package com.company.itpearls.web.widgets.others;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.StreamResource;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.SessionLogEntry;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UiController("itpearls_MyStatisticsWidget")
@UiDescriptor("my-statistics-widget.xml")
@DashboardWidget(name = "Моя статистика")
public class MyStatisticsWidget extends ScreenFragment {
    @Inject
    private UserSession userSession;
    @Inject
    private DataManager dataManager;
    @Inject
    private Label<String> workTimeTodayLabel;
    @Inject
    private Label<String> interviewSheduledTodayLabel;
    @Inject
    private Label<String> interviewsConductedTodayLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        setOffersCounter();
        setWorkTimeToday();
        setCunductedInterviewToday();
        setAssignedInterviewToday();
    }


    private static final String QUERY_CONDUCTED_TODAY_COUNT =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.iteractionType.signOurInterview = true and e.recrutier = :recrutier " +
                    "and @between(e.dateIteraction, now, now+1, day)";

    private static final String QUERY_ASSIGNED_TODAY_COUNT =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.iteractionType.signOurInterviewAssigned = true and e.recrutier = :recrutier " +
                    "and @between(e.dateIteraction, now, now+1, day)";

    private void setCunductedInterviewToday() {
        Long count = dataManager.loadValue(QUERY_CONDUCTED_TODAY_COUNT, Long.class)
                .parameter("recrutier", userSession.getUser())
                .one();
        interviewsConductedTodayLabel.setValue(String.valueOf(count != null ? count : 0L));
    }

    private void setAssignedInterviewToday() {
        Long count = dataManager.loadValue(QUERY_ASSIGNED_TODAY_COUNT, Long.class)
                .parameter("recrutier", userSession.getUser())
                .one();
        interviewSheduledTodayLabel.setValue(String.valueOf(count != null ? count : 0L));
    }

    private void setWorkTimeToday() {
        workTimeTodayLabel.setValue(getWorkTime(Calendar.DAY_OF_MONTH));
    }

    private String getWorkTime(int dayOfMonth) {
        String query = null;

        switch (dayOfMonth) {
            case Calendar.DAY_OF_MONTH:
                query = "select e from sec$SessionLogEntry e where e.user = :user and @between(e.startedTs, now, now+1, day)";
                break;
            default:
                break;
        }

        List<SessionLogEntry> sessionLogEntry =
                dataManager.loadValue(query, SessionLogEntry.class)
                        .parameter("user", userSession.getUser())
                        .list();
        int countTime = 0;

        for (SessionLogEntry sle : sessionLogEntry) {
            Date finishedTs = sle.getFinishedTs() != null ? sle.getFinishedTs() : new Date();
            countTime += (int) (finishedTs.getTime() - sle.getStartedTs().getTime()) / 60000 / 60;
        }

        return String.valueOf(countTime);
    }

    private void setOffersCounter() {
    }

}