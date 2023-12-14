package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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

}