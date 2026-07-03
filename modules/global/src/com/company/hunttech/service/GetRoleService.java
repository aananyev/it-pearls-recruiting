package com.company.hunttech.service;

import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

@Service(GetUserRoleService.NAME)
public interface GetRoleService {
    String NAME = "hunttech_GetRoleService";

    Boolean isUserRoles(User user, String role);
    Boolean checkUserRoles(User user, String role);
}
