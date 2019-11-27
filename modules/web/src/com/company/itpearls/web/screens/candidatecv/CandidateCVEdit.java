package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
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
    private LookupPickerField<JobCandidate> candidateField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setDatePost(new Date());  //установить дату публикации резюме

            getEditedEntity().setOwner(userSession.getUser());
        }

        if( getEditedEntity().getLinkOriginalCv() != null )
            getEditedEntity().setLinkOriginalCv( getEditedEntity().getLinkOriginalCv() );

        if( getEditedEntity().getLinkItPearlsCV() != null )
            getEditedEntity().setLinkItPearlsCV( getEditedEntity().getLinkItPearlsCV() );
    }

    public void setCandidate( JobCandidate candidate ) {
        candidateField.setValue( candidate );
    }

    @Inject
    private TextField<String> textFieldIOriginalCV;

    @Subscribe("textFieldIOriginalCV")
    public void onTextFieldIOriginalCVValueChange(HasValue.ValueChangeEvent<String> event) {
       getEditedEntity().setLinkOriginalCv( getEditedEntity().getLinkOriginalCv() );
    }

    @Inject
    private TextField<String> textFieldITPearlsCV;

    @Subscribe("textFieldITPearlsCV")
    public void onTextFieldITPearlsCVValueChange(HasValue.ValueChangeEvent<String> event) {
       getEditedEntity().setLinkItPearlsCV( getEditedEntity().getLinkItPearlsCV() );
    }
    
    
}