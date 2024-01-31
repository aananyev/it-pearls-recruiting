package com.company.itpearls.core;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.app.Authentication;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

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
    private Messages messages;
    @Inject
    protected Authentication authentication;
    @Inject
    private Persistence persistence;


    @Override
    public List<String> getOpenPositionSet() {
       List<String> openPositionSet = null;
        authentication.begin();
        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery("select e from itpearls_OpenPosition e where not e.openClose = true");

                openPositionSet = query.getResultList();
                tx.commit();
            }

        } finally {
            authentication.end();
            return openPositionSet;
        }
    }

    @Override
    public List<OpenPosition> getOpenPositionList() {
        authentication.begin();
        List<OpenPosition> openPositions = new ArrayList<>();

        try {
            openPositions = dataManager.load(OpenPosition.class)
                    .query("select e from itpearls_OpenPosition e where not (e.openClose = true)")
                    .view("openPosition-view")
                    .list();
        } finally {
            authentication.end();
            return openPositions;
        }
    }

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

        sb.append("\uD83D\uDCAC Закрыта вакансия: ")
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
        StringBuilder sb = new StringBuilder("\uD83D\uDCAC");

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
                .append(": ");

        if (entity.getSalaryCandidateRequest() != null ? entity.getSalaryCandidateRequest() : false) {
            salarySB.append(messages.getMessage(MESSAGES_OPEN_POSITION_CLASS, "msgSalaryCandidateRequest"));
        } else {
            if (entity.getSalaryMin() != null) {
                salarySB.append("от")
                        .append(" ")
                        .append(entity.getSalaryMin().toString()
                                .substring(0, entity.getSalaryMin().toString().length() - 3));
            } else {
                salarySB.append("неопределена");
            }

            if (entity.getSalaryMax() != null) {
                salarySB.append(" ")
                        .append("до")
                        .append(" ")
                        .append(entity.getSalaryMax().toString()
                                .substring(0, entity.getSalaryMax().toString().length() - 3));
            } else {
                salarySB.append("неопределена");
            }
        }

        if (entity.getSalaryComment() != null) {
            salarySB.append("\n")
                    .append("\uD83D\uDCAC Комментарий")
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

    @Override
    public Map<String, Integer> setCommandExperienceMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("3 года", 3);
        map.put("5 лет и более", 4);
        map.put("Управление командой", 5);

        return map;
    }

    @Override
    public Map<String, Integer> setWorkExperienceMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("2 года", 3);
        map.put("3 года", 4);
        map.put("5 лет и более", 5);

        return map;
    }
}