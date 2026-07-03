package com.company.hunttech.core;

import java.io.IOException;

public interface WebLoadService {
    String NAME = "hunttech_WebLoadService";

    String getCVWebPage(String urlCV) throws IOException;
}