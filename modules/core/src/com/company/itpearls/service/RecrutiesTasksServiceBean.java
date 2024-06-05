package com.company.itpearls.service;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Service(RecrutiesTasksService.NAME)
public class RecrutiesTasksServiceBean implements RecrutiesTasksService {

    private static final String QUERY_CHECK_SUBSCIBED = "select e from itpearls_RecrutiesTasks e " +
            "where e.reacrutier = :reacrutier " +
            "and e.openPosition = :openPosition " +
            "and not e.openPosition.openClose = true " +
            "and not e.closed = true " +
            "and ((e.startDate between :startDate and :endDate) " +
            "or (e.endDate between :startDate and :endDate) or " +
            "e.startDate = :startDate or e.endDate = :endDate)";
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;


    @Override
    public Boolean isSubscribed(OpenPosition openPosition, ExtUser user, Date startDate, Date endDate) {

        List<RecrutiesTasks> recrutiesTasks = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_CHECK_SUBSCIBED)
                .view("recrutiesTasks-view")
                .parameter("reacrutier", user)
                .parameter("openPosition", openPosition)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .cacheable(true)
                .list();

        return recrutiesTasks.size() == 0 ? false : true;
    }

    @Override
    public Boolean isSubscribed(OpenPosition openPosition, Date startDate, Date endDate) {
        return isSubscribed(openPosition, (ExtUser) userSessionSource.getUserSession().getUser(), startDate, endDate);
    }

}