package com.company.itpearls.core;

import com.company.itpearls.service.SubscribeDateService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

@Service(SubscribeDateService.NAME)
public class SubscribeDateServiceBean implements SubscribeDateService {

    @Override
    public Date dateOfNextMonday() {
        LocalDate currentDate = LocalDate.now();
        LocalDate nextMonday = currentDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Instant instant = nextMonday
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Date legacyDate = Date.from( instant );
        return  legacyDate;
    }
}