package com.company.hunttech.core;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;

import java.util.Date;
import java.util.HashMap;

public interface EmailGenerationService {
    String NAME = "hunttech_EmailGenerationService";

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            ExtUser user);

    String preparingMessage(String text,
                            ExtUser user,
                            Date addDate);

    String preparingMessage(String text,
                            JobCandidate jobCandidate,
                            ExtUser user,
                            Date addDate);

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
                            Date addDate);

    String preparingMessage(IteractionList newsItem);

    public HashMap<String, String> generateKeys();
}
