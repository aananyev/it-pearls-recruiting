package com.company.hunttech.web.screens.useraifunctionoverride;

import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_UserAiFunctionOverride.browse")
@UiDescriptor("user-ai-function-override-browse.xml")
@LookupComponent("overridesTable")
public class UserAiFunctionOverrideBrowse extends StandardLookup<UserAiFunctionOverride> {
    @Inject
    private CollectionLoader<UserAiFunctionOverride> overridesDl;
    @Inject
    private UserSessionSource userSessionSource;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // Параметр устанавливается до load: пользователь никогда не получает строки других пользователей.
        overridesDl.setParameter("user", userSessionSource.getUserSession().getUser());
        overridesDl.load();
    }
}
