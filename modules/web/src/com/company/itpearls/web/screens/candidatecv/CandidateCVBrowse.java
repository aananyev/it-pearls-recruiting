package com.company.itpearls.web.screens.candidatecv;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_CandidateCV.browse")
@UiDescriptor("candidate-cv-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVBrowse extends StandardLookup<CandidateCV> {
    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;
    @Inject
    private CheckBox checkBoxSetOnlyMy;
    @Inject
    private UserSession userSession;

    @Subscribe("checkBoxSetOnlyMy")
    public void onCheckBoxSetOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
       if(checkBoxSetOnlyMy.getValue()) {
           candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%" );
       } else {
          candidateCVsDl.removeParameter( "userName");
       }

       candidateCVsDl.load();
    }
    
    
/*    private CollectionContainer<CandidateCV> candidateCVsSetupDc;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setCheckBoxOnlyMy(); // установка флага "только мои клиенты"
    }

    private void setCheckBoxOnlyMy() {
       Boolean onlyMyCandidates =
                dataManager.loadValue("select e.paramSetBool from itpearls_setup e" +
                        "where e.paramName='" +
                        "OnlyMyCandidates" + "' and e.paramUser='" +
                        userSessionSource.getUserSession().getUser().getName() + "';",
                Boolean.class).one();
    } */
}