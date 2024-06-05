package com.company.itpearls.service;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;

import java.util.Date;

public interface RecrutiesTasksService {
    String NAME = "itpearls_RecrutiesTasksService";

    Boolean isSubscribed(OpenPosition openPosition, ExtUser user, Date startDate, Date endDate);

    Boolean isSubscribed(OpenPosition openPosition, Date startDate, Date endDate);
}