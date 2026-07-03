package com.company.hunttech.service;

import com.haulmont.cuba.security.entity.User;

import javax.management.relation.Role;

public interface GetUserRoleService {
    String NAME = "hunttech_GetUserRoleService";

    Boolean isUserRoles(User user, String role);
}