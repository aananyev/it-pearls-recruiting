package com.company.itpearls.service;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Partners;

public interface PartnersSubscribeOpenPositionService {
    String NAME = "itpearls_PartnersSubscribeOpenPositionService";

    Boolean checkSubscribePartners(Partners partner, OpenPosition openPosition);
}