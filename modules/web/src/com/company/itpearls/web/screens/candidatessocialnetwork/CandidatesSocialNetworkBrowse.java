package com.company.itpearls.web.screens.candidatessocialnetwork;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidatesSocialNetwork;

@UiController("itpearls_CandidatesSocialNetwork.browse")
@UiDescriptor("candidates-social-network-browse.xml")
@LookupComponent("candidatesSocialNetworksTable")
@LoadDataBeforeShow
public class CandidatesSocialNetworkBrowse extends StandardLookup<CandidatesSocialNetwork> {
}