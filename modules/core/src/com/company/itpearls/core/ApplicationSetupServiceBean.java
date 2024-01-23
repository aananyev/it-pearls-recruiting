package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.entity.ApplicationSetup;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service(ApplicationSetupService.NAME)
public class ApplicationSetupServiceBean implements ApplicationSetupService {

    private Logger logger = LoggerFactory.getLogger(Bot.class);

    private static final String QUERY_GET_ACTIVE_SETUP
            = "select e from itpearls_ApplicationSetup e where e.activeSetup = true";
    @Inject
    private DataManager dataManager;
    @Inject
    private Persistence persistence;

    @Override
    public String getTelegramBotName() {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());

        try {
            String scripts =
                    runner.query(
                            "select TELEGRAM_BOT_NAME from ITPEARLS_APPLICATION_SETUP where ACTIVE_SETUP = true",
                            rs -> rs.next() ? rs.getString(1) : null);

            return scripts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean getTelegramBotStart() {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());

        try {
            Boolean scripts =
                    runner.query(
                            "select TELEGRAM_BOT_START from ITPEARLS_APPLICATION_SETUP where ACTIVE_SETUP = true",
                            rs -> rs.next() ? rs.getBoolean(1) : null);

            return scripts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBotStartedConfig(ApplicationSetup applicationSetup, boolean b) {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());

        try {
            runner.query("update ITPEARLS_APPLICATION_SETUP set TELEGRAM_BOT_STARTED = false", null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            runner.query(String.format("update ITPEARLS_APPLICATION_SETUP set TELEGRAM_BOT_STARTED = true where NAME = %s",
                            applicationSetup.getName()),
                    null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getTelegramToken() {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());

        try {
            String scripts =
                    runner.query(
                            "select TELEGRAM_TOKEN from ITPEARLS_APPLICATION_SETUP where ACTIVE_SETUP = true",
                            rs -> rs.next() ? rs.getString(1) : null);

            return scripts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTelegramChatOpenPosition() {
        QueryRunner runner = new QueryRunner(persistence.getDataSource());

        try {
            String scripts =
                    runner.query(
                            "select TELEGRAM_CHAT_OPEN_POSITION from ITPEARLS_APPLICATION_SETUP where ACTIVE_SETUP = true",
                            rs -> rs.next() ? rs.getString(1) : null);


            return scripts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileDescriptor getCompanyImage() {
        ApplicationSetup applicationSetup = getActiveApplicationSetup();

        if (applicationSetup != null) {
            return getActiveApplicationSetup().getApplicationLogo();
        } else {
            return null;
        }
    }

    @Override
    public ApplicationSetup getApplicationSetup() {
        ApplicationSetup applicationSetup;

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP);
            query.setView(new View(ApplicationSetup.class, "applicationSetup-view"));
            query.addView(new View(ApplicationSetup.class).addProperty("applicationLogo"));
            applicationSetup = (ApplicationSetup) query.getFirstResult();
            tx.commit();
        }

        return applicationSetup;

    }

    @Override
    public FileDescriptor getActiveCompanyIcon() {
        ApplicationSetup applicationSetup;

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP);
            query.setView(new View(ApplicationSetup.class, "applicationSetup-view")
                    .addProperty("applicationLogo"));
            applicationSetup = (ApplicationSetup) query.getFirstResult();
            tx.commit();
        }

        return applicationSetup.getApplicationLogo();
    }

    @Override
    public FileDescriptor getActiveCompanyLogo() {
        ApplicationSetup applicationSetup;

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP);
            query.setView(ApplicationSetup.class,
                    "applicationSetup-view");
            query.addView(new View(ApplicationSetup.class)
                    .addProperty("applicationLogo"));
            applicationSetup = (ApplicationSetup) query.getFirstResult();
            tx.commit();
        }

        return applicationSetup.getApplicationIcon();
    }


    @Override
    public FileDescriptor getCompanyIcon() {
        ApplicationSetup applicationSetup = getActiveApplicationSetup();

        if (applicationSetup != null) {
            return getActiveApplicationSetup().getApplicationIcon();
        } else {
            return null;
        }
    }

    @Override
    public ApplicationSetup getActiveApplicationSetup() {
        List<ApplicationSetup> applicationSetup = new ArrayList<>();

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP);
                query.setView(new View(ApplicationSetup.class, "applicationSetup-view"));
                query.addView(new View(ApplicationSetup.class).addProperty( "applicationLogo"));
                query.addView(new View(ApplicationSetup.class).addProperty("applicationIcon"));

                applicationSetup = (List<ApplicationSetup>) query.getResultList();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return applicationSetup.size() > 0 ? applicationSetup.get(0) : null;
        }
    }

/*    @Override
    public ApplicationSetup getActiveApplicationSetup() {
        ApplicationSetup applicationSetup = null;

        try {
            applicationSetup = dataManager.load(ApplicationSetup.class)
                    .query(QUERY_GET_ACTIVE_SETUP)
                    .cacheable(true)
                    .view("applicationSetup-view")
                    .one();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return applicationSetup;
        }
    } */

    @Override
    public void clearActiveApplicationSetup() {
        List<ApplicationSetup> applicationSetups = dataManager.load(ApplicationSetup.class)
                .view("applicationSetup-view")
                .list();

        for (ApplicationSetup applicationSetup : applicationSetups) {
            applicationSetup.setActiveSetup(false);
        }

        CommitContext commitContext = new CommitContext(applicationSetups);
        dataManager.commit(commitContext);
    }

    @Override
    public void clearActiveApplicationSetup(ApplicationSetup current) {
        List<ApplicationSetup> applicationSetups = dataManager.load(ApplicationSetup.class)
                .view("applicationSetup-view")
                .list();

        for (ApplicationSetup applicationSetup : applicationSetups) {
            if (!applicationSetup.equals(current)) {
                applicationSetup.setActiveSetup(false);
            }
        }

        CommitContext commitContext = new CommitContext(applicationSetups);
        dataManager.commit(commitContext);
    }

    @Override
    public String getActiveConfigName() {
        return getActiveApplicationSetup().getName();
    }
}