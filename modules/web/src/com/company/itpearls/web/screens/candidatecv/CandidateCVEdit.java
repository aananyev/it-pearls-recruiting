package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.persistence.internal.sessions.DirectCollectionChangeRecord;
import org.graalvm.compiler.code.DataSection;

import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.util.Date;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {

    @Inject
    private CollectionContainer<JobCandidate> candidatesDc;

    @Subscribe(id = "candidateCVDc", target = Target.DATA_CONTAINER)
    private void onCandidateCVDcItemChange(InstanceContainer.ItemChangeEvent<CandidateCV> event) {
        
    }

    protected void setCurrentDate() {
/*
        CandidateCV line = candidatesDc.getItem();
        Date date = new Date();

        if( line.getDatePost() != DataSection.ZeroData) {
            line.setDatePost(date);

        } */
    }
    
    
}