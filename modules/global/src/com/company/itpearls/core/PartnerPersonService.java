package com.company.itpearls.core;

import com.company.itpearls.entity.Partners;

public interface PartnerPersonService {
    String NAME = "itpearls_PartnerPersonService";

    Partners getMyPartner();

    Partners getPartner(String userLogin);
}