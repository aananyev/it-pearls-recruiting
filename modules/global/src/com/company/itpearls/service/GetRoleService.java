package com.company.itpearls.service;

import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

@Service(GetUserRoleService.NAME)
public interface GetRoleService {
    String NAME = "itpearls_GetRoleService";

    Boolean isUserRoles(User user, String role);
}