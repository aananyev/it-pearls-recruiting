package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;

public interface ApplicationSetupService {
    String NAME = "itpearls_ApplicationSetupService";

    String getTelegramToken();

    String getTelegramChatOpenPosition();

    ApplicationSetup getActiveApplicationSetup();
}