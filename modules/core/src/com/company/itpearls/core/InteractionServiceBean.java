package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

@Service(InteractionService.NAME)
public class InteractionServiceBean implements InteractionService {
    @Inject
    private DataManager dataManager;

    private final static String QUERY = "select e.iteractionType, count(e.iteractionType) "
            + "from itpearls_IteractionList e "
            + "where "
            + "(e.dateIteraction between :endDate and :startDate) and "
            + "e.iteractionType is not null and "
            + "e.recrutier = :user "
            + "group by e.iteractionType "
            + "order by count(e.iteractionType) desc";

    @Override
    public List<Iteraction> getMostPolularIteraction(User user, int maxCount) {

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());

        Date startDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -1);
        Date endDate = gregorianCalendar.getTime();

        List<KeyValueEntity> list = dataManager.loadValues(QUERY)
                .properties("iteractionType", "count")
                .parameter("user", user)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .list();

        List<Iteraction> retIteraction = new ArrayList<>();

        if (maxCount > list.size())
            maxCount = list.size();

        for (int i = 0; i < maxCount; i++) {
            retIteraction.add(list.get(i).getValue("iteractionType"));
        }

        return retIteraction;
    }

    @Override
    public IteractionList getLastIteraction(JobCandidate jobCandidate) {
        if (jobCandidate.getIteractionList() != null) {
            IteractionList maxIteraction = null;

            for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
                if (maxIteraction == null)
                    maxIteraction = iteractionList;

                if (iteractionList.getNumberIteraction() != null) {
                    if (maxIteraction.getNumberIteraction().compareTo(iteractionList.getNumberIteraction()) < 0) {
                        maxIteraction = iteractionList;
                    }
                }
            }

            return maxIteraction;
        } else
            return null;
    }

}