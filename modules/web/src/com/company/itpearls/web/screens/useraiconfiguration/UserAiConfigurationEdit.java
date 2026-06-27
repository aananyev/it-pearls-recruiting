package com.company.itpearls.web.screens.useraiconfiguration;

import com.company.itpearls.entity.UserAiConfiguration;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_UserAiConfiguration.edit")
@UiDescriptor("user-ai-configuration-edit.xml")
@EditedEntityContainer("userAiConfigurationDc")
@LoadDataBeforeShow
public class UserAiConfigurationEdit extends StandardEditor<UserAiConfiguration> {

    @Inject
    private LookupField<String> providerCodeField;

    private User parentUser;

    public void setParentUser(User parentUser) {
        this.parentUser = parentUser;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        Map<String, String> providers = new LinkedHashMap<>();
        providers.put("OpenAI", "openai");
        providers.put("YandexGPT", "yandex");
        providers.put("GigaChat", "gigachat");
        providerCodeField.setOptionsMap(providers);
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<UserAiConfiguration> event) {
        if (parentUser != null) {
            event.getEntity().setUser(parentUser);
        }
        if (event.getEntity().getIsActive() == null) {
            event.getEntity().setIsActive(true);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (parentUser != null && PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setUser(parentUser);
        }
    }
}
