package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Service(IteractionService.NAME)
public class IteractionServiceBean implements IteractionService {
    @Inject
    private DataManager dataManager;

    @Override
    public List<Iteraction> getMostPolularIteraction(User user, int maxCount) {
        String QUERY = "select e.iteractionType, count(e) " +
                "from IteractionList e " +
                "where (e.dateIteraction between :startDate and :endDate) and e.recrutier = :user " +
                "group by e.iteractionType";

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());

        Date startDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -1);
        Date endDate = gregorianCalendar.getTime();

        ValueLoadContext context = ValueLoadContext.create()
                .setQuery(ValueLoadContext.createQuery(QUERY)
                        .setParameter("user", user)
                        .setParameter("startDate", startDate)
                        .setParameter("endDate", endDate))
                .addProperty("iteractionType")
                .addProperty("coubt");

        List<KeyValueEntity> list = dataManager.loadValues(context);
/*
        List<KeyValueEntity> mostPopularIteraction = dataManager.loadValues(QUERY)
                .properties("iteractionType", "count")
                .parameter("user", user)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .list();
*/
        return null;
    }
}