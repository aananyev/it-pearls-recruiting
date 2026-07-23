package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;

public interface UserAiContextService {
    String NAME = "hunttech_UserAiContextService";

    AiUserContext buildCurrentUserContext();

    AiUserContext buildContext(UserAiProfile profile);

    String buildCurrentUserContextPreview();

    String buildContextPreview(UserAiProfile profile);
}
