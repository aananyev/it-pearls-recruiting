package com.company.hunttech.service;

import java.util.Date;

public interface SubscribeDateService {
    String NAME = "hunttech_SubscribeDateService";

    public Date dateOfNextMonday();
}