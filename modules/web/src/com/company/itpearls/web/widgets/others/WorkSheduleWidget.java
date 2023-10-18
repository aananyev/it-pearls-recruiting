package com.company.itpearls.web.widgets.others;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.SessionLogEntry;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@UiController("itpearls_WorkSheduleWidget")
@UiDescriptor("work-shedule-widget.xml")
@DashboardWidget(name = "Work Shedule")
public class WorkSheduleWidget extends ScreenFragment {
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private Label<String> workTimeLabel;
    @Inject
    private MessageBundle messageBundle;

    @Subscribe
    public void onInit(InitEvent event) {
        setWorkTimeToday();
    }

    private void setWorkTimeToday() {
        int allWorkTimeInMonth = getWorkTime(Calendar.MONTH);
        workTimeLabel.setDescription(messageBundle.getMessage("msgAvgWorkTime")
                + ": "
                + String.valueOf(allWorkTimeInMonth / 30)
                + " "
                + messageBundle.getMessage("msgHours")
                + " "
                + messageBundle.getMessage("msgPerDay"));

        if (allWorkTimeInMonth < 4) {
            workTimeLabel.setValue("Part time");
        } else {
            workTimeLabel.setValue("Full time");
        }
    }

    private int getWorkTime(int dayOfMonth) {
        String query = null;
        int countTime = 0;
        workTimeLabel.setVisible(true);

        switch (dayOfMonth) {
            case Calendar.DAY_OF_MONTH:
                query = "select e from sec$SessionLogEntry e where e.user = :user and @between(e.startedTs, now, now+1, day)";
                break;
            case Calendar.MONTH:
                query = "select e from sec$SessionLogEntry e where e.user = :user and @between(e.startedTs, now-30, now+1, day)";
                break;
            default:
                break;
        }

        try {
            List<SessionLogEntry> sessionLogEntry =
                    dataManager.loadValue(query, SessionLogEntry.class)
                            .parameter("user", userSession.getUser())
                            .list();

            for (SessionLogEntry sle : sessionLogEntry) {
                Date finishedTs = sle.getFinishedTs() != null ? sle.getFinishedTs() : new Date();
                countTime += (int) (finishedTs.getTime() - sle.getStartedTs().getTime()) / 60000 / 60;
            }
        } catch (Exception e) {
            e.printStackTrace();
            workTimeLabel.setVisible(false);
        }

        return countTime;
    }
}