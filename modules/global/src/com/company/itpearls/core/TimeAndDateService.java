package com.company.itpearls.core;

import java.util.Date;

public interface TimeAndDateService {
    String NAME = "itpearls_TimeAndDateService";

    Date setStartDay(Date date);

    Date setEndDay(Date date);
}