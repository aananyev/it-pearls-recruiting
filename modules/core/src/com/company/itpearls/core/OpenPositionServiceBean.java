package com.company.itpearls.core;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.*;
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
    @Inject
    private TextManipulationService textManipulationService;
    @Inject
    private MessageTools messageTools;
    @Inject
    private Messages messages;

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
        openPosition.setProjectName(projectService.getProjectDefault());
        ;
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

    private final static String CLOSE_VACANCY = "ЗАКРЫТА ВАКАНСИЯ: ";
    private final static String OPEN_VACANCY = "ОТКРЫТА ВАКАНСИЯ: ";

    @Override
    public String getOpenPositionOpenShortMessage(OpenPosition entity, User user) {
        StringBuilder sb = new StringBuilder();

        if (entity.getOpenClose() != null) {
            sb.append(entity.getOpenClose() ? CLOSE_VACANCY : OPEN_VACANCY);
        } else {
            sb.append(OPEN_VACANCY);
        }

        sb.append(entity.getVacansyName())
                .append("<br><svg align=\"right\" width=\"100%\"><i>")
                .append(user.getName())
                .append("</i></svg>");

        return sb.toString();

    }

    final static String MESSAGES_OPEN_POSITION_CLASS = "com.company.itpearls.web";

    @Override
    public String getOpenPositionOpenLongMessage(OpenPosition entity, User user) {
        StringBuilder salarySB = new StringBuilder("Зарплатное предложение")
//                messageTools.loadString("msgSalaryMinMax"))
                .append(": ");

        if (entity.getSalaryCandidateRequest() != null ? entity.getSalaryCandidateRequest() : false) {
            salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryCandidateRequest"));
        } else {
            if (entity.getSalaryMin() != null) {
                salarySB.append("от")
//                        .append(messageTools.loadString("msgSalaryFrom"))
                        .append(" ")
                        .append(entity.getSalaryMin().toString()
                        .substring(0, entity.getSalaryMin().toString().length() - 3));
            } else {
//                salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryUndefined"));
                salarySB.append("неопределена");
            }

            if (entity.getSalaryMax() != null) {
                salarySB.append(" ")
                        .append("до")
//                        .append(messageTools.loadString("msgSalaryTo"))
                        .append(" ")
                        .append(entity.getSalaryMax().toString()
                                .substring(0, entity.getSalaryMax().toString().length() - 3));
            } else {
//                salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryUndefined"));
                salarySB.append("неопределена");
            }
        }

        if (entity.getSalaryComment() != null) {
            salarySB.append("\n")
                    .append("Комментарий")
//                    .append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryComment"))
                    .append(": ")
                    .append(entity.getSalaryComment());
        }

        return new StringBuilder(getOpenPositionOpenShortMessage(entity, user))
                .append("\n\n")
                .append(entity.getComment())
                .append("\n\n")
                .append(salarySB)
                .toString();
    }

    @Override
    public String getOpenPositionCloseLongMessage(OpenPosition entity, User user) {
        StringBuilder salarySB = new StringBuilder(
                messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryMinMax"))
                .append(": ");

        if (entity.getSalaryCandidateRequest() != null ? entity.getSalaryCandidateRequest() : false) {
            salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryCandidateRequest"));
        } else {
            if (entity.getSalaryMin() != null) {
                salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryFrom"))
                        .append(" ")
                        .append(entity.getSalaryMin().toString()
                                .substring(0, entity.getSalaryMin().toString().length() - 3));
            } else {
                salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryUndefined"));
            }

            if (entity.getSalaryMax() != null) {
                salarySB.append(" ")
                        .append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryTo"))
                        .append(" ")
                        .append(entity.getSalaryMax().toString()
                                .substring(0, entity.getSalaryMax().toString().length() - 3));
            } else {
                salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryUndefined"));
            }
        }

        if (entity.getSalaryComment() != null) {
            salarySB.append("\n")
                    .append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryComment"))
                    .append(": ")
                    .append(entity.getSalaryComment());
        }

        return textManipulationService
                .formattedHtml2text(new StringBuilder(getOpenPositionOpenShortMessage(entity, user))
                        .append("\n\n")
                        .append(entity.getComment())
                        .append("\n\n")
                        .append(salarySB)
                        .toString());
    }
}