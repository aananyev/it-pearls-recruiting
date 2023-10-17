package com.company.itpearls.web.widgets.others;

import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_GradeRecruter")
@UiDescriptor("grade-recruter.xml")
@DashboardWidget(name = "Recruiter's grade")
public class GradeRecruter extends ScreenFragment {
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private Label<String> gradeLabel;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Label<String> gradeGrowthLabel;
    @Inject
    private Label<String> gradeFallLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        setGradeLabel();
    }

    private void setGradeLabel() {
        Map<Date, Integer> interviewsCount = new HashMap<>();
/*        final String QUERY_INTERVIEWS_COUNT =
                "select e.dateIteraction, count(e) from itpearls_IteractionList e " +
                "where e.recrutier = :recrutier and "
                + "@between(e.dateIteraction, now-1, now, month) and "
                + "(e.iteractionType.signOurInterviewAssigned = true or e.iteractionType.signOurInterview = true)"
                + "group by e.dateIteraction";
        List<KeyValueEntity> list = dataManager.loadValues(QUERY_INTERVIEWS_COUNT)
                .properties("dateIteraction", "count")
                .parameter("recrutier", userSession.getUser())
                .list();*/

        final String QUERY_INTERVIEWS_COUNT_INTERVAL =
                "select count(e) from itpearls_IteractionList e " +
                        "where e.recrutier = :recrutier and "
                        + "@between(e.dateIteraction, now-30, now+1, day) and "
                        + "(e.iteractionType.signOurInterviewAssigned = true or e.iteractionType.signOurInterview = true)";

        final String QUERY_INTERVIEWS_COUNT_INTERVAL_YESTERDAY =
                "select count(e) from itpearls_IteractionList e " +
                        "where e.recrutier = :recrutier and "
                        + "@between(e.dateIteraction, now-31, now, day) and "
                        + "(e.iteractionType.signOurInterviewAssigned = true or e.iteractionType.signOurInterview = true)";

        int countInteraction = dataManager.loadValue(QUERY_INTERVIEWS_COUNT_INTERVAL, Integer.class)
                .parameter("recrutier", userSession.getUser())
                .one();

        int countInteractionYesterday = dataManager.loadValue(QUERY_INTERVIEWS_COUNT_INTERVAL, Integer.class)
                .parameter("recrutier", userSession.getUser())
                .one();

        int codeGrade = 0;
        int codeGradeYesterday = 0;

        if (countInteraction < 10) {
            gradeLabel.setValue("Junior");
        } else {
            if (countInteraction < 20) {
                gradeLabel.setValue("Regular");
                codeGrade = 1;
            } else {
                if (countInteraction < 30) {
                    gradeLabel.setValue("Master");
                    codeGrade = 2;
                } else {
                    gradeLabel.setValue("Grand Master");
                    codeGrade = 3;
                }
            }
        }

        if (countInteractionYesterday < 10) {
        } else {
            if (countInteractionYesterday < 20) {
                codeGradeYesterday = 1;
            } else {
                if (countInteractionYesterday < 30) {
                    codeGradeYesterday = 2;
                } else {
                    codeGradeYesterday = 3;
                }
            }
        }

        if (codeGrade > codeGradeYesterday) {
            gradeGrowthLabel.setVisible(true);
            gradeFallLabel.setVisible(false);
        }

        if (codeGrade < codeGradeYesterday) {
            gradeGrowthLabel.setVisible(false);
            gradeFallLabel.setVisible(true);
        }

        if (codeGrade == codeGradeYesterday) {
            gradeGrowthLabel.setVisible(false);
            gradeFallLabel.setVisible(false);
        }

        gradeLabel.setDescription(messageBundle.getMessage("msgInterviews")
                + ": "
                + countInteraction);
    }
}