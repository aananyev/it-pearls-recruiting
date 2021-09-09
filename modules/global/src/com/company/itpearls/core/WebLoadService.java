package com.company.itpearls.core;

import java.io.IOException;

public interface WebLoadService {
    String NAME = "itpearls_WebLoadService";

    String getCVWebPage(String urlCV) throws IOException;
}