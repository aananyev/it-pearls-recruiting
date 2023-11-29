package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionNews;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;

@Service(OpenPositionService.NAME)
public class OpenPositionServiceBean implements OpenPositionService {

    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;

    @Override
    public void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                    String subject,
                                                    String comment,
                                                    Date date,
                                                    JobCandidate jobCandidate,
                                                    ExtUser user,
                                                    Boolean priority) {
        try {
            OpenPositionNews openPositionNews = metadata.create(OpenPositionNews.class);

            openPositionNews.setOpenPosition(editedEntity);
            openPositionNews.setAuthor((ExtUser) user);
            openPositionNews.setDateNews(date);
            openPositionNews.setSubject(subject);
            openPositionNews.setComment(comment);
            openPositionNews.setCandidates(jobCandidate);
            openPositionNews.setPriorityNews(priority != null ? priority : false);

            CommitContext commitContext = new CommitContext();
            commitContext.addInstanceToCommit(openPositionNews);
            dataManager.commit(commitContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     ExtUser user) {

        OpenPositionNews openPositionNews = metadata.create(OpenPositionNews.class);

        openPositionNews.setOpenPosition(editedEntity);
        openPositionNews.setAuthor(user);
        openPositionNews.setDateNews(date);
        openPositionNews.setSubject(subject);
        openPositionNews.setComment(comment);
        openPositionNews.setPriorityNews(true);

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(openPositionNews);
        dataManager.commit(commitContext);
    }

}