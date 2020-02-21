package com.company.itpearls.web.screens.candidatecv;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.io.File;
import java.io.FileDescriptor;

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
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup().getName().equals("Стажер")) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");

            checkBoxSetOnlyMy.setValue(true);
            checkBoxSetOnlyMy.setEditable(false);
        }
    }

    @Subscribe("checkBoxSetOnlyMy")
    public void onCheckBoxSetOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxSetOnlyMy.getValue()) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            candidateCVsDl.removeParameter("userName");
        }

        candidateCVsDl.load();
    }

    public Component generateSemaforCell(CandidateCV entity) {
//        Image image = uiComponents.create(Image.NAME);

//        image.setSource( FileDescriptorResource.class ).setFileDescriptor( entity. );
//        image.setSource( FileDescriptorResource.class ).setFileDescriptor( entity.)

        return null;
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

    @Install(to = "candidateCVsTable", subject = "iconProvider")
    protected String candidateCVsTableiconProvider(CandidateCV candidateCV) {
        if (candidateCV.getLetter() != null &&
                candidateCV.getTextCV() != null &&
                candidateCV.getLinkItPearlsCV() != null) {
            return "icons/resume-green.png";
        } else {
            if ( ( candidateCV.getLetter() == null ||
                    candidateCV.getLinkItPearlsCV() == null ) &&
                    candidateCV.getOriginalFileCV() != null  &&
                    candidateCV.getTextCV() != null ) {
                return "icons/resume-yellow.png";
            } else {
                return "icons/resume-red.png";
            }
        }
    }
}