package com.company.itpearls.web.screens.candidatessocialnetwork;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidatesSocialNetwork;

@UiController("itpearls_CandidatesSocialNetwork.edit")
@UiDescriptor("candidates-social-network-edit.xml")
@EditedEntityContainer("candidatesSocialNetworkDc")
@LoadDataBeforeShow
public class CandidatesSocialNetworkEdit extends StandardEditor<CandidatesSocialNetwork> {
}