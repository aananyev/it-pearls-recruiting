package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;

public interface RecruterStatService {
    String NAME = "itpearls_RecruterStatService";

    public int countInteraction(ExtUser user);

    public int getGrade(int countInteractionYesterday);

    public String getGradeName(int gradeNumber);
}