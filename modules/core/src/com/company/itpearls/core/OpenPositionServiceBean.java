package com.company.itpearls.core;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.User;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;

@Service(OpenPositionService.NAME)
public class OpenPositionServiceBean implements OpenPositionService {

    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;
    @Inject
    private ProjectService projectService;

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

    @Override
    public OpenPosition createOpenPositionDefault() {
        OpenPosition openPosition = metadata.create(OpenPosition.class);

        openPosition.setVacansyName(DEFAULT_OPEN_POSITION);
        openPosition.setPriority(0);
        openPosition.setOpenClose(true);
        openPosition.setRemoteWork(0);
        openPosition.setCommandCandidate(0);
        openPosition.setProjectName(projectService.getProjectDefault());;
        openPosition.setWorkExperience(0);

        dataManager.commit(openPosition);

        return openPosition;
    }

    final static String DEFAULT_OPEN_POSITION = "Default";

    @Override
    public OpenPosition getOpenPositionDefault() {
        OpenPosition openPosition = null;

        try {
            openPosition = dataManager
                    .loadValue("select e from itpearls_OpenPosition e where e.vacansyName like \'"
                                    + DEFAULT_OPEN_POSITION
                                    + "\'",
                            OpenPosition.class)
                    .one();
        } catch (Exception e) {
            e.printStackTrace();
            openPosition = createOpenPositionDefault();
        } finally {
            return openPosition;
        }
    }

    @Override
    public String getOpenPositionCloseShortMessage(OpenPosition entity, User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("Закрыта вакансия: ")
                .append(entity.getVacansyName())
                .append("<br><svg align=\"right\" width=\"100%\"><i>")
                .append(user.getName())
                .append("</i></svg>");

        return sb.toString();
    }

    @Override
    public String getOpenPositionOpenShortMessage(OpenPosition entity, User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("Открыта вакансия: ")
                .append(entity.getVacansyName())
                .append("<br><svg align=\"right\" width=\"100%\"><i>")
                .append(user.getName())
                .append("</i></svg>");

        return sb.toString();

    }
    @Override
    public String getOpenPositionOpenLongMessage(OpenPosition entity, User user) {
        StringBuilder sb = new StringBuilder(getOpenPositionOpenShortMessage(entity, user));

        return new StringBuilder(Jsoup.parse(sb.toString()).text())
               .append("\n\n")
               .append(entity.getComment())
               .append("\n\n")
               .append("Salary")
               .append(" ")
               .append("from")
               .append(" ")
               .append(entity.getSalaryMin() != null ? entity.getSalaryMin() : "???")
               .append(" ")
               .append("to")
               .append(" ")
               .append(entity.getSalaryMax() != null ? entity.getSalaryMax() : "???")
               .append(entity.getSalaryComment() != null ? " (" : "")
               .append(entity.getSalaryComment() != null ? entity.getSalaryComment() : "")
               .append(entity.getSalaryComment() != null ? ")" : "")
               .toString();
    }

    @Override
    public String getOpenPositionCloseLongMessage(OpenPosition entity, User user) {
        StringBuilder sb = new StringBuilder(getOpenPositionOpenShortMessage(entity, user));

        return new StringBuilder(Jsoup.parse(sb.toString()).text())
                .append("\n\n")
                .append(entity.getComment())
                .append("\n\n")
                .append("Salary")
                .append(" ")
                .append("from")
                .append(" ")
                .append(entity.getSalaryMin() != null ? entity.getSalaryMin() : "???")
                .append(" ")
                .append("to")
                .append(" ")
                .append(entity.getSalaryMax() != null ? entity.getSalaryMax() : "???")
                .append(entity.getSalaryComment() != null ? " (" : "")
                .append(entity.getSalaryComment() != null ? entity.getSalaryComment() : "")
                .append(entity.getSalaryComment() != null ? ")" : "")
                .toString();
    }
}