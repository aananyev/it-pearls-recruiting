package com.company.itpearls.service;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Partners;
import com.company.itpearls.entity.PartnersSubscribeOpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(PartnersSubscribeOpenPositionService.NAME)
public class PartnersSubscribeOpenPositionServiceBean implements PartnersSubscribeOpenPositionService {

    final static String QUERY_CHECK_PARTNERS_SUBSCRIBE = "select e from itpearls_PartnersSubscribeOpenPosition e where e.openPosition = :openPosition and e.partner = :partner";
    @Inject
    private DataManager dataManager;

    @Override
    public Boolean checkSubscribePartners(Partners partner, OpenPosition openPosition) {
        return !dataManager.load(PartnersSubscribeOpenPosition.class)
                .query(QUERY_CHECK_PARTNERS_SUBSCRIBE)
                .parameter("openPosition", openPosition)
                .parameter("partner", partner)
                .cacheable(true)
                .view("partnersSubscribeOpenPosition-view")
                .list()
                .isEmpty();
    }

}