package com.company.itpearls.core;

import com.company.itpearls.service.GetRoleService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

@Service(GetRoleService.NAME)
public class GetRoleServiceBean implements GetRoleService {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private DataManager dataManager;

    @Override
    public Boolean isUserRoles(User user, String role) {
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        Boolean c = false;
        // установить поле рекрутера
        for (String a : s) {
            if (a.contains(role)) {
                c = true;
                break;
            }
        }
        return c;
    }

    @Override
    public Boolean checkUserRoles(User user, String role) {
        Role s = dataManager.load(Role.class)
                .query("select e from sec$Role e where e.name like :roleName")
                .parameter("roleName", role)
                .one();

        if(role != null) {
            return user.getUserRoles().contains(s);
        } else
            return false;
    }
}