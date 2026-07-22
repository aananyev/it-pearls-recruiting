package com.company.itpearls.service;

import com.company.itpearls.entity.UserAiProfile;
import com.company.itpearls.service.dto.AiUserContext;

public interface UserAiContextService {
    String NAME = "itpearls_UserAiContextService";

    AiUserContext buildCurrentUserContext();

    AiUserContext buildContext(UserAiProfile profile);

    String buildCurrentUserContextPreview();

    String buildContextPreview(UserAiProfile profile);
}
