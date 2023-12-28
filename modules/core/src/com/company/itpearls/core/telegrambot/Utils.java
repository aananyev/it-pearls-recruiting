package com.company.itpearls.core.telegrambot;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Date;
import java.util.List;

public class Utils {

    /**
     * Формирование имени пользователя
     *
     * @param msg сообщение
     */
    public static String getUserName(Message msg) {
        return getUserName(msg.getFrom());
    }

    /**
     * Формирование имени пользователя. Если заполнен никнейм, используем его. Если нет - используем фамилию и имя
     *
     * @param user пользователь
     */
    public static String getUserName(User user) {
        return (user.getUserName() != null) ? user.getUserName() :
                String.format("%s %s", user.getLastName(), user.getFirstName());
    }

    private static <T> T queryListResult(String queryStr, View view, String field, Date date) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.addView(view);
                query.setParameter(field, date);

                retObj = (T) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }


    /**
     * @param queryStr - запрос
     * @param field    - поле условия типа Date
     * @param date     - дата
     */

    private static <T> T queryListResult(String queryStr, String field, Date date) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.setParameter(field, date);

                retObj = (T) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     * @param startDate
     * @param endDate
     * @param <T>
     * @return
     */
    private static <T> T queryOneResult(String queryStr, Date startDate, Date endDate) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);

                retObj = (T) query.getFirstResult();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     */

    private static <T> T queryOneResult(String queryStr) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);

                retObj = (T) query.getFirstResult();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     */
    private static <T> T queryListResult(String queryStr) {
        List<Object> retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);

                retObj = (List<Object>) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    public static List<RecrutiesTasks> getRecrutiesTasks() {
        return queryListResult("select e from itpearls_RecrutiesTasks e " +
                        "where :date between e.startDate and e.endDate",
                new View(RecrutiesTasks.class)
                        .addProperty("startDate")
                        .addProperty("endDate")
                        .addProperty("recrutierName")
                        .addProperty("reacrutier",
                                new View(ExtUser.class)
                                        .addProperty("name"))
                        .addProperty("openPosition",
                                new View(OpenPosition.class)
                                        .addProperty("vacansyName")),
                "date", new Date());
    }

    public static List<OpenPosition> getOpenPosition() {
        return queryListResult("select e from itpearls_OpenPosition e " +
                "where not e.openClose = true");
    }

    public static String getBotName() {
        return queryOneResult("select e.telegramBotName from itpearls_ApplicationSetup e " +
                "where e.activeSetup = true");
    }

    public static String getHelloMessage() {
        return new StringBuilder()
                .append("Бот *")
                .append(getBotName())
                .append("*\n\n")
                .append("Описание команд:\n")
                .append("*/allvacancy* - список открытых вакансий\n")
                .append("*/subscribe* - мои подписки на вакансии\n")
                .append("*/settings* - настройки\n")
                .append("*/help* - получение помощи\n\n")
                .toString();
    }
}
