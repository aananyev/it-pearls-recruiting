package com.company.itpearls.core;

import com.company.itpearls.entity.Partners;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(PartnerPersonService.NAME)
public class PartnerPersonServiceBean implements PartnerPersonService {
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;

    @Override
    public Partners getMyPartner() {
        return getPartner(userSessionSource.getUserSession().getUser().getLogin());
    }

    @Override
    public Partners getPartner(String userLogin) {
        final String QUERY_GET_PARTNER = "select e from itpearls_Partners e where e in (select f.partners from itpearls_PartnersPerson f where f.partnerPersonLogin like :login)";

        List<Partners> partnersList = dataManager.load(Partners.class)
                .query(QUERY_GET_PARTNER)
                .parameter("login", userLogin)
                .cacheable(true)
                .view("partners-view")
                .list();

        if (partnersList.size() > 0) {
            return partnersList.iterator().next();
        } else {
            return null;
        }
    }
}