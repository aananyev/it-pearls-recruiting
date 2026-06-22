package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

@Service(InteractionService.NAME)
public class InteractionServiceBean implements InteractionService {
    @Inject
    private DataManager dataManager;

    private static final String QUERY_GET_MAX_NUMBER_INTERACTION =
            "select max(e.numberIteraction) from itpearls_IteractionList e";

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

    private static final String QUERY_LAST_BY_CANDIDATE =
            "select e from itpearls_IteractionList e "
                    + "where e.candidate = :candidate "
                    + "order by e.numberIteraction desc";

    @Override
    public IteractionList getLastIteraction(JobCandidate jobCandidate) {
        if (jobCandidate == null || jobCandidate.getId() == null) {
            return null;
        }
        return dataManager.load(IteractionList.class)
                .query(QUERY_LAST_BY_CANDIDATE)
                .parameter("candidate", jobCandidate)
                .maxResults(1)
                .view("iteractionList-picker-view")
                .optional()
                .orElse(null);
    }

    @Override
    public BigDecimal getCountInteraction() {
        return dataManager.loadValue(QUERY_GET_MAX_NUMBER_INTERACTION, BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }
}