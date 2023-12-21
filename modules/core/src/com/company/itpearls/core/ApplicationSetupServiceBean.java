package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(ApplicationSetupService.NAME)
public class ApplicationSetupServiceBean implements ApplicationSetupService {

    private static final String QUERY_GET_ACTIVE_SETUP
            = "select e from itpearls_ApplicationSetup e where e.activeSetup = true";
    @Inject
    private DataManager dataManager;
    @Inject
    private Persistence persistence;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageAPI fileStorageAPI;
    @Inject
    private Metadata metadata;
    @Inject
    private TimeSource timeSource;

    @Override
    public String getTelegramToken() {
        ApplicationSetup applicationSetup = getActiveApplicationSetup();

        if (applicationSetup != null) {
            return getActiveApplicationSetup().getTelegramToken();
        } else {
            return null;
        }
    }

    @Override
    public String getTelegramChatOpenPosition() {
        ApplicationSetup applicationSetup = getActiveApplicationSetup();

        if (applicationSetup != null) {
            return getActiveApplicationSetup().getTelegramChatOpenPosition();
        } else {
            return null;
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
    public FileDescriptor getActiveCompanyIcon() {
        ApplicationSetup applicationSetup;

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP);
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

            Query query = em.createQuery(QUERY_GET_ACTIVE_SETUP).addView(ApplicationSetup.class,
                    "applicationSetup-view");
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
    }

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