package com.company.itpearls.service;

import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

@Service(GetUserRoleService.NAME)
public class GetUserRoleServiceBean implements GetUserRoleService {

    @Inject
    private UserSessionSource userSessionSource;

    @Override
    public Boolean isUserRoles(User user, String role) {
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        Boolean c = false;
        // установить поле рекрутера
        for( String a : s ) {
            if (a.contains(role)) {
                c = true;
                break;
            }
        }
        return c;
    }
}