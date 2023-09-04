package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;

import javax.xml.crypto.Data;
import java.util.HashMap;

public interface EmailGenerationService {
    String NAME = "itpearls_EmailGenerationService";

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            ExtUser user);

    String preparingMessage(String text,
                            ExtUser user,
                            Data addDate);

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            ExtUser user,
                            Data addDate);

    String preparingMessage(String text,
                            OpenPosition openPosition);

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            OpenPosition openPosition,
                            ExtUser user);

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            OpenPosition openPosition,
                            ExtUser user,
                            Data addDate);

    String preparingMessage(IteractionList newsItem);

    public HashMap<String, String> generateKeys();
}
