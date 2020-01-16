package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.SomeFiles;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<SomeFiles> someFilesesDl;
    @Inject
    private LinkButton linkOriginalCV;
    @Inject
    private LinkButton linkITPearlsCV;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( getEditedEntity().getLinkOriginalCv() != null ) {
                linkOriginalCV.setVisible( true );
        } else {
                linkOriginalCV.setVisible( false );
        }

        if( getEditedEntity().getLinkItPearlsCV() != null ) {
                linkITPearlsCV.setVisible( true );
            } else {
                linkITPearlsCV.setVisible( false );
            }
        }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setDatePost(new Date());  //установить дату публикации резюме

            getEditedEntity().setOwner(userSession.getUser());
        }
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        if( !PersistenceHelper.isNew( getEditedEntity() ) )
            someFilesesDl.setParameter( "candidate", getEditedEntity().getCandidate().getFullName() );
        else
            someFilesesDl.setParameter( "candidate", "None" );
    }
}