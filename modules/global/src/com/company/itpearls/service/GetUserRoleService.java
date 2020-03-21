package com.company.itpearls.service;

import com.haulmont.cuba.security.entity.User;

import javax.management.relation.Role;

public interface GetUserRoleService {
    String NAME = "itpearls_GetUserRoleService";

    Boolean isUserRoles(User user, String role);
}