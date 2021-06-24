package com.company.itpearls.core;

import java.util.HashMap;

public interface EmailGenerationService {
    String NAME = "itpearls_EmailGenerationService";

    public HashMap<String, String> generateKeys();
}