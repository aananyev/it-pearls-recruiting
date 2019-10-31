package com.company.itpearls.web.screens.candidatecv;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;

import java.util.Date;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {
    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        getEditedEntity().setDatePost(new Date());  //установить дату публикации резюме

    }
}