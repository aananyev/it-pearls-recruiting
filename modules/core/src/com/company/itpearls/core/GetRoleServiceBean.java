package com.company.itpearls.core;

import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;
import com.company.itpearls.service.GetRoleService;

import javax.inject.Inject;
import java.util.Collection;

@Service(GetRoleService.NAME)
public class GetRoleServiceBean implements GetRoleService {
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