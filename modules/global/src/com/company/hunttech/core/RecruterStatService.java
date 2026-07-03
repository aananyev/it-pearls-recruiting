package com.company.hunttech.core;

import com.company.hunttech.entity.ExtUser;

public interface RecruterStatService {
    String NAME = "hunttech_RecruterStatService";

    public int countInteraction(ExtUser user);

    public int getGrade(int countInteractionYesterday);

    public String getGradeName(int gradeNumber);
}