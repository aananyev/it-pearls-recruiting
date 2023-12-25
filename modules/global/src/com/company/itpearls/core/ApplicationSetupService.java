package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.entity.FileDescriptor;

public interface ApplicationSetupService {
    String NAME = "itpearls_ApplicationSetupService";

    String getTelegramBotName();

    String getTelegramToken();

    String getTelegramChatOpenPosition();

    FileDescriptor getCompanyImage();

    FileDescriptor getActiveCompanyIcon();

    FileDescriptor getActiveCompanyLogo();

    FileDescriptor getCompanyIcon();

    ApplicationSetup getActiveApplicationSetup();

    void clearActiveApplicationSetup();

    void clearActiveApplicationSetup(ApplicationSetup current);

    String getActiveConfigName();
}