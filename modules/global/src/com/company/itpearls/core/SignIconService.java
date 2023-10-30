package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;

public interface SignIconService {
    String NAME = "itpearls_SignIconService";

    public void createDefaultIcons(ExtUser user);

    public void createDefaultIcons(ExtUser user, String iconsSet[]);
}
