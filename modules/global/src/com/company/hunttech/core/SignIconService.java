package com.company.hunttech.core;

import com.company.hunttech.entity.ExtUser;

public interface SignIconService {
    String NAME = "hunttech_SignIconService";

    public void createDefaultIcons(ExtUser user);

    public void createDefaultIcons(ExtUser user, String iconsSet[]);

    public boolean checkUserIcons();
}
