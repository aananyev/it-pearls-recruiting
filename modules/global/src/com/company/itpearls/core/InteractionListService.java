package com.company.itpearls.core;

import com.company.itpearls.entity.IteractionList;

import java.math.BigDecimal;

public interface InteractionListService {
    String NAME = "itpearls_InteractionListService";

    BigDecimal getCountIteraction(IteractionList iteractionList);

    BigDecimal getCountInteraction();
}