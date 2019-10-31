package com.company.itpearls.web.screens.candidatecv;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;

@UiController("itpearls_CandidateCV.browse")
@UiDescriptor("candidate-cv-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVBrowse extends StandardLookup<CandidateCV> {
}