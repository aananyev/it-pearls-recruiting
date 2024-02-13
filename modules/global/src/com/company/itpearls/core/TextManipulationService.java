package com.company.itpearls.core;

import com.haulmont.cuba.core.entity.FileDescriptor;

public interface TextManipulationService {
    String NAME = "itpearls_TextManipulationService";

    String formattedHtml2text(String inputHtml);

    String getMailHTMLFooter();

    String getMailHTMLHeader();

    String getImage(FileDescriptor fd);
}