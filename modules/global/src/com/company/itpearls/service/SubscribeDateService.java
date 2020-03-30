package com.company.itpearls.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public interface SubscribeDateService {
    String NAME = "itpearls_SubscribeDateService";


    public Date dateOfNextMonday();
}