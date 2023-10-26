package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.GradeCode;
import com.company.itpearls.entity.GradeIntCode;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(RecruterStatService.NAME)
public class RecruterStatServiceBean implements RecruterStatService {

    final String QUERY_INTERVIEWS_COUNT_INTERVAL =
            "select count(e) from itpearls_IteractionList e " +
                    "where e.recrutier = :recrutier and "
                    + "@between(e.dateIteraction, now-30, now+1, day) and "
                    + "(e.iteractionType.signOurInterviewAssigned = true or e.iteractionType.signOurInterview = true)";
    @Inject
    private DataManager dataManager;

    public int countInteraction(ExtUser user) {
        int retInt = 0;

        retInt = dataManager.loadValue(QUERY_INTERVIEWS_COUNT_INTERVAL, Integer.class)
                .parameter("recrutier", user)
                .one();

        return retInt;
    }

    public int getGrade(int countInteractionYesterday) {
        int codeGradeYesterday;

        if (countInteractionYesterday < 10) {
            codeGradeYesterday = GradeIntCode.JUNIOR.getId();
        } else {
            if (countInteractionYesterday < 20) {
                codeGradeYesterday = GradeIntCode.REGULAR.getId();
            } else {
                if (countInteractionYesterday < 30) {
                    codeGradeYesterday = GradeIntCode.MASTER.getId();
                } else {
                    codeGradeYesterday = GradeIntCode.GRAND_MASTER.getId();
                }
            }
        }

        return codeGradeYesterday;
    }

    public String getGradeName(int gradeNumber) {
        String retStr = "";

        if (gradeNumber == GradeCode.JUNIOR.ordinal()) {
            retStr = String.valueOf(GradeCode.JUNIOR);
        } else {
            if (gradeNumber == GradeCode.REGULAR.ordinal()) {
                retStr = String.valueOf(GradeCode.REGULAR);
            } else {
                if (gradeNumber == GradeCode.MASTER.ordinal()) {
                    retStr = String.valueOf(GradeCode.MASTER);
                } else {
                    if (gradeNumber == GradeCode.GRAND_MASTER.ordinal()) {
                        retStr = String.valueOf(GradeCode.GRAND_MASTER);
                    }
                }
            }
        }

        return retStr;
    }
}