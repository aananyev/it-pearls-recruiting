package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(ApplicationSetupService.NAME)
public class ApplicationSetupServiceBean implements ApplicationSetupService {

    private static final String QUERY_GET_ACTIVE_SETUP
            = "select e from itpearls_ApplicationSetup e where e.activeSetup = true";
    @Inject
    private DataManager dataManager;

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
    public ApplicationSetup getActiveApplicationSetup() {
        ApplicationSetup applicationSetup = null;

        try {
            applicationSetup = dataManager.load(ApplicationSetup.class)
                    .query(QUERY_GET_ACTIVE_SETUP)
                    .cacheable(true)
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
}