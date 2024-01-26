package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.SignIcons;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(SignIconService.NAME)
public class SignIconServiceBean implements SignIconService {
    @Inject
    private Metadata metadata;

    final static String iconRu[] = {"Звезда (красный)", "Звезда (желтый)", "Звезда (зеленый)",
            "Флаг (красный)", "Флаг (желтый)", "Флаг (зеленый)"};
    final static String iconEnd[] = {"Star (red)", "Star (yellow)", "Star (green)",
            "Flag (red)", "Flag (yellow)", "Flag (green)"};
//    final static String icon[] = {CubaIcon.STAR.source(), CubaIcon.STAR.source(), CubaIcon.STAR.source(),
//            CubaIcon.FLAG.source(), CubaIcon.FLAG.source(), CubaIcon.FLAG.source()};
    final static String iconColor[] = {"FF0000", "FFA500", "008000",
            "FF0000","FFA500", "008000"};

    final static String QUERY_USER_SIGN_ICONS = "select e from itpearls_SignIcons e where e.user = :user";

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;

    public void createDefaultIcons(ExtUser user) {
        for (int i = 0; i < 6; i++) {
            SignIcons signIcons = metadata.create(SignIcons.class);

            signIcons.setTitleRu(iconRu[i]);
            signIcons.setTitleEnd(iconEnd[i]);
            signIcons.setIconColor(iconColor[i]);
            signIcons.setUser((ExtUser) userSessionSource.getUserSession().getUser());

            dataManager.commit(signIcons);
        }
    }

    public void createDefaultIcons(ExtUser user, String iconsSet[]) {
        for (int i = 0; i < 6; i++) {
            SignIcons signIcons = metadata.create(SignIcons.class);

            signIcons.setTitleRu(iconRu[i]);
            signIcons.setTitleEnd(iconEnd[i]);
            signIcons.setIconName(iconsSet[i]);
            signIcons.setIconColor(iconColor[i]);
            signIcons.setUser((ExtUser) userSessionSource.getUserSession().getUser());

            dataManager.commit(signIcons);
        }
    }

    public boolean checkUserIcons() {
        if (dataManager.load(SignIcons.class)
                .query(QUERY_USER_SIGN_ICONS)
                .parameter("user", userSessionSource.getUserSession().getUser())
                .view("signIcons-view")
                .list()
                .size() == 0) {
            return true;
        } else {
            return false;
        }
    }
}