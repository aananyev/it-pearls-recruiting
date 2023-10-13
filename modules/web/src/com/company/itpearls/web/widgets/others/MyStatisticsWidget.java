package com.company.itpearls.web.widgets.others;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
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
@DashboardWidget(name = "My Statistics")
public class MyStatisticsWidget extends ScreenFragment {
    @Inject
    private Label<String> myNameLabel;
    @Inject
    private UserSession userSession;
    @Inject
    private Image myPhotoImage;
    @Inject
    private DataManager dataManager;
    @Inject
    private InstanceLoader<ExtUser> userDl;
    @Inject
    private InstanceContainer<ExtUser> userDc;
    @Inject
    private Label<String> workTimeTodayLabel;
    @Inject
    private Label<String> interviewSheduledTodayLabel;
    @Inject
    private Label<String> interviewsConductedTodayLabel;
    @Inject
    private Label<String> myGroupLabel;

    @Subscribe
    public void onInit(InitEvent event) {

        try {
            userDl.setParameter("login", userSession.getUser().getLogin());
            userDl.load();

            ExtUser curUser = userDc.getItem();
            myPhotoImage.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(curUser.getFileImageFace());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setRecruterName();
        setOffersCounter();
        setWorkTimeToday();
        setCunductedInterviewToday();
        setAssignedInterviewToday();
        setUserGroup();
    }

    private void setUserGroup() {
        myGroupLabel.setValue(userSession.getUser().getGroup().getName());
    }

    private void setCunductedInterviewToday() {
        final String query = "select e from itpearls_IteractionList e " +
                "where e.iteractionType.signOurInterview = true and e.recrutier = :recrutier and @between(e.dateIteraction, now, now+1, day)";

        interviewsConductedTodayLabel.setValue(
                String.valueOf(dataManager
                        .load(IteractionList.class)
                        .query(query)
                        .parameter("recrutier", userSession.getUser())
                        .view("iteractionList-view")
                        .list()
                        .size()));
    }

    private void setAssignedInterviewToday() {
        final String query = "select e from itpearls_IteractionList e " +
                "where e.iteractionType.signOurInterviewAssigned = true and e.recrutier = :recrutier and @between(e.dateIteraction, now, now+1, day)";

        interviewSheduledTodayLabel.setValue(
                String.valueOf(dataManager
                        .load(IteractionList.class)
                        .query(query)
                        .parameter("recrutier", userSession.getUser())
                        .view("iteractionList-view")
                        .list()
                        .size()));
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

    private void setRecruterName() {
        myNameLabel.setValue(userSession.getUser().getName());
    }
}